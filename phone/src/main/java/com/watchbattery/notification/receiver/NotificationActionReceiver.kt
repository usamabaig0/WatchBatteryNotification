package com.watchbattery.notification.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.watchbattery.notification.WatchBatteryApp
import com.watchbattery.notification.core.constants.WearConstants
import com.watchbattery.notification.core.physics.ChargePredictivePhysics
import com.watchbattery.notification.core.physics.PredictiveModelData
import com.watchbattery.notification.service.ChargingNotificationManager

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (intent.action) {
            WearConstants.ACTION_SNOOZE -> {
                // Cancel current notification/sound
                notificationManager.cancel(1001)

                val eventType = intent.getStringExtra(WearConstants.EXTRA_EVENT_TYPE) ?: WearConstants.EVENT_LOW_BATTERY
                val batteryLevel = intent.getIntExtra(WearConstants.EXTRA_BATTERY_LEVEL, 15)
                val watchModel = intent.getStringExtra(WearConstants.EXTRA_WATCH_MODEL) ?: "Smartwatch"

                // Stop current audio and schedule snooze alarm in 5 minutes (300 seconds)
                context.stopService(Intent(context, com.watchbattery.notification.service.NotificationPlayerService::class.java))
                SmartAlarmReceiver.scheduleNext(
                    context = context,
                    eventType = eventType,
                    batteryLevel = batteryLevel,
                    watchModel = watchModel,
                    intervalSeconds = 300
                )
            }

            WearConstants.ACTION_STOP_REPEAT -> {
                // Completely stop repeating alarm and active playback for this cycle
                notificationManager.cancel(1001)
                context.stopService(Intent(context, com.watchbattery.notification.service.NotificationPlayerService::class.java))
                com.watchbattery.notification.receiver.SmartAlarmReceiver.cancel(context)
            }

            WearConstants.ACTION_DISMISS_CHARGING_BAR -> {
                ChargingNotificationManager.dismissChargingNotification(context)
                CoroutineScope(Dispatchers.IO).launch {
                    WatchBatteryApp.instance.repository.updateSettings {
                        it.copy(chargingNotificationHiddenByUser = true)
                    }
                }
            }

            WearConstants.ACTION_RESTORE_CHARGING_BAR -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val repo = WatchBatteryApp.instance.repository
                    repo.updateSettings { it.copy(chargingNotificationHiddenByUser = false) }
                    val current = repo.settingsFlow.value
                    if (current.watchChargingStatus) {
                        val estMinutes = ChargePredictivePhysics.calculateMinutesToTarget(
                            current.watchBatteryLevel,
                            current.highBatteryThreshold,
                            PredictiveModelData()
                        )
                        ChargingNotificationManager.showChargingNotification(
                            context,
                            current.watchBatteryLevel,
                            current.watchModel,
                            estMinutes
                        )
                    }
                }
            }
        }
    }
}
