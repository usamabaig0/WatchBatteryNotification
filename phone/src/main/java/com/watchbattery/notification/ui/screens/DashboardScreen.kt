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

    val timeRemainingText = if (settings.watchChargingStatus) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
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
                text = "Wear Battery",
                style = WearBatteryTypography.appTitle,
                color = DarkTextPrimary
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
                color = DarkSurfaceElevated,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = settings.watchModel,
                    color = DarkTextSecondary,
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
                batteryLevel = settings.watchBatteryLevel,
                isCharging = settings.watchChargingStatus,
                targetReached = settings.watchBatteryLevel >= settings.highBatteryThreshold,
                timeRemainingText = timeRemainingText,
                accentColor = accentColor
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Telemetry Grid Cards
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TelemetryCard(modifier = Modifier.weight(1f)) {
                Text(text = "Battery", style = WearBatteryTypography.secondary, color = DarkTextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${settings.watchBatteryLevel}%",
                    style = WearBatteryTypography.telemetryValue,
                    color = DarkTextPrimary
                )
            }

            TelemetryCard(modifier = Modifier.weight(1f)) {
                Text(text = "Status", style = WearBatteryTypography.secondary, color = DarkTextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (settings.watchChargingStatus) "Charging" else "Discharging",
                    style = WearBatteryTypography.telemetryValue,
                    color = if (settings.watchChargingStatus) StatusCharging else DarkTextPrimary
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
                    text = if (settings.watchChargingStatus) "Time to full" else "Time to 0%",
                    style = WearBatteryTypography.secondary,
                    color = DarkTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeRemainingText,
                    style = WearBatteryTypography.telemetryValue,
                    color = DarkTextPrimary
                )
            }

            TelemetryCard(modifier = Modifier.weight(1f)) {
                Text(text = "Last sync", style = WearBatteryTypography.secondary, color = DarkTextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Just now",
                    style = WearBatteryTypography.telemetryValue,
                    color = DarkTextPrimary
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
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkTextPrimary),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = DarkTextPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Refresh", color = DarkTextPrimary, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
