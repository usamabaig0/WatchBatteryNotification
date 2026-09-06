package com.watchbattery.notification.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watchbattery.notification.ui.theme.*

@Composable
fun EventConfigCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    soundEnabled: Boolean,
    onSoundToggle: (Boolean) -> Unit,
    voiceEnabled: Boolean,
    onVoiceToggle: (Boolean) -> Unit,
    ttsText: String,
    onTtsChange: (String) -> Unit,
    onTestNotification: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = WearBatteryTheme.colors

    Surface(
        shape = RoundedCornerShape(WearBatteryRadius.card),
        color = colors.surface,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WearBatteryRadius.card))
            .border(1.dp, colors.border, RoundedCornerShape(WearBatteryRadius.card))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row (Click to toggle expansion)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = WearBatteryTypography.sectionTitle,
                        color = colors.textPrimary
                    )
                    Text(
                        text = subtitle,
                        style = WearBatteryTypography.secondary,
                        color = colors.textSecondary
                    )
                }

                Switch(
                    checked = soundEnabled || voiceEnabled,
                    onCheckedChange = { checked ->
                        onSoundToggle(checked)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accentColor,
                        uncheckedThumbColor = colors.textDisabled,
                        uncheckedTrackColor = colors.surfaceElevated
                    )
                )

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = colors.textSecondary
                    )
                }
            }

            // Expanded Controls
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Divider(color = colors.border, thickness = 1.dp)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sound Toggle Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Sound Chime", color = colors.textPrimary, fontSize = 14.sp)
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = onSoundToggle,
                            colors = SwitchDefaults.colors(checkedTrackColor = accentColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Voice (TTS) Toggle Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Voice Alert (TTS)", color = colors.textPrimary, fontSize = 14.sp)
                        Switch(
                            checked = voiceEnabled,
                            onCheckedChange = onVoiceToggle,
                            colors = SwitchDefaults.colors(checkedTrackColor = accentColor)
                        )
                    }

                    if (voiceEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = ttsText,
                            onValueChange = onTtsChange,
                            label = { Text("Voice Text (#bat = battery %)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            shape = RoundedCornerShape(WearBatteryRadius.input),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Preview: ${ttsText.replace("#bat", "80")}",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Test notification preview button
                    Button(
                        onClick = onTestNotification,
                        shape = RoundedCornerShape(WearBatteryRadius.button),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = accentColor)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Test Notification Preview", color = colors.textPrimary)
                    }
                }
            }
        }
    }
}
