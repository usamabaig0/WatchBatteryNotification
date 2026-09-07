package com.watchbattery.notification.wear

import com.watchbattery.notification.core.constants.WearConstants
import com.watchbattery.notification.core.model.BatteryStatus
import com.watchbattery.notification.core.model.WatchSettingsSync
import com.watchbattery.notification.wear.service.WatchChargingMonitorService
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
        assertEquals("/watch_fully_charged", WearConstants.PATH_WATCH_FULLY_CHARGED)
        assertEquals("/watch_charging_started", WearConstants.PATH_WATCH_CHARGING_STARTED)
        assertEquals("/watch_charging_stopped", WearConstants.PATH_WATCH_CHARGING_STOPPED)
        assertEquals("/watch_low_battery", WearConstants.PATH_WATCH_LOW_BATTERY)
        assertEquals("/request_status_update", WearConstants.PATH_REQUEST_STATUS_UPDATE)
        assertEquals("/open_app_on_phone", WearConstants.PATH_OPEN_APP_ON_PHONE)
        assertEquals("/open_app_on_watch", WearConstants.PATH_OPEN_APP_ON_WATCH)
        assertEquals("/request_settings_sync", WearConstants.PATH_REQUEST_SETTINGS_SYNC)
        assertEquals("/settings_sync_response", WearConstants.PATH_SETTINGS_SYNC_RESPONSE)
    }

    @Test
    fun testChargingMonitorServiceConstants() {
        assertEquals("watch_charging_monitor", WatchChargingMonitorService.CHANNEL_ID)
        assertEquals(1002, WatchChargingMonitorService.NOTIFICATION_ID)
    }

    @Test
    fun testChargingThresholdTriggerLogic() {
        val highThreshold = 80

        // Case 1: Just crossed threshold while charging -> should trigger
        val percent = 80
        val lastLevel = 79
        val isCharging = true
        val shouldTrigger = percent >= highThreshold && isCharging && (lastLevel < highThreshold || lastLevel == -1)
        assertTrue("Should trigger alert when crossing high threshold while charging", shouldTrigger)

        // Case 2: Already above threshold -> should not trigger again
        val nextPercent = 81
        val nextLastLevel = 80
        val shouldTriggerAgain = nextPercent >= highThreshold && isCharging && (nextLastLevel < highThreshold || nextLastLevel == -1)
        assertFalse("Should not trigger alert when already above threshold", shouldTriggerAgain)

        // Case 3: Reached threshold but unplugged -> should not trigger
        val unplugged = false
        val shouldTriggerUnplugged = percent >= highThreshold && unplugged && (lastLevel < highThreshold || lastLevel == -1)
        assertFalse("Should not trigger alert when unplugged", shouldTriggerUnplugged)
    }

    @Test
    fun testChargingCheckActionConstant() {
        assertEquals(
            "com.watchbattery.notification.ACTION_CHECK_CHARGING_BATTERY",
            WearConstants.ACTION_CHECK_CHARGING_BATTERY
        )
    }

    @Test
    fun testChargingCheckSessionGateLogic() {
        val highThreshold = 80
        var hasFiredThisSession = false

        // Charging at 75% -> does not fire
        var percent = 75
        var shouldFire = percent >= highThreshold && !hasFiredThisSession
        assertFalse(shouldFire)

        // Charging reaches 80% -> fires and sets flag
        percent = 80
        shouldFire = percent >= highThreshold && !hasFiredThisSession
        assertTrue(shouldFire)
        hasFiredThisSession = true

        // Charging reaches 85% in next check -> does not fire again in same session
        percent = 85
        shouldFire = percent >= highThreshold && !hasFiredThisSession
        assertFalse(shouldFire)

        // Unplugged and plugged back in -> flag reset
        hasFiredThisSession = false
        shouldFire = percent >= highThreshold && !hasFiredThisSession
        assertTrue(shouldFire)
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
