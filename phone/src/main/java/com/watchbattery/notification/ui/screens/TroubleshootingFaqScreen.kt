package com.watchbattery.notification.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watchbattery.notification.ui.components.TelemetryCard
import com.watchbattery.notification.ui.theme.*

@Composable
fun TroubleshootingFaqScreen(watchModel: String, accentColor: Color) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val colors = WearBatteryTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(scrollState)
            .padding(horizontal = WearBatterySpacing.lg, vertical = WearBatterySpacing.md)
    ) {
        Text(
            text = "Help & Troubleshooting",
            style = WearBatteryTypography.screenTitle,
            color = colors.textPrimary
        )
        Text(
            text = "Reliability guides, FAQ & Developer support",
            style = WearBatteryTypography.secondary,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // FAQ Accordion 1: Samsung Sleeping Apps
        FaqAccordion(
            title = "Missing notifications? (Samsung fix)",
            preview = "Exclude app from Samsung sleeping apps list",
            details = """
                Samsung One UI aggressively sleeps background companion apps. To ensure real-time alerts:
                1. Open phone Settings → Battery (or Device Care).
                2. Tap 'Background usage limits'.
                3. Tap 'Never auto-sleeping apps' (or Never sleeping apps).
                4. Tap the '+' icon in the top right.
                5. Select 'Watch Battery Notification' and tap Add.
            """.trimIndent(),
            accentColor = accentColor
        )

        Spacer(modifier = Modifier.height(10.dp))

        // FAQ Accordion 2: Battery Optimization
        FaqAccordion(
            title = "Ignore Battery Optimization (Doze)",
            preview = "Prevents Android from delaying alarms while idle",
            details = """
                Android Doze mode puts idle apps to sleep. By granting battery optimization exemption, your phone will reliably chime the moment your watch finishes charging or drops low.
            """.trimIndent(),
            actionButtonText = "Open Battery Optimization Settings",
            onAction = {
                try {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Please open Settings manually", Toast.LENGTH_SHORT).show()
                }
            },
            accentColor = accentColor
        )

        Spacer(modifier = Modifier.height(10.dp))

        // FAQ Accordion 3: How to connect watch
        FaqAccordion(
            title = "How to connect my watch?",
            preview = "Setup instructions for Wear OS watches",
            details = """
                1. Ensure Watch Battery Notification is installed on both your phone and your Wear OS watch.
                2. Open the app on your watch once to grant permissions.
                3. Ensure Bluetooth is turned on and your watch is connected in the Galaxy Wearable / Pixel Watch app.
                4. The phone app will automatically show a green 'Connected' badge!
            """.trimIndent(),
            accentColor = accentColor
        )

        Spacer(modifier = Modifier.height(10.dp))

        // FAQ Accordion 4: Inaccurate charts
        FaqAccordion(
            title = "Why are battery charts inaccurate at first?",
            preview = "Piecewise predictive learning requirements",
            details = """
                The app uses a 20-block Lithium-Ion physics engine rather than simple linear math. It requires 2 to 3 complete charge/discharge cycles to accurately learn your watch battery's CC-CV curves and habit profiles.
            """.trimIndent(),
            accentColor = accentColor
        )

        Spacer(modifier = Modifier.height(10.dp))

        // FAQ Accordion 5: Text to Speech
        FaqAccordion(
            title = "Why does Text-to-Speech sound robotic?",
            preview = "Configure Google Speech Services on phone",
            details = """
                The voice synthesizer uses your phone's default system TTS engine. To switch to a high-fidelity natural voice:
                1. Open phone Settings → Accessibility → Text-to-speech output.
                2. Tap Preferred Engine → Google Speech Services.
                3. Tap Settings icon → Install voice data to download HD voices.
            """.trimIndent(),
            accentColor = accentColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Contact Developer Section
        TelemetryCard {
            Text(
                text = "Contact Developer",
                style = WearBatteryTypography.sectionTitle,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Have feedback, found a bug, or have a new watch model to report?",
                color = colors.textSecondary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:watchbatterymonitor@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Watch Battery Notification Feedback ($watchModel)")
                    }
                    runCatching { context.startActivity(emailIntent) }.onFailure {
                        Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(WearBatteryRadius.button),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Email Developer", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tip Jar Section
        TelemetryCard {
            Text(
                text = "Developer Tip Jar",
                style = WearBatteryTypography.sectionTitle,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Enjoying clean battery alerts? Support independent development:",
                color = colors.textSecondary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("☕ Coffee ($2)", "🍺 Beer ($5)", "🍕 Pizza ($10)").forEach { item ->
                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Thank you for supporting development!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(WearBatteryRadius.button),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(colors.border)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = item, fontSize = 11.sp, color = colors.textPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun FaqAccordion(
    title: String,
    preview: String,
    details: String,
    actionButtonText: String? = null,
    onAction: (() -> Unit)? = null,
    accentColor: Color
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = WearBatteryTheme.colors

    Surface(
        shape = RoundedCornerShape(WearBatteryRadius.card),
        color = colors.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WearBatteryRadius.card))
            .border(1.dp, colors.border, RoundedCornerShape(WearBatteryRadius.card))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = WearBatteryTypography.sectionTitle, color = colors.textPrimary)
                    Text(text = preview, style = WearBatteryTypography.secondary, color = colors.textSecondary)
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = colors.textSecondary
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider(color = colors.border, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = details, color = colors.textSecondary, fontSize = 13.sp, lineHeight = 19.sp)
                    if (actionButtonText != null && onAction != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onAction,
                            shape = RoundedCornerShape(WearBatteryRadius.button),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated)
                        ) {
                            Text(text = actionButtonText, color = accentColor, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
