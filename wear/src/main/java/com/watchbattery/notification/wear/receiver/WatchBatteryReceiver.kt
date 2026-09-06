package com.watchbattery.notification.wear.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.watchbattery.notification.core.constants.WearConstants
import com.watchbattery.notification.core.model.BatteryStatus
import com.watchbattery.notification.wear.BuildConfig
import com.watchbattery.notification.wear.WearBatteryApp
import com.watchbattery.notification.wear.service.WatchChargingMonitorService
import java.nio.ByteBuffer

class WatchBatteryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        when (action) {
            Intent.ACTION_BATTERY_CHANGED -> {
                handleBatteryChanged(context, intent)
            }
            Intent.ACTION_POWER_CONNECTED -> {
                ContextCompat.startForegroundService(context, Intent(context, WatchChargingMonitorService::class.java))
                sendPowerEvent(context, WearConstants.PATH_WATCH_CHARGING_STARTED)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                context.stopService(Intent(context, WatchChargingMonitorService::class.java))
                sendPowerEvent(context, WearConstants.PATH_WATCH_CHARGING_STOPPED)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                if (isCharging) {
                    ContextCompat.startForegroundService(context, Intent(context, WatchChargingMonitorService::class.java))
                }
                queryAndSendCurrentStatus(context)
            }
        }
    }

    private fun handleBatteryChanged(context: Context, intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return

        val percent = ((level.toFloat() / scale.toFloat()) * 100).toInt()
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val prefs = context.getSharedPreferences("wear_battery_prefs", Context.MODE_PRIVATE)
        val lastLevel = prefs.getInt("last_battery_level", -1)
        val lastCharging = prefs.getBoolean("last_charging_state", false)

        // Save current telemetry in local cache
        prefs.edit()
            .putInt("last_battery_level", percent)
            .putBoolean("last_charging_state", isCharging)
            .putLong("last_reading_time", System.currentTimeMillis())
            .apply()

        // Power-Efficient Dispatch Filter:
        // While charging: send every 1% for phone rainbow bar
        // While discharging: send only when percent decreases or power state flips
        val shouldDispatch = (percent != lastLevel) || (isCharging != lastCharging)

        if (shouldDispatch) {
            dispatchStatus(context, percent, isCharging)

            // Check thresholds
            val lowThreshold = prefs.getInt("low_threshold", 15)
            val highThreshold = prefs.getInt("high_threshold", 80)

            if (percent <= lowThreshold && !isCharging && (lastLevel > lowThreshold || lastLevel == -1)) {
                sendIntMessage(context, WearConstants.PATH_WATCH_LOW_BATTERY, percent)
            } else if (percent >= highThreshold && isCharging && (lastLevel < highThreshold || lastLevel == -1)) {
                sendIntMessage(context, WearConstants.PATH_WATCH_FULLY_CHARGED, percent)
            }
        }
    }

    private fun sendPowerEvent(context: Context, path: String) {
        val prefs = context.getSharedPreferences("wear_battery_prefs", Context.MODE_PRIVATE)
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = when {
            level >= 0 && scale > 0 -> ((level.toFloat() / scale.toFloat()) * 100).toInt()
            else -> prefs.getInt("last_battery_level", -1)
        }
        if (percent >= 0) {
            sendIntMessage(context, path, percent)
        }
    }

    private fun sendIntMessage(context: Context, path: String, value: Int) {
        scope.launch {
            try {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                val bytes = ByteBuffer.allocate(4).putInt(value).array()
                val messageClient = Wearable.getMessageClient(context)
                for (node in nodes) {
                    messageClient.sendMessage(node.id, path, bytes).await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(Dispatchers.IO)
        private val json = Json { ignoreUnknownKeys = true }

        fun dispatchStatus(context: Context, percent: Int, isCharging: Boolean) {
            scope.launch {
                try {
                    val model = Build.MODEL ?: "Smartwatch"
                    val timestamp = System.currentTimeMillis()

                    // 1. Put into DataClient (Persistent DataLayer sync across reconnection and reboots)
                    val dataMapRequest = PutDataMapRequest.create(WearConstants.PATH_WATCH_STATUS).apply {
                        dataMap.putInt(WearConstants.EXTRA_BATTERY_LEVEL, percent)
                        dataMap.putBoolean(WearConstants.EXTRA_CHARGING_STATUS, isCharging)
                        dataMap.putString(WearConstants.EXTRA_WATCH_MODEL, model)
                        dataMap.putLong(WearConstants.EXTRA_TIMESTAMP, timestamp)
                        dataMap.putInt("watch_app_version_code", BuildConfig.VERSION_CODE)
                    }
                    val putDataReq = dataMapRequest.asPutDataRequest().setUrgent()
                    Wearable.getDataClient(context).putDataItem(putDataReq).await()

                    // 2. Also send via MessageClient to connected nodes for instantaneous UI reactivity
                    val nodeClient = Wearable.getNodeClient(context)
                    val nodes = nodeClient.connectedNodes.await()
                    if (nodes.isNotEmpty()) {
                        val telemetry = BatteryStatus(
                            battery_level = percent,
                            is_charging = isCharging,
                            time = timestamp,
                            watch_model = model,
                            watch_app_version_code = BuildConfig.VERSION_CODE
                        )
                        val bytes = json.encodeToString(telemetry).toByteArray(Charsets.UTF_8)
                        val messageClient = Wearable.getMessageClient(context)

                        for (node in nodes) {
                            messageClient.sendMessage(node.id, WearConstants.PATH_WATCH_STATUS, bytes).await()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun queryAndSendCurrentStatus(context: Context) {
            forceSendCurrentStatus(context)
        }

        fun forceSendCurrentStatus(context: Context) {
            val prefs = context.getSharedPreferences("wear_battery_prefs", Context.MODE_PRIVATE)
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val capacity = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val percent = when {
                level >= 0 && scale > 0 -> ((level.toFloat() / scale.toFloat()) * 100).toInt()
                capacity in 0..100 -> capacity
                else -> prefs.getInt("last_battery_level", -1)
            }

            if (percent < 0) return

            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            prefs.edit()
                .putInt("last_battery_level", percent)
                .putBoolean("last_charging_state", isCharging)
                .putLong("last_reading_time", System.currentTimeMillis())
                .apply()

            dispatchStatus(context, percent, isCharging)
        }
    }
}
