package com.watchbattery.notification.service

import android.content.Intent
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.watchbattery.notification.WatchBatteryApp
import com.watchbattery.notification.core.constants.WearConstants
import com.watchbattery.notification.core.model.BatteryStatus
import com.watchbattery.notification.core.model.ConnectionStatus
import com.watchbattery.notification.core.model.WatchSettingsSync
import com.watchbattery.notification.core.physics.ChargePredictivePhysics
import com.watchbattery.notification.ui.MainActivity
import java.nio.ByteBuffer

class PhoneWearableListenerService : WearableListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val data = messageEvent.data

        when (path) {
            WearConstants.PATH_WATCH_STATUS -> {
                runCatching {
                    val statusStr = String(data, Charsets.UTF_8)
                    val status = json.decodeFromString<BatteryStatus>(statusStr)
                    handleStatusUpdate(status)
                }
            }

            WearConstants.PATH_WATCH_CHARGING_STARTED -> {
                val level = extractInt(data)
                triggerAlert(WearConstants.EVENT_CHARGING_STARTED, level)
            }

            WearConstants.PATH_WATCH_CHARGING_STOPPED -> {
                val level = extractInt(data)
                triggerAlert(WearConstants.EVENT_CHARGING_STOPPED, level)
                ChargingNotificationManager.dismissChargingNotification(applicationContext)
            }

            WearConstants.PATH_WATCH_FULLY_CHARGED -> {
                val level = extractInt(data)
                triggerAlert(WearConstants.EVENT_FULLY_CHARGED, level)
            }

            WearConstants.PATH_WATCH_LOW_BATTERY -> {
                val level = extractInt(data)
                triggerAlert(WearConstants.EVENT_LOW_BATTERY, level)
            }

            WearConstants.PATH_REQUEST_SETTINGS_SYNC -> {
                respondToSettingsSync(messageEvent.sourceNodeId)
            }

            WearConstants.PATH_OPEN_APP_ON_PHONE -> {
                val launchIntent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(launchIntent)
            }
        }
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        val hasConnectedNodes = capabilityInfo.nodes.any { it.isNearby }
        val status = if (hasConnectedNodes) ConnectionStatus.CONNECTED else ConnectionStatus.DISCONNECTED
        WatchBatteryApp.instance.repository.setConnectionStatus(status)
    }

    private fun handleStatusUpdate(status: BatteryStatus) {
        scope.launch {
            val repo = WatchBatteryApp.instance.repository
            val prevSettings = repo.settingsFlow.value

            repo.updateTelemetry(status.battery_level, status.is_charging, status.watch_model)
            repo.setConnectionStatus(ConnectionStatus.CONNECTED)

            // Update predictive model estimates
            val predictiveModel = repo.predictiveModelFlow
            val estMinutes = if (status.is_charging) {
                ChargePredictivePhysics.calculateMinutesToTarget(
                    status.battery_level,
                    prevSettings.highBatteryThreshold,
                    com.watchbattery.notification.core.physics.PredictiveModelData()
                )
            } else 0

            // Update persistent charging drawer notification
            if (status.is_charging && prevSettings.chargingNotificationEnabled && !prevSettings.chargingNotificationHiddenByUser) {
                ChargingNotificationManager.showChargingNotification(
                    applicationContext,
                    status.battery_level,
                    status.watch_model,
                    estMinutes
                )
            } else if (!status.is_charging) {
                ChargingNotificationManager.dismissChargingNotification(applicationContext)
            }

            // Check if battery level crossed thresholds
            if (status.battery_level <= prevSettings.lowBatteryThreshold && !status.is_charging && prevSettings.watchBatteryLevel > prevSettings.lowBatteryThreshold) {
                triggerAlert(WearConstants.EVENT_LOW_BATTERY, status.battery_level)
            } else if (status.battery_level >= prevSettings.highBatteryThreshold && status.is_charging && prevSettings.watchBatteryLevel < prevSettings.highBatteryThreshold) {
                triggerAlert(WearConstants.EVENT_FULLY_CHARGED, status.battery_level)
            }
        }
    }

    private fun triggerAlert(eventType: String, level: Int) {
        val intent = Intent(this, NotificationPlayerService::class.java).apply {
            putExtra(WearConstants.EXTRA_EVENT_TYPE, eventType)
            putExtra(WearConstants.EXTRA_BATTERY_LEVEL, level)
            putExtra(WearConstants.EXTRA_WATCH_MODEL, WatchBatteryApp.instance.repository.settingsFlow.value.watchModel)
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun respondToSettingsSync(nodeId: String) {
        scope.launch {
            val settings = WatchBatteryApp.instance.repository.settingsFlow.value
            val syncPayload = WatchSettingsSync(
                lowBatteryThreshold = settings.lowBatteryThreshold,
                highBatteryThreshold = settings.highBatteryThreshold
            )
            val bytes = json.encodeToString(syncPayload).toByteArray(Charsets.UTF_8)
            Wearable.getMessageClient(this@PhoneWearableListenerService)
                .sendMessage(nodeId, WearConstants.PATH_SETTINGS_SYNC_RESPONSE, bytes)
        }
    }

    private fun extractInt(data: ByteArray?): Int {
        if (data == null || data.isEmpty()) return 80
        return try {
            if (data.size == 4) ByteBuffer.wrap(data).int else String(data, Charsets.UTF_8).toInt()
        } catch (e: Exception) {
            80
        }
    }
}
