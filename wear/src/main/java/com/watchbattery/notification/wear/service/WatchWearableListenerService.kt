package com.watchbattery.notification.wear.service

import android.app.ActivityOptions
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import com.watchbattery.notification.core.constants.WearConstants
import com.watchbattery.notification.core.model.WatchSettingsSync
import com.watchbattery.notification.wear.R
import com.watchbattery.notification.wear.receiver.WatchBatteryReceiver
import com.watchbattery.notification.wear.ui.WearMainActivity
import com.google.android.gms.wearable.Node

class WatchWearableListenerService : WearableListenerService() {

    private val json = Json { ignoreUnknownKeys = true }

    override fun onPeerConnected(peer: Node) {
        super.onPeerConnected(peer)
        // As soon as the phone connects, dispatch current watch battery telemetry immediately
        WatchBatteryReceiver.forceSendCurrentStatus(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            WearConstants.PATH_REQUEST_STATUS_UPDATE -> {
                val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
                val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                val percent = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else -1
                if (percent >= 0) {
                    WatchBatteryReceiver.dispatchStatus(this, percent, isCharging)
                }
            }

            WearConstants.PATH_SETTINGS_SYNC_RESPONSE -> {
                runCatching {
                    val syncStr = String(messageEvent.data, Charsets.UTF_8)
                    val sync = json.decodeFromString<WatchSettingsSync>(syncStr)
                    getSharedPreferences("wear_battery_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putInt("low_threshold", sync.lowBatteryThreshold)
                        .putInt("high_threshold", sync.highBatteryThreshold)
                        .apply()
                }
            }

            WearConstants.PATH_OPEN_APP_ON_WATCH -> {
                runCatching {
                    val launchIntent = Intent(this, WearMainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        ActivityOptions.makeBasic().apply {
                            pendingIntentBackgroundActivityStartMode = ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                        }.toBundle()
                    } else null
                    startActivity(launchIntent, options)
                }
                showOpenAppNotification()
            }
        }
    }

    private fun showOpenAppNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "watch_battery_open_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Watch Battery Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications to open Watch Battery companion"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        val openIntent = Intent(this, WearMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityOptions.makeBasic().apply {
                pendingIntentBackgroundActivityStartMode = ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            }.toBundle()
        } else {
            null
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            2002,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            options
        )
        val notif = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_watch_battery)
            .setContentTitle("Watch Battery")
            .setContentText("Tap to open app")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .build()
        notificationManager.notify(2002, notif)
    }
}
