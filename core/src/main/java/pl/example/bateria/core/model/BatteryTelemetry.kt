package pl.example.bateria.core.model

import kotlinx.serialization.Serializable

@Serializable
data class BatteryStatus(
    val battery_level: Int,
    val is_charging: Boolean,
    val time: Long = System.currentTimeMillis(),
    val watch_model: String = "Smartwatch",
    val watch_app_version_code: Int = 256
)

@Serializable
data class WatchSettingsSync(
    val lowBatteryThreshold: Int,
    val highBatteryThreshold: Int
)

enum class ConnectionStatus {
    CONNECTED,
    CONNECTING,
    DISCONNECTED
}
