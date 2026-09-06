package com.watchbattery.notification.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watchbattery.notification.core.model.AppSettings
import com.watchbattery.notification.core.model.ConnectionStatus
import com.watchbattery.notification.core.physics.ChargePredictivePhysics
import com.watchbattery.notification.core.physics.PredictiveModelData
import com.watchbattery.notification.ui.components.ConnectionStatusPill
import com.watchbattery.notification.ui.components.HeroBatteryGauge
import com.watchbattery.notification.ui.components.TelemetryCard
import com.watchbattery.notification.ui.theme.*

@Composable
fun DashboardScreen(
    settings: AppSettings,
    connectionStatus: ConnectionStatus,
    predictiveModel: PredictiveModelData,
    onOpenOnWatch: () -> Unit,
    onRefreshConnection: () -> Unit,
    accentColor: Color
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val colors = WearBatteryTheme.colors

    val isConnected = connectionStatus == ConnectionStatus.CONNECTED && settings.watchBatteryLevel >= 0

    val timeRemainingText = if (!isConnected) {
        "Waiting for watch..."
    } else if (settings.watchChargingStatus) {
        val mins = ChargePredictivePhysics.calculateMinutesToTarget(
            settings.watchBatteryLevel,
            settings.highBatteryThreshold,
            predictiveModel
        )
        "~$mins min remaining"
    } else {
        val (hrs, mins) = ChargePredictivePhysics.calculateDischargeTimeRemaining(
            settings.watchBatteryLevel,
            false,
            predictiveModel
        )
        "~${hrs}h ${mins}m left"
    }

    val lastSyncText = if (settings.lastSyncTimestamp > 0L) {
        val diffMinutes = ((System.currentTimeMillis() - settings.lastSyncTimestamp) / 60000L).coerceAtLeast(0)
        if (diffMinutes < 1) "Just now" else "${diffMinutes}m ago"
    } else {
        "Never"
    }

    val watchDisplayName = if (settings.watchModel.isNotBlank()) {
        settings.watchModel
    } else {
        "No Smartwatch Linked"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(scrollState)
            .padding(horizontal = WearBatterySpacing.lg, vertical = WearBatterySpacing.md)
    ) {
        // App Bar / Header Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Watch Battery Notification",
                style = WearBatteryTypography.appTitle,
                color = colors.textPrimary
            )
            ConnectionStatusPill(status = connectionStatus)
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Watch Model Badge
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(WearBatteryRadius.pill),
                color = colors.surfaceElevated,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = watchDisplayName,
                    color = if (isConnected) colors.textPrimary else colors.textSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        // Hero Battery Gauge
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            HeroBatteryGauge(
                batteryLevel = if (isConnected) settings.watchBatteryLevel else -1,
                isCharging = if (isConnected) settings.watchChargingStatus else false,
                targetReached = isConnected && settings.watchBatteryLevel >= settings.highBatteryThreshold,
                timeRemainingText = timeRemainingText,
                accentColor = accentColor
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Unlinked informative card if not yet connected
        if (!isConnected) {
            TelemetryCard(modifier = Modifier.padding(bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BluetoothSearching,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Waiting for Smartwatch",
                            style = WearBatteryTypography.sectionTitle,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Install Watch Battery Notification on your Wear OS watch. Telemetry will sync automatically once connected.",
                            style = WearBatteryTypography.secondary,
                            color = colors.textSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Telemetry Grid Cards
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TelemetryCard(modifier = Modifier.weight(1f)) {
                Text(text = "Battery", style = WearBatteryTypography.secondary, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isConnected) "${settings.watchBatteryLevel}%" else "--",
                    style = WearBatteryTypography.telemetryValue,
                    color = colors.textPrimary
                )
            }

            TelemetryCard(modifier = Modifier.weight(1f)) {
                Text(text = "Status", style = WearBatteryTypography.secondary, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        !isConnected -> "Disconnected"
                        settings.watchChargingStatus -> "Charging"
                        else -> "Discharging"
                    },
                    style = WearBatteryTypography.telemetryValue,
                    color = if (isConnected && settings.watchChargingStatus) StatusCharging else colors.textPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TelemetryCard(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isConnected && settings.watchChargingStatus) "Time to full" else "Time to 0%",
                    style = WearBatteryTypography.secondary,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isConnected) timeRemainingText else "--",
                    style = WearBatteryTypography.telemetryValue,
                    color = colors.textPrimary
                )
            }

            TelemetryCard(modifier = Modifier.weight(1f)) {
                Text(text = "Last sync", style = WearBatteryTypography.secondary, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lastSyncText,
                    style = WearBatteryTypography.telemetryValue,
                    color = colors.textPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Action Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onOpenOnWatch,
                shape = RoundedCornerShape(WearBatteryRadius.button),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Watch, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Open on watch", color = Color.White, fontSize = 14.sp)
            }

            OutlinedButton(
                onClick = {
                    onRefreshConnection()
                    Toast.makeText(context, "Pinging watch for telemetry...", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(WearBatteryRadius.button),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(colors.border)),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = colors.textPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Refresh", color = colors.textPrimary, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
