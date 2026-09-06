package pl.example.bateria.core.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class AppSettingsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testBatteryStatusSerialization() {
        val status = BatteryStatus(
            battery_level = 82,
            is_charging = true,
            time = 1725559200000L,
            watch_model = "Galaxy Watch 6",
            watch_app_version_code = 256
        )

        val serialized = json.encodeToString(status)
        val deserialized = json.decodeFromString<BatteryStatus>(serialized)

        assertEquals(82, deserialized.battery_level)
        assertTrue(deserialized.is_charging)
        assertEquals("Galaxy Watch 6", deserialized.watch_model)
    }

    @Test
    fun testDynamicTtsTokenReplacement() {
        val template = "Your watch has reached #bat percent."
        val level = 85
        val replaced = template.replace("#bat", level.toString())
        assertEquals("Your watch has reached 85 percent.", replaced)
    }

    @Test
    fun testVibrationPatterns() {
        assertEquals(4, VibrationPattern.STANDARD.timings.size)
        assertEquals(8, VibrationPattern.HEARTBEAT.timings.size)
        assertEquals(6, VibrationPattern.ALARM.timings.size)
    }

    @Test
    fun testDefaultSettingsIntegrity() {
        val settings = AppSettings()
        assertEquals(15, settings.lowBatteryThreshold)
        assertEquals(80, settings.highBatteryThreshold)
        assertTrue(settings.phoneStartSound)
        assertFalse(settings.phoneStartVoice)
        assertEquals(80, settings.notificationVolume)
        assertTrue(settings.vibrationEnabled)
        assertTrue(settings.chargingNotificationEnabled)
        assertEquals(AccentColor.PROFESSIONAL_BLUE, settings.accentColor)
    }
}
