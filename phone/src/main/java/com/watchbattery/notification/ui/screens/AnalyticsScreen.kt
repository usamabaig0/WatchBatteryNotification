package com.watchbattery.notification.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.style.TextAlign
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
    val colors = WearBatteryTheme.colors

    val hasData = batteryHistory.size >= 4

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(scrollState)
            .padding(horizontal = WearBatterySpacing.lg, vertical = WearBatterySpacing.md)
    ) {
        Text(
            text = "Battery Analytics",
            style = WearBatteryTypography.screenTitle,
            color = colors.textPrimary
        )
        Text(
            text = "Piecewise Lithium-Ion predictive telemetry",
            style = WearBatteryTypography.secondary,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 24h Telemetry Chart Card
        TelemetryCard {
            Text(
                text = "Battery Velocity Curve",
                style = WearBatteryTypography.sectionTitle,
                color = colors.textPrimary
            )
            Text(
                text = "Charging vs Discharging history over time",
                style = WearBatteryTypography.secondary,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Canvas Chart / Empty State Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceElevated)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (hasData) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Grid lines (subtle)
                        for (i in 1..3) {
                            val y = h * (i / 4f)
                            drawLine(
                                color = colors.border,
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Draw real history samples
                        val points = batteryHistory.map { it.batteryLevel.toFloat() }
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
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = null,
                            tint = colors.textDisabled,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Battery History Yet",
                            style = WearBatteryTypography.sectionTitle,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Connect your smartwatch to record real-time battery curves and habit patterns.",
                            style = WearBatteryTypography.secondary,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (hasData) "Start" else "0h",
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
                Text(
                    text = if (hasData) "Recent trend" else "Awaiting watch data",
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
                Text(
                    text = if (hasData) "Latest" else "Now",
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Metrics Grid
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TelemetryCard(modifier = Modifier.weight(1f)) {
                Text(text = "Charging speed", style = WearBatteryTypography.secondary, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                val chargeSpeedText = if (predictiveModel.totalSessionsAnalyzed > 0 && predictiveModel.averageChargeVelocityPercentPerMin > 0.0) {
                    "+${predictiveModel.averageChargeVelocityPercentPerMin}%/min"
                } else {
                    "--"
                }
                Text(
                    text = chargeSpeedText,
                    style = WearBatteryTypography.telemetryValue,
                    color = if (chargeSpeedText != "--") StatusConnected else colors.textPrimary
                )
            }

            TelemetryCard(modifier = Modifier.weight(1f)) {
                Text(text = "Discharge rate", style = WearBatteryTypography.secondary, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                val dischargeSpeedText = if (predictiveModel.totalSessionsAnalyzed > 0 && predictiveModel.averageDischargePercentPerHour > 0.0) {
                    "-${predictiveModel.averageDischargePercentPerHour}%/hour"
                } else {
                    "--"
                }
                Text(
                    text = dischargeSpeedText,
                    style = WearBatteryTypography.telemetryValue,
                    color = colors.textPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TelemetryCard(modifier = Modifier.weight(1f)) {
                Text(text = "Sessions analyzed", style = WearBatteryTypography.secondary, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${predictiveModel.totalSessionsAnalyzed}",
                    style = WearBatteryTypography.telemetryValue,
                    color = colors.textPrimary
                )
            }

            TelemetryCard(modifier = Modifier.weight(1f)) {
                Text(text = "Confidence", style = WearBatteryTypography.secondary, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                val confidenceText = if (predictiveModel.totalSessionsAnalyzed > 0) {
                    "${(predictiveModel.confidenceScore * 100).toInt()}%"
                } else {
                    "Uncalibrated (0%)"
                }
                Text(
                    text = confidenceText,
                    style = WearBatteryTypography.telemetryValue,
                    color = if (predictiveModel.totalSessionsAnalyzed > 0) accentColor else colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Clusters and Anomaly Card
        TelemetryCard {
            Text(
                text = "Discharge Clusters",
                style = WearBatteryTypography.sectionTitle,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Daytime active drain", color = colors.textSecondary, fontSize = 14.sp)
                val dayText = if (predictiveModel.totalSessionsAnalyzed > 0 && predictiveModel.daytimeDischargePerHour > 0.0) {
                    "${predictiveModel.daytimeDischargePerHour}% / hr"
                } else {
                    "--"
                }
                Text(text = dayText, color = colors.textPrimary, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Night sleep drain", color = colors.textSecondary, fontSize = 14.sp)
                val nightText = if (predictiveModel.totalSessionsAnalyzed > 0 && predictiveModel.nightDischargePerHour > 0.0) {
                    "${predictiveModel.nightDischargePerHour}% / hr"
                } else {
                    "--"
                }
                Text(text = nightText, color = colors.textPrimary, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Anomaly budget remaining", color = colors.textSecondary, fontSize = 14.sp)
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
            title = { Text(text = "Reset Battery Habits?", color = colors.textPrimary) },
            text = {
                Text(
                    text = "This flushes all learned ChargeBlock weights, discharge curves, and anomaly buffers. Use this when switching to a new charger or watch band.",
                    color = colors.textSecondary
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
                    Text(text = "Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surfaceElevated,
            shape = RoundedCornerShape(WearBatteryRadius.dialog)
        )
    }
}
