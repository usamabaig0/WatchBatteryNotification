package com.watchbattery.notification.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.net.Uri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.watchbattery.notification.WatchBatteryApp
import com.watchbattery.notification.core.constants.WearConstants
import com.watchbattery.notification.core.model.ConnectionStatus
import com.watchbattery.notification.core.model.WatchSettingsSync
import com.watchbattery.notification.core.physics.PredictiveModelData
import com.watchbattery.notification.ui.screens.*
import com.watchbattery.notification.ui.theme.*

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Notifications are required to alert when watch charges or gets low", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
        refreshConnectionState()

        setContent {
            val repository = WatchBatteryApp.instance.repository
            val settings by repository.settingsFlow.collectAsStateWithLifecycle()
            val connectionStatus by repository.connectionStatusFlow.collectAsStateWithLifecycle()
            val predictiveModel by repository.predictiveModelFlow.collectAsStateWithLifecycle(initialValue = PredictiveModelData())
            val batteryHistory by repository.batteryHistoryFlow.collectAsStateWithLifecycle(initialValue = emptyList())

            WatchBatteryTheme(
                colorTheme = settings.colorTheme,
                accentColor = settings.accentColor
            ) {
                val themeColors = WearBatteryTheme.colors
                if (!settings.welcomeScreenShown) {
                    OnboardingScreen(
                        onFinished = {
                            lifecycleScope.launch {
                                repository.updateSettings { it.copy(welcomeScreenShown = true) }
                            }
                        },
                        accentColor = themeColors.accent
                    )
                } else {
                    MainAppScaffold(
                        settings = settings,
                        connectionStatus = connectionStatus,
                        predictiveModel = predictiveModel,
                        batteryHistory = batteryHistory,
                        onUpdateSettings = { transform ->
                            lifecycleScope.launch {
                                repository.updateSettings(transform)
                                syncSettingsToWatch()
                            }
                        },
                        onResetHabits = {
                            lifecycleScope.launch {
                                repository.resetHabits()
                                Toast.makeText(this@MainActivity, "Battery habits reset", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onOpenOnWatch = { openOnWatch() },
                        onRefreshConnection = {
                            refreshConnectionState()
                            sendRemoteWatchCommand(WearConstants.PATH_REQUEST_STATUS_UPDATE)
                        },
                        accentColor = themeColors.accent
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshConnectionState()
    }

    private fun refreshConnectionState() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val nodeClient = Wearable.getNodeClient(this@MainActivity)
                val nodes = nodeClient.connectedNodes.await()
                val isConnected = nodes.isNotEmpty()
                val repo = WatchBatteryApp.instance.repository
                val status = if (isConnected) ConnectionStatus.CONNECTED else ConnectionStatus.DISCONNECTED
                repo.setConnectionStatus(status)

                if (isConnected) {
                    val messageClient = Wearable.getMessageClient(this@MainActivity)
                    for (node in nodes) {
                        runCatching {
                            messageClient.sendMessage(node.id, WearConstants.PATH_REQUEST_STATUS_UPDATE, byteArrayOf()).await()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun openOnWatch() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val nodeClient = Wearable.getNodeClient(this@MainActivity)
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "No watch connected via Bluetooth", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Method A: RemoteActivityHelper with deep link watchbattery://open
                val remoteActivityHelper = RemoteActivityHelper(this@MainActivity)
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
                        Wearable.getMessageClient(this@MainActivity)
                            .sendMessage(node.id, WearConstants.PATH_OPEN_APP_ON_WATCH, byteArrayOf()).await()
                    }
                }
                launch(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Opening app on watch...", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error communicating with watch", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun syncSettingsToWatch() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val nodes = Wearable.getNodeClient(this@MainActivity).connectedNodes.await()
                if (nodes.isEmpty()) return@launch

                val settings = WatchBatteryApp.instance.repository.settingsFlow.value
                val syncPayload = WatchSettingsSync(
                    lowBatteryThreshold = settings.lowBatteryThreshold,
                    highBatteryThreshold = settings.highBatteryThreshold
                )
                val bytes = Json.encodeToString(syncPayload).toByteArray(Charsets.UTF_8)
                val messageClient = Wearable.getMessageClient(this@MainActivity)
                for (node in nodes) {
                    runCatching {
                        messageClient.sendMessage(node.id, WearConstants.PATH_SETTINGS_SYNC_RESPONSE, bytes).await()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun sendRemoteWatchCommand(path: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val nodeClient = Wearable.getNodeClient(this@MainActivity)
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "No watch connected via Bluetooth", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val messageClient = Wearable.getMessageClient(this@MainActivity)
                for (node in nodes) {
                    messageClient.sendMessage(node.id, path, byteArrayOf()).await()
                }
                launch(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Command sent to watch", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error communicating with watch", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

enum class NavigationItem(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Watch),
    ANALYTICS("Analytics", Icons.Default.BarChart),
    SETTINGS("Settings", Icons.Default.Tune),
    HELP("Help", Icons.Default.HelpOutline)
}

@Composable
fun MainAppScaffold(
    settings: com.watchbattery.notification.core.model.AppSettings,
    connectionStatus: com.watchbattery.notification.core.model.ConnectionStatus,
    predictiveModel: PredictiveModelData,
    batteryHistory: List<com.watchbattery.notification.core.physics.BatterySample>,
    onUpdateSettings: ((com.watchbattery.notification.core.model.AppSettings) -> com.watchbattery.notification.core.model.AppSettings) -> Unit,
    onResetHabits: () -> Unit,
    onOpenOnWatch: () -> Unit,
    onRefreshConnection: () -> Unit,
    accentColor: Color
) {
    var selectedItem by remember { mutableStateOf(NavigationItem.DASHBOARD) }
    val colors = WearBatteryTheme.colors

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = colors.surfaceElevated,
                tonalElevation = 0.dp
            ) {
                NavigationItem.values().forEach { item ->
                    val selected = selectedItem == item
                    NavigationBarItem(
                        icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                        label = { Text(text = item.label) },
                        selected = selected,
                        onClick = { selectedItem = item },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = colors.accent,
                            indicatorColor = colors.accent,
                            unselectedIconColor = colors.textSecondary,
                            unselectedTextColor = colors.textSecondary
                        )
                    )
                }
            }
        },
        containerColor = colors.background
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedItem) {
                NavigationItem.DASHBOARD -> DashboardScreen(
                    settings = settings,
                    connectionStatus = connectionStatus,
                    predictiveModel = predictiveModel,
                    onOpenOnWatch = onOpenOnWatch,
                    onRefreshConnection = onRefreshConnection,
                    accentColor = accentColor
                )
                NavigationItem.ANALYTICS -> AnalyticsScreen(
                    predictiveModel = predictiveModel,
                    batteryHistory = batteryHistory,
                    onResetHabits = onResetHabits,
                    accentColor = accentColor
                )
                NavigationItem.SETTINGS -> SettingsScreen(
                    settings = settings,
                    onUpdateSettings = onUpdateSettings,
                    accentColor = accentColor
                )
                NavigationItem.HELP -> TroubleshootingFaqScreen(
                    watchModel = settings.watchModel,
                    accentColor = accentColor
                )
            }
        }
    }
}
