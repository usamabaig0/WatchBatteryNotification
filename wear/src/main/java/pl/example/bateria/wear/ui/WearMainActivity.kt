package pl.example.bateria.wear.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.*
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import pl.example.bateria.core.constants.WearConstants
import pl.example.bateria.wear.receiver.WatchBatteryReceiver

class WearMainActivity : ComponentActivity() {

    private var batteryReceiver: BroadcastReceiver? = null
    private val batteryLevelState = mutableStateOf(82)
    private val isChargingState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read initial battery state
        val initialFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val initialBattery = registerReceiver(null, initialFilter)
        updateBatteryFromIntent(initialBattery)

        // Register dynamic receiver while activity is in foreground
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                updateBatteryFromIntent(intent)
            }
        }
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        // Initial sync request to phone
        requestSettingsSync()

        setContent {
            val level by batteryLevelState
            val isCharging by isChargingState

            WearBatteryScreen(
                batteryLevel = level,
                isCharging = isCharging,
                watchModel = Build.MODEL ?: "Smartwatch",
                onOpenOnPhone = { openOnPhone() },
                onForceSync = {
                    WatchBatteryReceiver.queryAndSendCurrentStatus(this)
                    Toast.makeText(this, "Telemetry synced with phone", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun updateBatteryFromIntent(intent: Intent?) {
        if (intent == null) return
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level >= 0 && scale > 0) {
            batteryLevelState.value = ((level.toFloat() / scale.toFloat()) * 100).toInt()
        }
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        isChargingState.value = (status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL)
    }

    private fun requestSettingsSync() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val nodes = Wearable.getNodeClient(this@WearMainActivity).connectedNodes.await()
                for (node in nodes) {
                    Wearable.getMessageClient(this@WearMainActivity)
                        .sendMessage(node.id, WearConstants.PATH_REQUEST_SETTINGS_SYNC, byteArrayOf()).await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun openOnPhone() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val nodes = Wearable.getNodeClient(this@WearMainActivity).connectedNodes.await()
                if (nodes.isEmpty()) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(this@WearMainActivity, "Phone not connected", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                for (node in nodes) {
                    Wearable.getMessageClient(this@WearMainActivity)
                        .sendMessage(node.id, WearConstants.PATH_OPEN_APP_ON_PHONE, byteArrayOf()).await()
                }
                launch(Dispatchers.Main) {
                    Toast.makeText(this@WearMainActivity, "Opening on phone...", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(this@WearMainActivity, "Error reaching phone", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        batteryReceiver?.let {
            runCatching { unregisterReceiver(it) }
        }
    }
}

@Composable
fun WearBatteryScreen(
    batteryLevel: Int,
    isCharging: Boolean,
    watchModel: String,
    onOpenOnPhone: () -> Unit,
    onForceSync: () -> Unit
) {
    val professionalBlue = Color(0xFF6AAFE4)
    val successGreen = Color(0xFF3E9B68)

    Scaffold(
        timeText = { TimeText() },
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101214))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 20.dp)
        ) {
            // Large Hero Percentage (Section 17 of DESIGN.md)
            Text(
                text = "$batteryLevel%",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF2F4F7)
            )

            // Status Label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                if (isCharging) {
                    Text(text = "⚡ ", color = professionalBlue, fontSize = 14.sp)
                }
                Text(
                    text = if (isCharging) "Charging" else "Discharging",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isCharging) professionalBlue else Color(0xFFA7AFB8)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Watch Model
            Text(
                text = watchModel,
                fontSize = 11.sp,
                color = Color(0xFF626A73)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Synced with Phone Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(Color(0xFF20242A))
                    .border(1.dp, Color(0xFF2B3138), RoundedCornerShape(9999.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(successGreen)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Synced with Phone",
                    fontSize = 10.sp,
                    color = Color(0xFFF2F4F7)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CompactButton(
                    onClick = onOpenOnPhone,
                    colors = ButtonDefaults.buttonColors(backgroundColor = professionalBlue),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Phone", fontSize = 11.sp, color = Color.White)
                }

                CompactButton(
                    onClick = onForceSync,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF20242A)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Sync", fontSize = 11.sp, color = Color(0xFFF2F4F7))
                }
            }
        }
    }
}
