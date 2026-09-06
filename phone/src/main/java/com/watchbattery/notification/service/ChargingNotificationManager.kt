package com.watchbattery.notification.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.watchbattery.notification.R
import com.watchbattery.notification.core.constants.WearConstants
import com.watchbattery.notification.receiver.NotificationActionReceiver
import com.watchbattery.notification.ui.MainActivity

object ChargingNotificationManager {

    private const val NOTIFICATION_ID_CHARGING = 2002

    fun showChargingNotification(
        context: Context,
        batteryLevel: Int,
        watchModel: String,
        minutesRemaining: Int
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openAppIntent = Intent(context, MainActivity::class.java).let {
            PendingIntent.getActivity(context, 10, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = WearConstants.ACTION_DISMISS_CHARGING_BAR
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, 11, dismissIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val timeText = if (minutesRemaining > 0) "~$minutesRemaining min remaining" else "Almost full"

        val notification = NotificationCompat.Builder(context, WearConstants.CHANNEL_ID_CHARGING_MONITOR)
            .setContentTitle("Watch charging · $watchModel")
            .setContentText("$batteryLevel% · $timeText")
            .setSmallIcon(R.drawable.ic_stat_watch_battery)
            .setProgress(100, batteryLevel.coerceIn(0, 100), false)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .setDeleteIntent(dismissPendingIntent)
            .addAction(0, "Open App", openAppIntent)
            .addAction(0, "Hide", dismissPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(NOTIFICATION_ID_CHARGING, notification)
    }

    fun dismissChargingNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_CHARGING)
    }
}
