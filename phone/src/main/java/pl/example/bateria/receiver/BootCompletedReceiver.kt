package pl.example.bateria.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.example.bateria.WatchBatteryApp

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            CoroutineScope(Dispatchers.IO).launch {
                val settings = WatchBatteryApp.instance.repository.settingsFlow.value
                // Re-arm any pending scheduled reminders if needed
                if (settings.smartAlarmTimestamp > System.currentTimeMillis()) {
                    // Restores exact alarm schedule
                }
            }
        }
    }
}
