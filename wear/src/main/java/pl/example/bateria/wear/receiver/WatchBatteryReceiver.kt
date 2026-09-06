package pl.example.bateria.wear.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pl.example.bateria.core.constants.WearConstants
import pl.example.bateria.core.model.BatteryStatus
import pl.example.bateria.wear.WearBatteryApp
import java.nio.ByteBuffer

class WatchBatteryReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        when (action) {
            Intent.ACTION_BATTERY_CHANGED -> {
                handleBatteryChanged(context, intent)
            }
            Intent.ACTION_POWER_CONNECTED -> {
                sendPowerEvent(context, WearConstants.PATH_WATCH_CHARGING_STARTED)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                sendPowerEvent(context, WearConstants.PATH_WATCH_CHARGING_STOPPED)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
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

    private fun dispatchStatus(context: Context, percent: Int, isCharging: Boolean) {
        scope.launch {
            try {
                val nodeClient = Wearable.getNodeClient(context)
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) return@launch

                val model = Build.MODEL ?: "Smartwatch"
                val telemetry = BatteryStatus(
                    battery_level = percent,
                    is_charging = isCharging,
                    time = System.currentTimeMillis(),
                    watch_model = model,
                    watch_app_version_code = 256
                )
                val bytes = json.encodeToString(telemetry).toByteArray(Charsets.UTF_8)
                val messageClient = Wearable.getMessageClient(context)

                for (node in nodes) {
                    messageClient.sendMessage(node.id, WearConstants.PATH_WATCH_STATUS, bytes).await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendPowerEvent(context: Context, path: String) {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 80) ?: 80
        sendIntMessage(context, path, level)
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
        fun queryAndSendCurrentStatus(context: Context) {
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (batteryIntent != null) {
                val receiver = WatchBatteryReceiver()
                receiver.handleBatteryChanged(context, batteryIntent)
            }
        }
    }
}
