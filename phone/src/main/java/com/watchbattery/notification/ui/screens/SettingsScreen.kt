package com.watchbattery.notification.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.core.content.ContextCompat
import com.watchbattery.notification.core.constants.WearConstants
import com.watchbattery.notification.core.model.*
import com.watchbattery.notification.service.NotificationPlayerService
import com.watchbattery.notification.ui.components.EventConfigCard
import com.watchbattery.notification.ui.components.TelemetryCard
import com.watchbattery.notification.ui.theme.*

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    accentColor: Color
) {
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
            text = "Settings",
            style = WearBatteryTypography.screenTitle,
            color = colors.textPrimary
        )
        Text(
            text = "Alerts, audio loudness, repetition & styling",
            style = WearBatteryTypography.secondary,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 1: Battery Alerts & Thresholds
        Text(text = "BATTERY THRESHOLDS", style = WearBatteryTypography.label, color = accentColor)
        Spacer(modifier = Modifier.height(8.dp))

        TelemetryCard {
            // High Battery Threshold Slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "High charge alert", color = colors.textPrimary, fontSize = 15.sp)
                Surface(
                    shape = RoundedCornerShape(WearBatteryRadius.pill),
                    color = colors.surfaceElevated
                ) {
                    Text(
                        text = "${settings.highBatteryThreshold}%",
                        color = StatusConnected,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Slider(
                value = settings.highBatteryThreshold.toFloat(),
                onValueChange = { onUpdateSettings { s -> s.copy(highBatteryThreshold = it.toInt()) } },
                valueRange = 50f..100f,
                steps = 50,
                colors = SliderDefaults.colors(
                    thumbColor = StatusConnected,
                    activeTrackColor = StatusConnected,
                    inactiveTrackColor = colors.border
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Low Battery Threshold Slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Low battery alert", color = colors.textPrimary, fontSize = 15.sp)
                Surface(
                    shape = RoundedCornerShape(WearBatteryRadius.pill),
                    color = colors.surfaceElevated
                ) {
                    Text(
                        text = "${settings.lowBatteryThreshold}%",
                        color = StatusCritical,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Slider(
                value = settings.lowBatteryThreshold.toFloat(),
                onValueChange = { onUpdateSettings { s -> s.copy(lowBatteryThreshold = it.toInt()) } },
                valueRange = 5f..50f,
                steps = 45,
                colors = SliderDefaults.colors(
                    thumbColor = StatusCritical,
                    activeTrackColor = StatusCritical,
                    inactiveTrackColor = colors.border
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 2: 4 Battery Event Notification Cards
        Text(text = "EVENT NOTIFICATIONS", style = WearBatteryTypography.label, color = accentColor)
        Spacer(modifier = Modifier.height(8.dp))

        // Event 1: Charging Started
        EventConfigCard(
            title = "Charging Started",
            subtitle = "When watch docks onto magnetic puck",
            icon = Icons.Default.Bolt,
            soundEnabled = settings.phoneStartSound,
            onSoundToggle = { onUpdateSettings { s -> s.copy(phoneStartSound = it) } },
            voiceEnabled = settings.phoneStartVoice,
            onVoiceToggle = { onUpdateSettings { s -> s.copy(phoneStartVoice = it) } },
            ttsText = settings.customTtsTextStart,
            onTtsChange = { onUpdateSettings { s -> s.copy(customTtsTextStart = it) } },
            onTestNotification = { testAlert(context, WearConstants.EVENT_CHARGING_STARTED, settings.watchModel) },
            accentColor = accentColor
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Event 2: Charging Stopped / Slippage
        EventConfigCard(
            title = "Charging Stopped",
            subtitle = "When disconnected or slips off dock",
            icon = Icons.Default.PowerOff,
            soundEnabled = settings.phoneStopSound,
            onSoundToggle = { onUpdateSettings { s -> s.copy(phoneStopSound = it) } },
            voiceEnabled = settings.phoneStopVoice,
            onVoiceToggle = { onUpdateSettings { s -> s.copy(phoneStopVoice = it) } },
            ttsText = settings.customTtsTextStop,
            onTtsChange = { onUpdateSettings { s -> s.copy(customTtsTextStop = it) } },
            onTestNotification = { testAlert(context, WearConstants.EVENT_CHARGING_STOPPED, settings.watchModel) },
            accentColor = accentColor
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Event 3: Fully Charged / High Threshold
        EventConfigCard(
            title = "Charge Target Reached",
            subtitle = "When battery reaches ${settings.highBatteryThreshold}%",
            icon = Icons.Default.BatteryChargingFull,
            soundEnabled = settings.phoneFullSound,
            onSoundToggle = { onUpdateSettings { s -> s.copy(phoneFullSound = it) } },
            voiceEnabled = settings.phoneFullVoice,
            onVoiceToggle = { onUpdateSettings { s -> s.copy(phoneFullVoice = it) } },
            ttsText = settings.customTtsTextFull,
            onTtsChange = { onUpdateSettings { s -> s.copy(customTtsTextFull = it) } },
            onTestNotification = { testAlert(context, WearConstants.EVENT_FULLY_CHARGED, settings.watchModel) },
            accentColor = accentColor
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Event 4: Low Battery
        EventConfigCard(
            title = "Low Battery Alert",
            subtitle = "When battery drops below ${settings.lowBatteryThreshold}%",
            icon = Icons.Default.BatteryAlert,
            soundEnabled = settings.phoneLowSound,
            onSoundToggle = { onUpdateSettings { s -> s.copy(phoneLowSound = it) } },
            voiceEnabled = settings.phoneLowVoice,
            onVoiceToggle = { onUpdateSettings { s -> s.copy(phoneLowVoice = it) } },
            ttsText = settings.customTtsTextLow,
            onTtsChange = { onUpdateSettings { s -> s.copy(customTtsTextLow = it) } },
            onTestNotification = { testAlert(context, WearConstants.EVENT_LOW_BATTERY, settings.watchModel) },
            accentColor = accentColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 3: Audio Behavior & Loudness Knobs
        Text(text = "AUDIO BEHAVIOR & LOUDNESS", style = WearBatteryTypography.label, color = accentColor)
        Spacer(modifier = Modifier.height(8.dp))

        TelemetryCard {
            // Master volume slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Alert Volume", color = colors.textPrimary, fontSize = 15.sp)
                Text(text = "${settings.notificationVolume}%", color = accentColor, fontSize = 14.sp)
            }
            Slider(
                value = settings.notificationVolume.toFloat(),
                onValueChange = { onUpdateSettings { s -> s.copy(notificationVolume = it.toInt()) } },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = colors.border
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // DND Bypass Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Bypass Do Not Disturb (DND)", color = colors.textPrimary, fontSize = 15.sp)
                    Text(text = "Plays as an alarm even if silent", color = colors.textSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = settings.volumeBypassDnd,
                    onCheckedChange = { onUpdateSettings { s -> s.copy(volumeBypassDnd = it) } },
                    colors = SwitchDefaults.colors(checkedTrackColor = StatusCritical)
                )
            }

            if (settings.volumeBypassDnd) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StatusCritical.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Warning: Bypassing DND ensures alerts sound when asleep. Keep volume comfortable.",
                        color = StatusCritical,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Force Phone Speaker
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Force Phone Speaker", color = colors.textPrimary, fontSize = 15.sp)
                    Text(text = "Audible chime even with headphones connected", color = colors.textSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = settings.volumeForcePhoneSpeaker,
                    onCheckedChange = { onUpdateSettings { s -> s.copy(volumeForcePhoneSpeaker = it) } },
                    colors = SwitchDefaults.colors(checkedTrackColor = accentColor)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Smart Volume (Quiet when screen on)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Smart Screen-Aware Volume", color = colors.textPrimary, fontSize = 15.sp)
                    Text(text = "Gentle chime if holding phone, loud if locked", color = colors.textSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = settings.smartVolumeEnabled,
                    onCheckedChange = { onUpdateSettings { s -> s.copy(smartVolumeEnabled = it) } },
                    colors = SwitchDefaults.colors(checkedTrackColor = accentColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 4: Vibration & Haptics
        Text(text = "VIBRATION & HAPTICS", style = WearBatteryTypography.label, color = accentColor)
        Spacer(modifier = Modifier.height(8.dp))

        TelemetryCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Enable Vibrations", color = colors.textPrimary, fontSize = 15.sp)
                Switch(
                    checked = settings.vibrationEnabled,
                    onCheckedChange = { onUpdateSettings { s -> s.copy(vibrationEnabled = it) } },
                    colors = SwitchDefaults.colors(checkedTrackColor = accentColor)
                )
            }

            if (settings.vibrationEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Pattern:", color = colors.textSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))

                VibrationPattern.values().forEach { pattern ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUpdateSettings { s -> s.copy(vibrationPattern = pattern) } }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = settings.vibrationPattern == pattern,
                            onClick = { onUpdateSettings { s -> s.copy(vibrationPattern = pattern) } },
                            colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = pattern.displayName, color = colors.textPrimary, fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 5: Repetition, Snooze & Reminders
        Text(text = "REPETITION & SNOOZE", style = WearBatteryTypography.label, color = accentColor)
        Spacer(modifier = Modifier.height(8.dp))

        TelemetryCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Repeat Low Battery Alert", color = colors.textPrimary, fontSize = 15.sp)
                    Text(
                        text = if (settings.lowBatteryRepeatConfig != RepeatConfig.DO_NOT_REPEAT) {
                            "Repeats every ${RepeatIntervalPresets.formatInterval(settings.lowBatteryRepeatIntervalSeconds)} until docked"
                        } else {
                            "Do not repeat"
                        },
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = settings.lowBatteryRepeatConfig != RepeatConfig.DO_NOT_REPEAT,
                    onCheckedChange = { active ->
                        onUpdateSettings { s ->
                            s.copy(lowBatteryRepeatConfig = if (active) RepeatConfig.UNTIL_PLUGGED else RepeatConfig.DO_NOT_REPEAT)
                        }
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = accentColor)
                )
            }

            if (settings.lowBatteryRepeatConfig != RepeatConfig.DO_NOT_REPEAT) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Repeat Interval (5 sec to 10 hours):",
                    color = colors.textSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                RepeatIntervalSelector(
                    selectedSeconds = settings.lowBatteryRepeatIntervalSeconds,
                    onSelect = { sec -> onUpdateSettings { s -> s.copy(lowBatteryRepeatIntervalSeconds = sec) } },
                    accentColor = accentColor
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Repeat Charge Target Alert", color = colors.textPrimary, fontSize = 15.sp)
                    Text(
                        text = if (settings.highBatteryRepeatConfig != RepeatConfig.DO_NOT_REPEAT) {
                            "Repeats every ${RepeatIntervalPresets.formatInterval(settings.highBatteryRepeatIntervalSeconds)} until unplugged"
                        } else {
                            "Do not repeat"
                        },
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = settings.highBatteryRepeatConfig != RepeatConfig.DO_NOT_REPEAT,
                    onCheckedChange = { active ->
                        onUpdateSettings { s ->
                            s.copy(highBatteryRepeatConfig = if (active) RepeatConfig.UNTIL_UNPLUGGED else RepeatConfig.DO_NOT_REPEAT)
                        }
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = accentColor)
                )
            }

            if (settings.highBatteryRepeatConfig != RepeatConfig.DO_NOT_REPEAT) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Repeat Interval (5 sec to 10 hours):",
                    color = colors.textSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                RepeatIntervalSelector(
                    selectedSeconds = settings.highBatteryRepeatIntervalSeconds,
                    onSelect = { sec -> onUpdateSettings { s -> s.copy(highBatteryRepeatIntervalSeconds = sec) } },
                    accentColor = accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 6: Charging Notification Bar
        Text(text = "CHARGING MONITOR IN NOTIFICATION SHADE", style = WearBatteryTypography.label, color = accentColor)
        Spacer(modifier = Modifier.height(8.dp))

        TelemetryCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Persistent Charging Bar", color = colors.textPrimary, fontSize = 15.sp)
                    Text(text = "Live battery progress bar in notification drawer", color = colors.textSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = settings.chargingNotificationEnabled,
                    onCheckedChange = { onUpdateSettings { s -> s.copy(chargingNotificationEnabled = it) } },
                    colors = SwitchDefaults.colors(checkedTrackColor = accentColor)
                )
            }

            if (settings.chargingNotificationHiddenByUser) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        onUpdateSettings { s -> s.copy(chargingNotificationHiddenByUser = false) }
                        Toast.makeText(context, "Restored charging notification bar", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(WearBatteryRadius.button),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Restore hidden charging bar", color = accentColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 7: Appearance & Accent Colors
        Text(text = "APPEARANCE & ACCENT THEMES", style = WearBatteryTypography.label, color = accentColor)
        Spacer(modifier = Modifier.height(8.dp))

        TelemetryCard {
            Text(text = "Color Theme Mode", color = colors.textPrimary, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ColorTheme.values().forEach { theme ->
                    FilterChip(
                        selected = settings.colorTheme == theme,
                        onClick = { onUpdateSettings { s -> s.copy(colorTheme = theme) } },
                        label = { Text(text = theme.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Accent Color (5 Themes)", color = colors.textPrimary, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.fillMaxWidth()
            ) {
                AccentColor.values().forEach { accent ->
                    val isSelected = settings.accentColor == accent
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(accent.hex)))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) colors.textPrimary else colors.border,
                                shape = CircleShape
                            )
                            .clickable { onUpdateSettings { s -> s.copy(accentColor = accent) } }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun RepeatIntervalSelector(
    selectedSeconds: Int,
    onSelect: (Int) -> Unit,
    accentColor: Color
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        RepeatIntervalPresets.PRESETS_SECONDS.forEach { seconds ->
            val isSelected = selectedSeconds == seconds
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(seconds) },
                label = { Text(text = RepeatIntervalPresets.formatInterval(seconds), fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accentColor,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

private fun testAlert(context: Context, eventType: String, watchModel: String) {
    val intent = Intent(context, NotificationPlayerService::class.java).apply {
        putExtra(WearConstants.EXTRA_EVENT_TYPE, eventType)
        putExtra(WearConstants.EXTRA_BATTERY_LEVEL, 80)
        putExtra(WearConstants.EXTRA_WATCH_MODEL, watchModel.ifBlank { "Smartwatch" })
    }
    ContextCompat.startForegroundService(context, intent)
}
