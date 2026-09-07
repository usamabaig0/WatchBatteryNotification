package com.watchbattery.notification.wear.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import android.net.Uri
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.*
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.watchbattery.notification.core.constants.WearConstants
import com.watchbattery.notification.wear.receiver.WatchBatteryReceiver

class WearMainActivity : ComponentActivity() {

    private var batteryReceiver: BroadcastReceiver? = null
    private val batteryLevelState = mutableStateOf(0)
    private val isChargingState = mutableStateOf(false)
    private val isPhoneConnectedState = mutableStateOf(false)
    private val isSyncedState = mutableStateOf(false)

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Notification permission needed for alerts", Toast.LENGTH_SHORT).show()
        }
    }

    private val capabilityListener = CapabilityClient.OnCapabilityChangedListener { capabilityInfo ->
        val hasNodes = capabilityInfo.nodes.isNotEmpty()
        isPhoneConnectedState.value = hasNodes
        if (!hasNodes) {
            isSyncedState.value = false
        }
    }

    private val messageListener = com.google.android.gms.wearable.MessageClient.OnMessageReceivedListener { messageEvent ->
        if (messageEvent.path == WearConstants.PATH_SETTINGS_SYNC_RESPONSE ||
            messageEvent.path == WearConstants.PATH_REQUEST_STATUS_UPDATE) {
            isPhoneConnectedState.value = true
            isSyncedState.value = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()

        // Read initial battery state from system
        val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val realLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        if (realLevel in 0..100) {
            batteryLevelState.value = realLevel
        }
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

        checkPhoneConnection()
        requestSettingsSync()

        setContent {
            val level by batteryLevelState
            val isCharging by isChargingState
            val isPhoneConnected by isPhoneConnectedState
            val isSynced by isSyncedState

            WearBatteryScreen(
                batteryLevel = level,
                isCharging = isCharging,
                watchModel = Build.MODEL ?: "Smartwatch",
                isPhoneConnected = isPhoneConnected,
                isSynced = isSynced,
                onOpenOnPhone = { openOnPhone() },
                onForceSync = {
                    WatchBatteryReceiver.forceSendCurrentStatus(this)
                    checkPhoneConnection()
                    isSyncedState.value = true
                    Toast.makeText(this, "Telemetry synced with phone", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        checkPhoneConnection()
        runCatching {
            Wearable.getCapabilityClient(this).addListener(
                capabilityListener,
                Uri.parse("wear://*/${WearConstants.CAPABILITY_PHONE_APP}"),
                CapabilityClient.FILTER_ALL
            )
        }
        runCatching {
            Wearable.getMessageClient(this).addListener(messageListener)
        }
    }

    override fun onPause() {
        super.onPause()
        runCatching {
            Wearable.getCapabilityClient(this).removeListener(capabilityListener)
        }
        runCatching {
            Wearable.getMessageClient(this).removeListener(messageListener)
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun checkPhoneConnection() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val nodes = Wearable.getNodeClient(this@WearMainActivity).connectedNodes.await()
                val isConnected = nodes.isNotEmpty()
                launch(Dispatchers.Main) {
                    isPhoneConnectedState.value = isConnected
                    if (!isConnected) {
                        isSyncedState.value = false
                    }
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    isPhoneConnectedState.value = false
                    isSyncedState.value = false
                }
            }
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
        WatchBatteryReceiver.forceSendCurrentStatus(this)
    }

    private fun requestSettingsSync() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val nodes = Wearable.getNodeClient(this@WearMainActivity).connectedNodes.await()
                if (nodes.isNotEmpty()) {
                    for (node in nodes) {
                        Wearable.getMessageClient(this@WearMainActivity)
                            .sendMessage(node.id, WearConstants.PATH_REQUEST_SETTINGS_SYNC, byteArrayOf()).await()
                    }
                    WatchBatteryReceiver.forceSendCurrentStatus(this@WearMainActivity)
                    launch(Dispatchers.Main) {
                        isSyncedState.value = true
                    }
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

                // Method A: RemoteActivityHelper with deep link watchbattery://open
                val remoteActivityHelper = RemoteActivityHelper(this@WearMainActivity)
                val deepLinkIntent = Intent(Intent.ACTION_VIEW)
                    .addCategory(Intent.CATEGORY_DEFAULT)
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                    .setData(Uri.parse(WearConstants.DEEP_LINK_URI))
                    .setPackage("com.watchbattery.notification")

                for (node in nodes) {
                    runCatching {
                        remoteActivityHelper.startRemoteActivity(deepLinkIntent, node.id)
                    }
                    // Method B: Direct MessageClient trigger
                    runCatching {
                        Wearable.getMessageClient(this@WearMainActivity)
                            .sendMessage(node.id, WearConstants.PATH_OPEN_APP_ON_PHONE, byteArrayOf()).await()
                    }
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
    isPhoneConnected: Boolean,
    isSynced: Boolean,
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

            // Dynamic Phone Connection Badge
            val badgeColor = when {
                !isPhoneConnected -> Color(0xFFC95158)
                isSynced -> successGreen
                else -> Color(0xFFC58A26)
            }
            val badgeText = when {
                !isPhoneConnected -> "Phone Disconnected"
                isSynced -> "Synced with Phone"
                else -> "Connected (Syncing...)"
            }
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
                        .background(badgeColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = badgeText,
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
