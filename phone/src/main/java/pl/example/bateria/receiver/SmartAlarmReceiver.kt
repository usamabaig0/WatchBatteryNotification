package pl.example.bateria.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.example.bateria.WatchBatteryApp
import pl.example.bateria.core.constants.WearConstants
import pl.example.bateria.core.model.RepeatConfig
import pl.example.bateria.service.NotificationPlayerService

class SmartAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val eventType = intent.getStringExtra(WearConstants.EXTRA_EVENT_TYPE) ?: return
        val batteryLevel = intent.getIntExtra(WearConstants.EXTRA_BATTERY_LEVEL, 82)
        val watchModel = intent.getStringExtra(WearConstants.EXTRA_WATCH_MODEL) ?: "Smartwatch"

        CoroutineScope(Dispatchers.IO).launch {
            val settings = WatchBatteryApp.instance.repository.settingsFlow.value

            // Cancellation triggers (Kill switches):
            // 1. Low battery: if watch is already charging, abort!
            if (eventType == WearConstants.EVENT_LOW_BATTERY && settings.watchChargingStatus) {
                return@launch
            }
            // 2. High battery: if watch is already unplugged, abort!
            if (eventType == WearConstants.EVENT_FULLY_CHARGED && !settings.watchChargingStatus) {
                return@launch
            }

            // Launch NotificationPlayerService
            val playerIntent = Intent(context, NotificationPlayerService::class.java).apply {
                putExtra(WearConstants.EXTRA_EVENT_TYPE, eventType)
                putExtra(WearConstants.EXTRA_BATTERY_LEVEL, settings.watchBatteryLevel)
                putExtra(WearConstants.EXTRA_WATCH_MODEL, settings.watchModel)
            }
            ContextCompat.startForegroundService(context, playerIntent)

            // Reschedule repeating alarm if configured
            val (repeatConfig, repeatInterval) = if (eventType == WearConstants.EVENT_LOW_BATTERY) {
                Pair(settings.lowBatteryRepeatConfig, settings.lowBatteryRepeatInterval)
            } else {
                Pair(settings.highBatteryRepeatConfig, settings.highBatteryRepeatInterval)
            }

            if (repeatConfig != RepeatConfig.DO_NOT_REPEAT && repeatInterval > 0) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val triggerAtMillis = System.currentTimeMillis() + repeatInterval * 60 * 1000L

                val nextAlarmIntent = Intent(context, SmartAlarmReceiver::class.java).apply {
                    action = WearConstants.ACTION_TRIGGER_SMART_ALARM
                    putExtra(WearConstants.EXTRA_EVENT_TYPE, eventType)
                    putExtra(WearConstants.EXTRA_BATTERY_LEVEL, settings.watchBatteryLevel)
                    putExtra(WearConstants.EXTRA_WATCH_MODEL, settings.watchModel)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, 201, nextAlarmIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            }
        }
    }
}
