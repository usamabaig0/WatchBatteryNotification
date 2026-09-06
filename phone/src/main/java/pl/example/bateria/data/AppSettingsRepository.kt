package pl.example.bateria.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pl.example.bateria.core.model.*
import pl.example.bateria.core.physics.BatterySample
import pl.example.bateria.core.physics.PredictiveModelData

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "watch_battery_settings")

class AppSettingsRepository(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        val KEY_LOW_THRESHOLD = intPreferencesKey("lowBatteryThreshold")
        val KEY_HIGH_THRESHOLD = intPreferencesKey("highBatteryThreshold")
        val KEY_WELCOME_SHOWN = booleanPreferencesKey("welcomeScreenShown")
        val KEY_TRACKING_ENABLED = booleanPreferencesKey("isTrackingEnabled")
        val KEY_DISCHARGE_TRACKING = booleanPreferencesKey("dischargingTrackingEnabled")

        // Start
        val KEY_START_SOUND = booleanPreferencesKey("phoneStartSound")
        val KEY_START_VOICE = booleanPreferencesKey("phoneStartVoice")
        val KEY_START_URI = stringPreferencesKey("customSoundUriStart")
        val KEY_START_TTS = stringPreferencesKey("customTtsTextStart")

        // Stop
        val KEY_STOP_SOUND = booleanPreferencesKey("phoneStopSound")
        val KEY_STOP_VOICE = booleanPreferencesKey("phoneStopVoice")
        val KEY_STOP_URI = stringPreferencesKey("customSoundUriStop")
        val KEY_STOP_TTS = stringPreferencesKey("customTtsTextStop")

        // Full
        val KEY_FULL_SOUND = booleanPreferencesKey("phoneFullSound")
        val KEY_FULL_VOICE = booleanPreferencesKey("phoneFullVoice")
        val KEY_FULL_URI = stringPreferencesKey("customSoundUriFull")
        val KEY_FULL_TTS = stringPreferencesKey("customTtsTextFull")

        // Low
        val KEY_LOW_SOUND = booleanPreferencesKey("phoneLowSound")
        val KEY_LOW_VOICE = booleanPreferencesKey("phoneLowVoice")
        val KEY_LOW_URI = stringPreferencesKey("customSoundUriLow")
        val KEY_LOW_TTS = stringPreferencesKey("customTtsTextLow")

        // Loudness
        val KEY_VOLUME = intPreferencesKey("notificationVolume")
        val KEY_BYPASS_DND = booleanPreferencesKey("volumeBypassDnd")
        val KEY_FORCE_SPEAKER = booleanPreferencesKey("volumeForcePhoneSpeaker")
        val KEY_SMART_VOLUME = booleanPreferencesKey("smartVolumeEnabled")

        // Vibration
        val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibrationEnabled")
        val KEY_VIBRATION_PATTERN = stringPreferencesKey("vibrationPattern")

        // Repeat
        val KEY_LOW_REPEAT_CONFIG = stringPreferencesKey("lowBatteryRepeatConfig")
        val KEY_LOW_REPEAT_INTERVAL = intPreferencesKey("lowBatteryRepeatInterval")
        val KEY_HIGH_REPEAT_CONFIG = stringPreferencesKey("highBatteryRepeatConfig")
        val KEY_HIGH_REPEAT_INTERVAL = intPreferencesKey("highBatteryRepeatInterval")
        val KEY_SMART_ALARM_TIMESTAMP = longPreferencesKey("smartAlarmTimestamp")

        // Charging Notification Bar
        val KEY_CHARGING_BAR_ENABLED = booleanPreferencesKey("chargingNotificationEnabled")
        val KEY_CHARGING_BAR_HIDDEN = booleanPreferencesKey("chargingNotificationHiddenByUser")

        // Appearance
        val KEY_THEME = stringPreferencesKey("colorTheme")
        val KEY_ACCENT = stringPreferencesKey("accentColor")

        // Telemetry Cache
        val KEY_BATTERY_LEVEL = intPreferencesKey("watchBatteryLevel")
        val KEY_CHARGING_STATUS = booleanPreferencesKey("watchChargingStatus")
        val KEY_WATCH_MODEL = stringPreferencesKey("watchModel")
        val KEY_LAST_SYNC = longPreferencesKey("lastSyncTimestamp")
        val KEY_VERSION_CODE = intPreferencesKey("watchAppVersionCode")
        val KEY_UPDATE_AVAILABLE = booleanPreferencesKey("isWatchUpdateAvailable")
        val KEY_TRIAL_START = longPreferencesKey("trialStartTime")
        val KEY_PREMIUM = booleanPreferencesKey("isPremiumUnlocked")

        // Serialized Models
        val KEY_PREDICTIVE_DATA = stringPreferencesKey("predictiveModelData")
        val KEY_BATTERY_HISTORY = stringPreferencesKey("batteryHistoryJson")
    }

    val settingsFlow: StateFlow<AppSettings> = context.dataStore.data
        .map { prefs ->
            AppSettings(
                lowBatteryThreshold = prefs[KEY_LOW_THRESHOLD] ?: 15,
                highBatteryThreshold = prefs[KEY_HIGH_THRESHOLD] ?: 80,
                welcomeScreenShown = prefs[KEY_WELCOME_SHOWN] ?: false,
                isTrackingEnabled = prefs[KEY_TRACKING_ENABLED] ?: true,
                dischargingTrackingEnabled = prefs[KEY_DISCHARGE_TRACKING] ?: true,

                phoneStartSound = prefs[KEY_START_SOUND] ?: true,
                phoneStartVoice = prefs[KEY_START_VOICE] ?: false,
                customSoundUriStart = prefs[KEY_START_URI],
                customTtsTextStart = prefs[KEY_START_TTS] ?: "Watch charging started (#bat%)",

                phoneStopSound = prefs[KEY_STOP_SOUND] ?: true,
                phoneStopVoice = prefs[KEY_STOP_VOICE] ?: false,
                customSoundUriStop = prefs[KEY_STOP_URI],
                customTtsTextStop = prefs[KEY_STOP_TTS] ?: "Watch disconnected from charger at #bat%",

                phoneFullSound = prefs[KEY_FULL_SOUND] ?: true,
                phoneFullVoice = prefs[KEY_FULL_VOICE] ?: false,
                customSoundUriFull = prefs[KEY_FULL_URI],
                customTtsTextFull = prefs[KEY_FULL_TTS] ?: "Watch charged to #bat percent",

                phoneLowSound = prefs[KEY_LOW_SOUND] ?: true,
                phoneLowVoice = prefs[KEY_LOW_VOICE] ?: false,
                customSoundUriLow = prefs[KEY_LOW_URI],
                customTtsTextLow = prefs[KEY_LOW_TTS] ?: "Low watch battery: #bat percent",

                notificationVolume = prefs[KEY_VOLUME] ?: 80,
                volumeBypassDnd = prefs[KEY_BYPASS_DND] ?: false,
                volumeForcePhoneSpeaker = prefs[KEY_FORCE_SPEAKER] ?: false,
                smartVolumeEnabled = prefs[KEY_SMART_VOLUME] ?: true,

                vibrationEnabled = prefs[KEY_VIBRATION_ENABLED] ?: true,
                vibrationPattern = prefs[KEY_VIBRATION_PATTERN]?.let { runCatching { VibrationPattern.valueOf(it) }.getOrNull() } ?: VibrationPattern.STANDARD,

                lowBatteryRepeatConfig = prefs[KEY_LOW_REPEAT_CONFIG]?.let { runCatching { RepeatConfig.valueOf(it) }.getOrNull() } ?: RepeatConfig.DO_NOT_REPEAT,
                lowBatteryRepeatInterval = prefs[KEY_LOW_REPEAT_INTERVAL] ?: 5,
                highBatteryRepeatConfig = prefs[KEY_HIGH_REPEAT_CONFIG]?.let { runCatching { RepeatConfig.valueOf(it) }.getOrNull() } ?: RepeatConfig.DO_NOT_REPEAT,
                highBatteryRepeatInterval = prefs[KEY_HIGH_REPEAT_INTERVAL] ?: 5,
                smartAlarmTimestamp = prefs[KEY_SMART_ALARM_TIMESTAMP] ?: 0L,

                chargingNotificationEnabled = prefs[KEY_CHARGING_BAR_ENABLED] ?: true,
                chargingNotificationHiddenByUser = prefs[KEY_CHARGING_BAR_HIDDEN] ?: false,

                colorTheme = prefs[KEY_THEME]?.let { runCatching { ColorTheme.valueOf(it) }.getOrNull() } ?: ColorTheme.SYSTEM,
                accentColor = prefs[KEY_ACCENT]?.let { runCatching { AccentColor.valueOf(it) }.getOrNull() } ?: AccentColor.PROFESSIONAL_BLUE,

                watchBatteryLevel = prefs[KEY_BATTERY_LEVEL] ?: 82,
                watchChargingStatus = prefs[KEY_CHARGING_STATUS] ?: false,
                watchModel = prefs[KEY_WATCH_MODEL] ?: "Galaxy Watch 6",
                lastSyncTimestamp = prefs[KEY_LAST_SYNC] ?: System.currentTimeMillis(),
                watchAppVersionCode = prefs[KEY_VERSION_CODE] ?: 256,
                isWatchUpdateAvailable = prefs[KEY_UPDATE_AVAILABLE] ?: false,
                trialStartTime = prefs[KEY_TRIAL_START] ?: System.currentTimeMillis(),
                isPremiumUnlocked = prefs[KEY_PREMIUM] ?: true
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, AppSettings())

    val predictiveModelFlow: Flow<PredictiveModelData> = context.dataStore.data
        .map { prefs ->
            val jsonStr = prefs[KEY_PREDICTIVE_DATA]
            if (jsonStr != null) {
                runCatching { json.decodeFromString<PredictiveModelData>(jsonStr) }.getOrDefault(PredictiveModelData())
            } else {
                PredictiveModelData()
            }
        }

    val batteryHistoryFlow: Flow<List<BatterySample>> = context.dataStore.data
        .map { prefs ->
            val jsonStr = prefs[KEY_BATTERY_HISTORY]
            if (jsonStr != null) {
                runCatching { json.decodeFromString<List<BatterySample>>(jsonStr) }.getOrDefault(emptyList())
            } else {
                emptyList()
            }
        }

    private val _connectionStatusFlow = MutableStateFlow(ConnectionStatus.CONNECTED)
    val connectionStatusFlow: StateFlow<ConnectionStatus> = _connectionStatusFlow.asStateFlow()

    fun setConnectionStatus(status: ConnectionStatus) {
        _connectionStatusFlow.value = status
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val current = settingsFlow.value
        val updated = transform(current)
        context.dataStore.edit { prefs ->
            prefs[KEY_LOW_THRESHOLD] = updated.lowBatteryThreshold
            prefs[KEY_HIGH_THRESHOLD] = updated.highBatteryThreshold
            prefs[KEY_WELCOME_SHOWN] = updated.welcomeScreenShown
            prefs[KEY_TRACKING_ENABLED] = updated.isTrackingEnabled
            prefs[KEY_DISCHARGE_TRACKING] = updated.dischargingTrackingEnabled

            prefs[KEY_START_SOUND] = updated.phoneStartSound
            prefs[KEY_START_VOICE] = updated.phoneStartVoice
            updated.customSoundUriStart?.let { prefs[KEY_START_URI] = it } ?: prefs.remove(KEY_START_URI)
            prefs[KEY_START_TTS] = updated.customTtsTextStart

            prefs[KEY_STOP_SOUND] = updated.phoneStopSound
            prefs[KEY_STOP_VOICE] = updated.phoneStopVoice
            updated.customSoundUriStop?.let { prefs[KEY_STOP_URI] = it } ?: prefs.remove(KEY_STOP_URI)
            prefs[KEY_STOP_TTS] = updated.customTtsTextStop

            prefs[KEY_FULL_SOUND] = updated.phoneFullSound
            prefs[KEY_FULL_VOICE] = updated.phoneFullVoice
            updated.customSoundUriFull?.let { prefs[KEY_FULL_URI] = it } ?: prefs.remove(KEY_FULL_URI)
            prefs[KEY_FULL_TTS] = updated.customTtsTextFull

            prefs[KEY_LOW_SOUND] = updated.phoneLowSound
            prefs[KEY_LOW_VOICE] = updated.phoneLowVoice
            updated.customSoundUriLow?.let { prefs[KEY_LOW_URI] = it } ?: prefs.remove(KEY_LOW_URI)
            prefs[KEY_LOW_TTS] = updated.customTtsTextLow

            prefs[KEY_VOLUME] = updated.notificationVolume
            prefs[KEY_BYPASS_DND] = updated.volumeBypassDnd
            prefs[KEY_FORCE_SPEAKER] = updated.volumeForcePhoneSpeaker
            prefs[KEY_SMART_VOLUME] = updated.smartVolumeEnabled

            prefs[KEY_VIBRATION_ENABLED] = updated.vibrationEnabled
            prefs[KEY_VIBRATION_PATTERN] = updated.vibrationPattern.name

            prefs[KEY_LOW_REPEAT_CONFIG] = updated.lowBatteryRepeatConfig.name
            prefs[KEY_LOW_REPEAT_INTERVAL] = updated.lowBatteryRepeatInterval
            prefs[KEY_HIGH_REPEAT_CONFIG] = updated.highBatteryRepeatConfig.name
            prefs[KEY_HIGH_REPEAT_INTERVAL] = updated.highBatteryRepeatInterval
            prefs[KEY_SMART_ALARM_TIMESTAMP] = updated.smartAlarmTimestamp

            prefs[KEY_CHARGING_BAR_ENABLED] = updated.chargingNotificationEnabled
            prefs[KEY_CHARGING_BAR_HIDDEN] = updated.chargingNotificationHiddenByUser

            prefs[KEY_THEME] = updated.colorTheme.name
            prefs[KEY_ACCENT] = updated.accentColor.name

            prefs[KEY_BATTERY_LEVEL] = updated.watchBatteryLevel
            prefs[KEY_CHARGING_STATUS] = updated.watchChargingStatus
            prefs[KEY_WATCH_MODEL] = updated.watchModel
            prefs[KEY_LAST_SYNC] = updated.lastSyncTimestamp
            prefs[KEY_VERSION_CODE] = updated.watchAppVersionCode
            prefs[KEY_UPDATE_AVAILABLE] = updated.isWatchUpdateAvailable
            prefs[KEY_PREMIUM] = updated.isPremiumUnlocked
        }
    }

    suspend fun updateTelemetry(batteryLevel: Int, isCharging: Boolean, model: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BATTERY_LEVEL] = batteryLevel
            prefs[KEY_CHARGING_STATUS] = isCharging
            prefs[KEY_WATCH_MODEL] = model
            prefs[KEY_LAST_SYNC] = System.currentTimeMillis()
        }
        recordBatterySample(batteryLevel, isCharging)
    }

    suspend fun recordBatterySample(batteryLevel: Int, isCharging: Boolean) {
        context.dataStore.edit { prefs ->
            val existing = prefs[KEY_BATTERY_HISTORY]?.let {
                runCatching { json.decodeFromString<List<BatterySample>>(it) }.getOrNull()
            } ?: emptyList()

            val newSample = BatterySample(System.currentTimeMillis(), batteryLevel, isCharging)
            val updated = (existing + newSample).takeLast(200) // retain last 200 points for charts
            prefs[KEY_BATTERY_HISTORY] = json.encodeToString(updated)
        }
    }

    suspend fun savePredictiveModel(model: PredictiveModelData) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PREDICTIVE_DATA] = json.encodeToString(model)
        }
    }

    suspend fun resetHabits() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_PREDICTIVE_DATA)
            prefs.remove(KEY_BATTERY_HISTORY)
        }
    }
}
