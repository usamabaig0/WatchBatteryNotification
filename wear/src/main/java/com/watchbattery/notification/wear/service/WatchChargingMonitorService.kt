package com.watchbattery.notification.wear.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.watchbattery.notification.core.constants.WearConstants
import com.watchbattery.notification.wear.BuildConfig
import com.watchbattery.notification.wear.R
import com.watchbattery.notification.wear.receiver.WatchBatteryReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.nio.ByteBuffer

class WatchChargingMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var batteryReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        registerBatteryReceiver()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Charging Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracking charge level while docked"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Monitoring Battery")
            .setContentText("Tracking charge level while docked")
            .setSmallIcon(R.drawable.ic_stat_watch_battery)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun registerBatteryReceiver() {
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return

                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level < 0 || scale <= 0) return

                val percent = ((level.toFloat() / scale.toFloat()) * 100).toInt()
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                val prefs = context.getSharedPreferences("wear_battery_prefs", Context.MODE_PRIVATE)
                val highThreshold = prefs.getInt("high_threshold", 80)
                val lastLevel = prefs.getInt("last_battery_level", -1)
                val lastCharging = prefs.getBoolean("last_charging_state", false)

                if (percent >= highThreshold && isCharging && (lastLevel < highThreshold || lastLevel == -1)) {
                    notifyFullyCharged(percent)
                }

                if (percent != lastLevel || isCharging != lastCharging) {
                    WatchBatteryReceiver.dispatchStatus(context, percent, isCharging)
                }

                prefs.edit()
                    .putInt("last_battery_level", percent)
                    .putBoolean("last_charging_state", isCharging)
                    .putLong("last_reading_time", System.currentTimeMillis())
                    .apply()
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
    }

    private fun notifyFullyCharged(percent: Int) {
        serviceScope.launch {
            try {
                // Send WearConstants.PATH_WATCH_FULLY_CHARGED with percent via MessageClient to all connected nodes
                val nodes = Wearable.getNodeClient(this@WatchChargingMonitorService).connectedNodes.await()
                val bytes = ByteBuffer.allocate(4).putInt(percent).array()
                val messageClient = Wearable.getMessageClient(this@WatchChargingMonitorService)
                for (node in nodes) {
                    messageClient.sendMessage(node.id, WearConstants.PATH_WATCH_FULLY_CHARGED, bytes).await()
                }

                // Send WearConstants.PATH_WATCH_STATUS with .setUrgent() via DataClient
                val model = Build.MODEL ?: "Smartwatch"
                val timestamp = System.currentTimeMillis()
                val dataMapRequest = PutDataMapRequest.create(WearConstants.PATH_WATCH_STATUS).apply {
                    dataMap.putInt(WearConstants.EXTRA_BATTERY_LEVEL, percent)
                    dataMap.putBoolean(WearConstants.EXTRA_CHARGING_STATUS, true)
                    dataMap.putString(WearConstants.EXTRA_WATCH_MODEL, model)
                    dataMap.putLong(WearConstants.EXTRA_TIMESTAMP, timestamp)
                    dataMap.putInt("watch_app_version_code", BuildConfig.VERSION_CODE)
                }
                val putDataReq = dataMapRequest.asPutDataRequest().setUrgent()
                Wearable.getDataClient(this@WatchChargingMonitorService).putDataItem(putDataReq).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        batteryReceiver?.let {
            runCatching { unregisterReceiver(it) }
            batteryReceiver = null
        }
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "watch_charging_monitor"
        const val NOTIFICATION_ID = 1002
    }
}
