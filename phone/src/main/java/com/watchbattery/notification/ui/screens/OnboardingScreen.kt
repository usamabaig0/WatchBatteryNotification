package com.watchbattery.notification.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watchbattery.notification.ui.theme.*

@Composable
fun OnboardingScreen(onFinished: () -> Unit, accentColor: Color) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(1) }

    val totalSteps = 4

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
    ) {
        // Step Indicator Dots
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            for (step in 1..totalSteps) {
                Surface(
                    shape = RoundedCornerShape(WearBatteryRadius.pill),
                    color = if (step == currentStep) accentColor else DarkBorder,
                    modifier = Modifier
                        .height(6.dp)
                        .width(if (step == currentStep) 24.dp else 8.dp)
                        .padding(horizontal = 2.dp)
                ) {}
            }
        }

        // Center Content based on currentStep (Section 15 of DESIGN.md)
        when (currentStep) {
            1 -> OnboardingPageContent(
                icon = Icons.Default.Watch,
                title = "Watch Battery Notification",
                subtitle = "Know your watch battery\nwithout checking your watch.",
                accentColor = accentColor
            )
            2 -> OnboardingPageContent(
                icon = Icons.Default.NotificationsActive,
                title = "Smart Alerts",
                subtitle = "Know when your watch starts charging, reaches your target, or gets low.",
                accentColor = accentColor
            )
            3 -> OnboardingPageContent(
                icon = Icons.Default.TrendingUp,
                title = "Smart Predictions",
                subtitle = "Learn how your watch charges and discharges over time with piecewise physics.",
                accentColor = accentColor
            )
            4 -> OnboardingPageContent(
                icon = Icons.Default.Security,
                title = "Reliable Background Alerts",
                subtitle = "Allow background battery optimization exemption so alerts are never delayed by Doze mode.",
                accentColor = accentColor,
                actionButton = {
                    Button(
                        onClick = { requestIgnoreBatteryOptimizations(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                        shape = RoundedCornerShape(WearBatteryRadius.button)
                    ) {
                        Text(text = "Allow Background Exemption", color = accentColor)
                    }
                }
            )
        }

        // Bottom Action Button
        Button(
            onClick = {
                if (currentStep < totalSteps) {
                    currentStep++
                } else {
                    onFinished()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape = RoundedCornerShape(WearBatteryRadius.button),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(bottom = 8.dp)
        ) {
            Text(
                text = if (currentStep < totalSteps) "Next" else "Get Started",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    actionButton: (@Composable () -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(WearBatteryRadius.hero),
            color = DarkSurfaceElevated,
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = title,
            style = WearBatteryTypography.screenTitle,
            color = DarkTextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = subtitle,
            style = WearBatteryTypography.body,
            color = DarkTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        if (actionButton != null) {
            Spacer(modifier = Modifier.height(24.dp))
            actionButton()
        }
    }
}

private fun requestIgnoreBatteryOptimizations(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            runCatching { context.startActivity(intent) }
        }
    }
}
