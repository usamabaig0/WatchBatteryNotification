package com.watchbattery.notification.core.model

import kotlinx.serialization.Serializable

enum class ColorTheme {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AccentColor(val displayName: String, val hex: String) {
    PROFESSIONAL_BLUE("Professional Blue", "#3B82C4"),
    AMBER_GOLD("Amber Gold", "#D49A2A"),
    LAVA_RED("Lava Red", "#C95158"),
    BALANCED_GREEN("Balanced Green", "#3E9B68"),
    REFINED_PURPLE("Refined Purple", "#8064B5")
}

enum class VibrationPattern(val displayName: String, val timings: LongArray) {
    STANDARD("Standard", longArrayOf(0, 400, 200, 400)),
    HEARTBEAT("Heartbeat", longArrayOf(0, 150, 100, 150, 600, 150, 100, 150)),
    ALARM("Alarm", longArrayOf(0, 800, 300, 800, 300, 800))
}

enum class RepeatConfig(val displayName: String) {
    DO_NOT_REPEAT("Do Not Repeat"),
    TIMES_COUNT("Repeat Count"),
    UNTIL_PLUGGED("Until Plugged In"),
    UNTIL_UNPLUGGED("Until Unplugged")
}

object RepeatIntervalPresets {
    val PRESETS_SECONDS = listOf(
        5,      // 5 seconds
        10,     // 10 seconds
        15,     // 15 seconds
        30,     // 30 seconds
        45,     // 45 seconds
        60,     // 1 minute
        120,    // 2 minutes
        300,    // 5 minutes
        600,    // 10 minutes
        900,    // 15 minutes
        1800,   // 30 minutes
        3600,   // 1 hour
        7200,   // 2 hours
        10800,  // 3 hours
        18000,  // 5 hours
        28800,  // 8 hours
        36000   // 10 hours
    )

    fun format(seconds: Int): String {
        return when {
            seconds < 60 -> "$seconds sec"
            seconds < 3600 -> {
                val mins = seconds / 60
                val remSec = seconds % 60
                if (remSec == 0) "$mins min" else "$mins min $remSec sec"
            }
            else -> {
                val hrs = seconds / 3600
                val remMin = (seconds % 3600) / 60
                if (remMin == 0) "$hrs hr" else "$hrs hr $remMin min"
            }
        }
    }

    fun formatInterval(seconds: Int): String = format(seconds)
}

@Serializable
data class AppSettings(
    // Thresholds
    val lowBatteryThreshold: Int = 15,
    val highBatteryThreshold: Int = 80,
    val welcomeScreenShown: Boolean = false,
    val isTrackingEnabled: Boolean = true,
    val dischargingTrackingEnabled: Boolean = true,

    // Audio & Voice - Charging Started
    val phoneStartSound: Boolean = true,
    val phoneStartVoice: Boolean = false,
    val customSoundUriStart: String? = null,
    val customTtsTextStart: String = "Watch charging started (#bat%)",

    // Audio & Voice - Charging Stopped
    val phoneStopSound: Boolean = true,
    val phoneStopVoice: Boolean = false,
    val customSoundUriStop: String? = null,
    val customTtsTextStop: String = "Watch disconnected from charger at #bat%",

    // Audio & Voice - Fully Charged / High Threshold
    val phoneFullSound: Boolean = true,
    val phoneFullVoice: Boolean = false,
    val customSoundUriFull: String? = null,
    val customTtsTextFull: String = "Watch charged to #bat percent",

    // Audio & Voice - Low Battery
    val phoneLowSound: Boolean = true,
    val phoneLowVoice: Boolean = false,
    val customSoundUriLow: String? = null,
    val customTtsTextLow: String = "Low watch battery: #bat percent",

    // Sound Behavior & Loudness Knobs
    val notificationVolume: Int = 80,
    val volumeBypassDnd: Boolean = false,
    val volumeForcePhoneSpeaker: Boolean = false,
    val smartVolumeEnabled: Boolean = true,

    // Vibrations & Patterns
    val vibrationEnabled: Boolean = true,
    val vibrationPattern: VibrationPattern = VibrationPattern.STANDARD,

    // Repetition & Snooze (Intervals in SECONDS from 5 seconds to 36000 seconds / 10 hours)
    val lowBatteryRepeatConfig: RepeatConfig = RepeatConfig.DO_NOT_REPEAT,
    val lowBatteryRepeatIntervalSeconds: Int = 300, // 5 min default
    val highBatteryRepeatConfig: RepeatConfig = RepeatConfig.DO_NOT_REPEAT,
    val highBatteryRepeatIntervalSeconds: Int = 300, // 5 min default
    val smartAlarmTimestamp: Long = 0L,

    // Charging Notification Bar
    val chargingNotificationEnabled: Boolean = true,
    val chargingNotificationHiddenByUser: Boolean = false,

    // Appearance & Theming
    val colorTheme: ColorTheme = ColorTheme.SYSTEM,
    val accentColor: AccentColor = AccentColor.PROFESSIONAL_BLUE,

    // Telemetry Cache & Licensing State (Unset by default until a watch connects)
    val watchBatteryLevel: Int = -1, // -1 means no reading yet
    val watchChargingStatus: Boolean = false,
    val watchModel: String = "", // Empty means no watch connected yet
    val lastSyncTimestamp: Long = 0L, // 0 means never synced
    val watchAppVersionCode: Int = 0,
    val isWatchUpdateAvailable: Boolean = false,
    val trialStartTime: Long = 0L,
    val isPremiumUnlocked: Boolean = true
) {
    val isWatchConnected: Boolean
        get() = watchBatteryLevel >= 0 && watchModel.isNotBlank()
}
