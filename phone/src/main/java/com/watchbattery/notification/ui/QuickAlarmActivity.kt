package com.watchbattery.notification.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.watchbattery.notification.WatchBatteryApp
import com.watchbattery.notification.core.constants.WearConstants
import com.watchbattery.notification.receiver.SmartAlarmReceiver
import com.watchbattery.notification.ui.theme.WatchBatteryTheme
import com.watchbattery.notification.ui.theme.WearBatteryTheme

class QuickAlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = WatchBatteryApp.instance.repository

        setContent {
            val settings by repository.settingsFlow.collectAsStateWithLifecycle()
            WatchBatteryTheme(
                colorTheme = settings.colorTheme,
                accentColor = settings.accentColor
            ) {
                QuickAlarmDialog(
                    onSelectMinutes = { minutes ->
                        scheduleReminder(minutes, settings.watchModel)
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }

    private fun scheduleReminder(minutes: Int, watchModel: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + minutes * 60 * 1000L

        val intent = Intent(this, SmartAlarmReceiver::class.java).apply {
            action = WearConstants.ACTION_TRIGGER_SMART_ALARM
            putExtra(WearConstants.EXTRA_EVENT_TYPE, WearConstants.EVENT_LOW_BATTERY)
            putExtra(WearConstants.EXTRA_BATTERY_LEVEL, 15)
            putExtra(WearConstants.EXTRA_WATCH_MODEL, watchModel.ifBlank { "Smartwatch" })
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, 301, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }

        Toast.makeText(this, "Reminder scheduled in $minutes minutes", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun QuickAlarmDialog(onSelectMinutes: (Int) -> Unit, onDismiss: () -> Unit) {
    val colors = WearBatteryTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Set Battery Reminder",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                listOf(10, 15, 30, 60).forEach { mins ->
                    Button(
                        onClick = { onSelectMinutes(mins) },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(text = "In $mins minutes", color = colors.accent)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss) {
                    Text(text = "Cancel", color = colors.textSecondary)
                }
            }
        }
    }
}
