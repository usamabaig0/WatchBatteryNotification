package com.watchbattery.notification.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.watchbattery.notification.WatchBatteryApp
import com.watchbattery.notification.core.constants.WearConstants
import com.watchbattery.notification.core.model.RepeatConfig
import com.watchbattery.notification.service.NotificationPlayerService
import com.watchbattery.notification.ui.MainActivity

class SmartAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val eventType = intent.getStringExtra(WearConstants.EXTRA_EVENT_TYPE) ?: return
        val batteryLevel = intent.getIntExtra(WearConstants.EXTRA_BATTERY_LEVEL, 0)
        val watchModel = intent.getStringExtra(WearConstants.EXTRA_WATCH_MODEL) ?: "Smartwatch"

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WatchBattery:SmartAlarmWakeLock"
        )
        wakeLock?.acquire(15000L)

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = WatchBatteryApp.instance.repository.settingsFlow.value

                // Cancellation triggers (Kill switches):
                // 1. Low battery: if watch is already charging, abort!
                if (eventType == WearConstants.EVENT_LOW_BATTERY && settings.watchChargingStatus) {
                    cancel(context)
                    return@launch
                }
                // 2. High battery: if watch is already unplugged, abort!
                if (eventType == WearConstants.EVENT_FULLY_CHARGED && !settings.watchChargingStatus) {
                    cancel(context)
                    return@launch
                }

                // Launch NotificationPlayerService
                val playerIntent = Intent(context, NotificationPlayerService::class.java).apply {
                    putExtra(WearConstants.EXTRA_EVENT_TYPE, eventType)
                    putExtra(WearConstants.EXTRA_BATTERY_LEVEL, if (settings.watchBatteryLevel >= 0) settings.watchBatteryLevel else batteryLevel)
                    putExtra(WearConstants.EXTRA_WATCH_MODEL, settings.watchModel.ifBlank { watchModel })
                }
                ContextCompat.startForegroundService(context, playerIntent)

                // Reschedule repeating alarm if configured
                val repeatConfig: RepeatConfig = if (eventType == WearConstants.EVENT_LOW_BATTERY) {
                    settings.lowBatteryRepeatConfig
                } else {
                    settings.highBatteryRepeatConfig
                }

                val repeatIntervalSeconds: Int = if (eventType == WearConstants.EVENT_LOW_BATTERY) {
                    settings.lowBatteryRepeatIntervalSeconds
                } else {
                    settings.highBatteryRepeatIntervalSeconds
                }

                if (repeatConfig != RepeatConfig.DO_NOT_REPEAT && repeatIntervalSeconds > 0) {
                    scheduleNext(
                        context = context,
                        eventType = eventType,
                        batteryLevel = settings.watchBatteryLevel,
                        watchModel = settings.watchModel.ifBlank { watchModel },
                        intervalSeconds = repeatIntervalSeconds
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                wakeLock?.let {
                    if (it.isHeld) {
                        runCatching { it.release() }
                    }
                }
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val SMART_ALARM_REQUEST_CODE = 201

        fun scheduleNext(
            context: Context,
            eventType: String,
            batteryLevel: Int,
            watchModel: String,
            intervalSeconds: Int
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val triggerAtMillis = System.currentTimeMillis() + (intervalSeconds.coerceAtLeast(5) * 1000L)

            val nextAlarmIntent = Intent(context, SmartAlarmReceiver::class.java).apply {
                action = WearConstants.ACTION_TRIGGER_SMART_ALARM
                putExtra(WearConstants.EXTRA_EVENT_TYPE, eventType)
                putExtra(WearConstants.EXTRA_BATTERY_LEVEL, batteryLevel)
                putExtra(WearConstants.EXTRA_WATCH_MODEL, watchModel)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                SMART_ALARM_REQUEST_CODE,
                nextAlarmIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val showIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

            try {
                if (canExact) {
                    // setAlarmClock guarantees execution down to the second without 9-minute Doze throttling
                    val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent)
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } catch (e: SecurityException) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } catch (e2: Exception) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } catch (e: Exception) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }

        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val alarmIntent = Intent(context, SmartAlarmReceiver::class.java).apply {
                action = WearConstants.ACTION_TRIGGER_SMART_ALARM
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                SMART_ALARM_REQUEST_CODE,
                alarmIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
