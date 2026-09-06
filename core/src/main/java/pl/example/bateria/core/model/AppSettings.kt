package pl.example.bateria.core.model

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

    // Repetition & Snooze
    val lowBatteryRepeatConfig: RepeatConfig = RepeatConfig.DO_NOT_REPEAT,
    val lowBatteryRepeatInterval: Int = 5,
    val highBatteryRepeatConfig: RepeatConfig = RepeatConfig.DO_NOT_REPEAT,
    val highBatteryRepeatInterval: Int = 5,
    val smartAlarmTimestamp: Long = 0L,

    // Charging Notification Bar
    val chargingNotificationEnabled: Boolean = true,
    val chargingNotificationHiddenByUser: Boolean = false,

    // Appearance & Theming
    val colorTheme: ColorTheme = ColorTheme.SYSTEM,
    val accentColor: AccentColor = AccentColor.PROFESSIONAL_BLUE,

    // Telemetry Cache & Licensing
    val watchBatteryLevel: Int = 82,
    val watchChargingStatus: Boolean = false,
    val watchModel: String = "Galaxy Watch 6",
    val lastSyncTimestamp: Long = 0L,
    val watchAppVersionCode: Int = 256,
    val isWatchUpdateAvailable: Boolean = false,
    val trialStartTime: Long = 0L,
    val isPremiumUnlocked: Boolean = true
)
