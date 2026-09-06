package com.watchbattery.notification.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watchbattery.notification.core.physics.BatterySample
import com.watchbattery.notification.core.physics.PredictiveModelData
import com.watchbattery.notification.ui.components.TelemetryCard
import com.watchbattery.notification.ui.theme.*

@Composable
fun AnalyticsScreen(
    predictiveModel: PredictiveModelData,
    batteryHistory: List<BatterySample>,
    onResetHabits: () -> Unit,
    accentColor: Color
) {
    val scrollState = rememberScrollState()
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = WearBatterySpacing.lg, vertical = WearBatterySpacing.md)
    ) {
        Text(
            text = "Battery Analytics",
            style = WearBatteryTypography.screenTitle,
            color = DarkTextPrimary
        )
        Text(
            text = "Piecewise Lithium-Ion predictive telemetry",
            style = WearBatteryTypography.secondary,
            color = DarkTextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 24h Telemetry Chart Card
        TelemetryCard {
            Text(
                text = "Battery Velocity Curve",
                style = WearBatteryTypography.sectionTitle,
                color = DarkTextPrimary
            )
            Text(
                text = "Charging (Blue) vs Discharging (Teal)",
                style = WearBatteryTypography.secondary,
                color = DarkTextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Canvas Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceElevated)
                    .padding(8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Grid lines (subtle)
                    for (i in 1..3) {
                        val y = h * (i / 4f)
                        drawLine(
                            color = Color(0xFF2B3138),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Draw smooth curve using mock/historical samples
                    val points = if (batteryHistory.size >= 4) {
                        batteryHistory.map { it.batteryLevel.toFloat() }
                    } else {
                        listOf(45f, 50f, 62f, 75f, 82f, 80f, 77f, 74f, 71f, 68f, 65f, 60f)
                    }

                    val path = Path()
                    val stepX = w / (points.size - 1).coerceAtLeast(1)

                    points.forEachIndexed { index, level ->
                        val x = index * stepX
                        val y = h - (level / 100f * h)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }

                    drawPath(
                        path = path,
                        color = accentColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "0h (Start)", fontSize = 11.sp, color = DarkTextSecondary)
                Text(text = "12h", fontSize = 11.sp, color = DarkTextSecondary)
                Text(text = "Now", fontSize = 11.sp, color = DarkTextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Metrics Grid
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TelemetryCard(modifier = Modifier.weight(1f)) {
                Text(text = "Charging speed", style = WearBatteryTypography.secondary, color = DarkTextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "+${predictiveModel.averageChargeVelocityPercentPerMin}%/min",
                    style = WearBatteryTypography.telemetryValue,
                    color = StatusConnected
                )
            }

            TelemetryCard(modifier = Modifier.weight(1f)) {
                Text(text = "Discharge rate", style = WearBatteryTypography.secondary, color = DarkTextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "-${predictiveModel.averageDischargePercentPerHour}%/hour",
                    style = WearBatteryTypography.telemetryValue,
                    color = DarkTextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TelemetryCard(modifier = Modifier.weight(1f)) {
                Text(text = "Sessions analyzed", style = WearBatteryTypography.secondary, color = DarkTextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${predictiveModel.totalSessionsAnalyzed}",
                    style = WearBatteryTypography.telemetryValue,
                    color = DarkTextPrimary
                )
            }

            TelemetryCard(modifier = Modifier.weight(1f)) {
                Text(text = "Confidence", style = WearBatteryTypography.secondary, color = DarkTextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "High (${(predictiveModel.confidenceScore * 100).toInt()}%)",
                    style = WearBatteryTypography.telemetryValue,
                    color = accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Clusters and Anomaly Card
        TelemetryCard {
            Text(
                text = "Discharge Clusters",
                style = WearBatteryTypography.sectionTitle,
                color = DarkTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Daytime active drain", color = DarkTextSecondary, fontSize = 14.sp)
                Text(text = "${predictiveModel.daytimeDischargePerHour}% / hr", color = DarkTextPrimary, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Night sleep drain", color = DarkTextSecondary, fontSize = 14.sp)
                Text(text = "${predictiveModel.nightDischargePerHour}% / hr", color = DarkTextPrimary, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Anomaly budget remaining", color = DarkTextSecondary, fontSize = 14.sp)
                Text(text = "${predictiveModel.anomalyBudgetRemaining} tokens", color = StatusConnecting, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Reset Habits Button
        OutlinedButton(
            onClick = { showResetDialog = true },
            shape = RoundedCornerShape(WearBatteryRadius.button),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCritical),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(StatusCritical.copy(alpha = 0.5f))),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = StatusCritical)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Reset Battery Habits", color = StatusCritical, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(text = "Reset Battery Habits?", color = DarkTextPrimary) },
            text = {
                Text(
                    text = "This flushes all learned ChargeBlock weights, discharge curves, and anomaly buffers. Use this when switching to a new charger or watch band.",
                    color = DarkTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetHabits()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCritical)
                ) {
                    Text(text = "Reset", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(text = "Cancel", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated,
            shape = RoundedCornerShape(WearBatteryRadius.dialog)
        )
    }
}
