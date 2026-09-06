package com.watchbattery.notification.wear

import com.watchbattery.notification.core.constants.WearConstants
import com.watchbattery.notification.core.model.BatteryStatus
import com.watchbattery.notification.core.model.WatchSettingsSync
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer

class WearSyncTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testDeepLinkUriMatchesPhone() {
        assertEquals("watchbattery://open", WearConstants.DEEP_LINK_URI)
        assertEquals("watchbattery", WearConstants.DEEP_LINK_SCHEME)
        assertEquals("open", WearConstants.DEEP_LINK_HOST)
    }

    @Test
    fun testPathsConsistency() {
        assertEquals("/watch_status", WearConstants.PATH_WATCH_STATUS)
        assertEquals("/request_status_update", WearConstants.PATH_REQUEST_STATUS_UPDATE)
        assertEquals("/open_app_on_phone", WearConstants.PATH_OPEN_APP_ON_PHONE)
        assertEquals("/open_app_on_watch", WearConstants.PATH_OPEN_APP_ON_WATCH)
        assertEquals("/request_settings_sync", WearConstants.PATH_REQUEST_SETTINGS_SYNC)
        assertEquals("/settings_sync_response", WearConstants.PATH_SETTINGS_SYNC_RESPONSE)
    }

    @Test
    fun testTelemetrySerialization() {
        val status = BatteryStatus(
            battery_level = 88,
            is_charging = false,
            time = 1725605000000L,
            watch_model = "Galaxy Watch 6",
            watch_app_version_code = 256
        )
        val serialized = json.encodeToString(status)
        val parsed = json.decodeFromString<BatteryStatus>(serialized)

        assertEquals(88, parsed.battery_level)
        assertFalse(parsed.is_charging)
        assertEquals("Galaxy Watch 6", parsed.watch_model)
        assertEquals(256, parsed.watch_app_version_code)
    }

    @Test
    fun testIntBufferSerialization() {
        val testLevel = 73
        val buffer = ByteBuffer.allocate(4).putInt(testLevel).array()
        val readLevel = ByteBuffer.wrap(buffer).int
        assertEquals(73, readLevel)
    }

    @Test
    fun testSettingsSyncSerialization() {
        val sync = WatchSettingsSync(
            lowBatteryThreshold = 25,
            highBatteryThreshold = 90
        )
        val serialized = json.encodeToString(sync)
        val parsed = json.decodeFromString<WatchSettingsSync>(serialized)

        assertEquals(25, parsed.lowBatteryThreshold)
        assertEquals(90, parsed.highBatteryThreshold)
    }

    @Test
    fun testDeepLinkPackageMatching() {
        assertEquals("com.watchbattery.notification", WearConstants.PACKAGE_NAME)
    }

    @Test
    fun testCapabilitiesMatching() {
        assertEquals("watch_battery_notification_capability", WearConstants.CAPABILITY_WEAR_APP)
        assertEquals("watch_battery_phone_capability", WearConstants.CAPABILITY_PHONE_APP)
    }
}
