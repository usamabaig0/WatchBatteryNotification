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

                // Schedule snooze alarm in 5 minutes
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val triggerAtMillis = System.currentTimeMillis() + 5 * 60 * 1000L

                val alarmIntent = Intent(context, SmartAlarmReceiver::class.java).apply {
                    action = WearConstants.ACTION_TRIGGER_SMART_ALARM
                    putExtra(WearConstants.EXTRA_EVENT_TYPE, eventType)
                    putExtra(WearConstants.EXTRA_BATTERY_LEVEL, batteryLevel)
                    putExtra(WearConstants.EXTRA_WATCH_MODEL, watchModel)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, 201, alarmIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            }

            WearConstants.ACTION_STOP_REPEAT -> {
                // Completely stop repeating alarm for this cycle
                notificationManager.cancel(1001)
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val alarmIntent = Intent(context, SmartAlarmReceiver::class.java).apply {
                    action = WearConstants.ACTION_TRIGGER_SMART_ALARM
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, 201, alarmIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                alarmManager.cancel(pendingIntent)
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
