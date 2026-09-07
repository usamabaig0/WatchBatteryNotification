package com.watchbattery.notification.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.watchbattery.notification.core.constants.WearConstants
import com.watchbattery.notification.receiver.ChargingPollReceiver

object PhoneChargingPoller {

    const val POLL_REQUEST_CODE = 3001

    fun calculateDelay(currentLevel: Int, targetThreshold: Int): Long? {
        val delta = targetThreshold - currentLevel
        return when {
            delta > 15 -> 240_000L // 4 minutes
            delta in 6..15 -> 120_000L // 2 minutes
            delta in 1..5 -> 45_000L // 45 seconds
            else -> null // delta <= 0: target reached, no further poll needed
        }
    }

    fun scheduleNextPoll(context: Context, currentLevel: Int, targetThreshold: Int) {
        val delay = calculateDelay(currentLevel, targetThreshold) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, ChargingPollReceiver::class.java).apply {
            action = WearConstants.ACTION_PHONE_POLL_WATCH_CHARGING
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            POLL_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + delay,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + delay,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + delay,
                pendingIntent
            )
        }
    }

    fun cancelPoll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ChargingPollReceiver::class.java).apply {
            action = WearConstants.ACTION_PHONE_POLL_WATCH_CHARGING
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            POLL_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
