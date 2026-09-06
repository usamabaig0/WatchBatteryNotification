package com.watchbattery.notification

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import com.watchbattery.notification.core.constants.WearConstants
import com.watchbattery.notification.data.AppSettingsRepository

class WatchBatteryApp : Application() {

    lateinit var repository: AppSettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = AppSettingsRepository(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Player Channel for sound, TTS, and haptics
            val playerChannel = NotificationChannel(
                WearConstants.CHANNEL_ID_PLAYER,
                getString(R.string.channel_player_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_player_desc)
                enableVibration(true)
                enableLights(true)
            }

            // 2. Smart Alarm Channel for repeating alarms and snooze
            val alarmChannel = NotificationChannel(
                WearConstants.CHANNEL_ID_ALARM,
                getString(R.string.channel_alarm_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_alarm_desc)
                enableVibration(true)
                enableLights(true)
                setBypassDnd(true)
            }

            // 3. Charging Monitor Channel for live progress bar
            val chargingChannel = NotificationChannel(
                WearConstants.CHANNEL_ID_CHARGING_MONITOR,
                getString(R.string.channel_charging_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_charging_desc)
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }

            notificationManager.createNotificationChannels(
                listOf(playerChannel, alarmChannel, chargingChannel)
            )
        }
    }

    companion object {
        lateinit var instance: WatchBatteryApp
            private set
    }
}
