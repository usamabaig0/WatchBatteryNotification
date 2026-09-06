package com.watchbattery.notification

import com.watchbattery.notification.core.constants.WearConstants
import com.watchbattery.notification.core.model.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class SyncProtocolTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testDeepLinkUri() {
        assertEquals("watchbattery://open", WearConstants.DEEP_LINK_URI)
        assertEquals("watchbattery", WearConstants.DEEP_LINK_SCHEME)
        assertEquals("open", WearConstants.DEEP_LINK_HOST)
    }

    @Test
    fun testCapabilityConstants() {
        assertEquals("watch_battery_notification_capability", WearConstants.CAPABILITY_WEAR_APP)
        assertEquals("watch_battery_phone_capability", WearConstants.CAPABILITY_PHONE_APP)
    }

    @Test
    fun testSettingsSyncSerialization() {
        val sync = WatchSettingsSync(
            lowBatteryThreshold = 20,
            highBatteryThreshold = 85
        )
        val serialized = json.encodeToString(sync)
        val deserialized = json.decodeFromString<WatchSettingsSync>(serialized)

        assertEquals(20, deserialized.lowBatteryThreshold)
        assertEquals(85, deserialized.highBatteryThreshold)
    }

    @Test
    fun testTelemetryDataSyncSerialization() {
        val telemetry = BatteryStatus(
            battery_level = 45,
            is_charging = true,
            time = 1725600000000L,
            watch_model = "Pixel Watch 2",
            watch_app_version_code = 256
        )
        val encoded = json.encodeToString(telemetry)
        val decoded = json.decodeFromString<BatteryStatus>(encoded)

        assertEquals(45, decoded.battery_level)
        assertTrue(decoded.is_charging)
        assertEquals("Pixel Watch 2", decoded.watch_model)
        assertEquals(256, decoded.watch_app_version_code)
    }

    @Test
    fun testUnconnectedDefaultValues() {
        val defaultSettings = AppSettings()
        assertEquals(-1, defaultSettings.watchBatteryLevel)
        assertEquals("", defaultSettings.watchModel)
        assertFalse(defaultSettings.watchChargingStatus)
        assertEquals(0L, defaultSettings.lastSyncTimestamp)
    }

    @Test
    fun testRepeatIntervalSecondsRange() {
        val settings = AppSettings(
            lowBatteryRepeatIntervalSeconds = 5,
            highBatteryRepeatIntervalSeconds = 36000
        )
        assertTrue("Min interval should be >= 5s", settings.lowBatteryRepeatIntervalSeconds >= 5)
        assertTrue("Max interval should be <= 36000s (10h)", settings.highBatteryRepeatIntervalSeconds <= 36000)
    }

    @Test
    fun testDeepLinkPackageMatching() {
        assertEquals("com.watchbattery.notification", WearConstants.PACKAGE_NAME)
    }

    @Test
    fun testPredictivePhysicsMinutesCalculation() {
        val minutes = com.watchbattery.notification.core.physics.ChargePredictivePhysics.calculateMinutesToTarget(
            currentLevel = 50,
            targetLevel = 80,
            model = com.watchbattery.notification.core.physics.PredictiveModelData()
        )
        assertTrue("Estimated minutes should be positive when current < target", minutes > 0)
    }
}
