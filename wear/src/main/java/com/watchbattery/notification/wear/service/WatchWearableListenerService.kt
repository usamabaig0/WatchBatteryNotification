package com.watchbattery.notification.wear.service

import android.content.Context
import android.content.Intent
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import com.watchbattery.notification.core.constants.WearConstants
import com.watchbattery.notification.core.model.WatchSettingsSync
import com.watchbattery.notification.wear.receiver.WatchBatteryReceiver
import com.watchbattery.notification.wear.ui.WearMainActivity

class WatchWearableListenerService : WearableListenerService() {

    private val json = Json { ignoreUnknownKeys = true }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            WearConstants.PATH_REQUEST_STATUS_UPDATE -> {
                WatchBatteryReceiver.queryAndSendCurrentStatus(this)
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
                val launchIntent = Intent(this, WearMainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(launchIntent)
            }
        }
    }
}
