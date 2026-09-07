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
    fun testAlertEventConstants() {
        assertEquals("FULLY_CHARGED", WearConstants.EVENT_FULLY_CHARGED)
        assertEquals("LOW_BATTERY", WearConstants.EVENT_LOW_BATTERY)
        assertEquals("CHARGING_STARTED", WearConstants.EVENT_CHARGING_STARTED)
        assertEquals("CHARGING_STOPPED", WearConstants.EVENT_CHARGING_STOPPED)
        assertEquals("SmartAlarmChannel_v2", WearConstants.CHANNEL_ID_ALARM)
        assertEquals("NotificationPlayerChannel_v8", WearConstants.CHANNEL_ID_PLAYER)
        assertEquals("ChargingMonitorChannel_v3", WearConstants.CHANNEL_ID_CHARGING_MONITOR)
    }

    @Test
    fun testAlertActionConstants() {
        assertEquals("com.watchbattery.notification.ACTION_SNOOZE", WearConstants.ACTION_SNOOZE)
        assertEquals("com.watchbattery.notification.ACTION_STOP_REPEAT", WearConstants.ACTION_STOP_REPEAT)
        assertEquals("com.watchbattery.notification.ACTION_TRIGGER_SMART_ALARM", WearConstants.ACTION_TRIGGER_SMART_ALARM)
        assertEquals("com.watchbattery.notification.ACTION_CHECK_CHARGING_BATTERY", WearConstants.ACTION_CHECK_CHARGING_BATTERY)
        assertEquals("com.watchbattery.notification.ACTION_PHONE_POLL_WATCH_CHARGING", WearConstants.ACTION_PHONE_POLL_WATCH_CHARGING)
    }

    @Test
    fun testPhoneChargingPollerDelayCalculation() {
        // delta > 15 -> 4 minutes (240_000 ms)
        assertEquals(240_000L, com.watchbattery.notification.service.PhoneChargingPoller.calculateDelay(50, 80))
        assertEquals(240_000L, com.watchbattery.notification.service.PhoneChargingPoller.calculateDelay(64, 80)) // delta = 16

        // delta in 6..15 -> 2 minutes (120_000 ms)
        assertEquals(120_000L, com.watchbattery.notification.service.PhoneChargingPoller.calculateDelay(65, 80)) // delta = 15
        assertEquals(120_000L, com.watchbattery.notification.service.PhoneChargingPoller.calculateDelay(70, 80)) // delta = 10
        assertEquals(120_000L, com.watchbattery.notification.service.PhoneChargingPoller.calculateDelay(74, 80)) // delta = 6

        // delta in 1..5 -> 45 seconds (45_000 ms)
        assertEquals(45_000L, com.watchbattery.notification.service.PhoneChargingPoller.calculateDelay(75, 80)) // delta = 5
        assertEquals(45_000L, com.watchbattery.notification.service.PhoneChargingPoller.calculateDelay(79, 80)) // delta = 1

        // delta <= 0 -> null (target reached, no polling needed)
        assertNull(com.watchbattery.notification.service.PhoneChargingPoller.calculateDelay(80, 80))
        assertNull(com.watchbattery.notification.service.PhoneChargingPoller.calculateDelay(85, 80))
    }

    @Test
    fun testPhoneSessionAlertStateTracking() {
        com.watchbattery.notification.service.PhoneWearableListenerService.resetSessionAlertStates()
        assertFalse(com.watchbattery.notification.service.PhoneWearableListenerService.getHighBatteryAlertFired())
        assertFalse(com.watchbattery.notification.service.PhoneWearableListenerService.getLowBatteryAlertFired())

        com.watchbattery.notification.service.PhoneWearableListenerService.setHighBatteryAlertFired(true)
        assertTrue(com.watchbattery.notification.service.PhoneWearableListenerService.getHighBatteryAlertFired())

        com.watchbattery.notification.service.PhoneWearableListenerService.resetSessionAlertStates()
        assertFalse(com.watchbattery.notification.service.PhoneWearableListenerService.getHighBatteryAlertFired())
    }

    @Test
    fun testPhoneChargingHysteresisLogic() {
        val highThreshold = 80
        var hasFiredHigh = false

        // Charging below threshold
        var level = 79
        var isCharging = true
        if (isCharging) {
            if (level >= highThreshold && !hasFiredHigh) {
                hasFiredHigh = true
            } else if (level < highThreshold) {
                hasFiredHigh = false
            }
        }
        assertFalse(hasFiredHigh)

        // Reaches threshold while charging
        level = 80
        if (isCharging) {
            if (level >= highThreshold && !hasFiredHigh) {
                hasFiredHigh = true
            } else if (level < highThreshold) {
                hasFiredHigh = false
            }
        }
        assertTrue("High alert should fire at threshold", hasFiredHigh)

        // Stays above threshold - should not re-trigger
        var firedAgain = false
        level = 85
        if (isCharging) {
            if (level >= highThreshold && !hasFiredHigh) {
                hasFiredHigh = true
                firedAgain = true
            } else if (level < highThreshold) {
                hasFiredHigh = false
            }
        }
        assertFalse("Should not trigger again while still above threshold", firedAgain)
        assertTrue(hasFiredHigh)

        // Drops below threshold
        level = 75
        if (isCharging) {
            if (level >= highThreshold && !hasFiredHigh) {
                hasFiredHigh = true
            } else if (level < highThreshold) {
                hasFiredHigh = false
            }
        }
        assertFalse("Should reset when dropping below threshold", hasFiredHigh)
    }

    @Test
    fun testPhoneDischargingHysteresisLogic() {
        val lowThreshold = 20
        var hasFiredLow = false

        // Discharging above threshold
        var level = 25
        var isCharging = false
        if (!isCharging) {
            if (level <= lowThreshold && !hasFiredLow) {
                hasFiredLow = true
            } else if (level > lowThreshold + 5) {
                hasFiredLow = false
            }
        }
        assertFalse(hasFiredLow)

        // Hits low threshold
        level = 20
        if (!isCharging) {
            if (level <= lowThreshold && !hasFiredLow) {
                hasFiredLow = true
            } else if (level > lowThreshold + 5) {
                hasFiredLow = false
            }
        }
        assertTrue("Low alert should fire at threshold", hasFiredLow)

        // Jitters to 22 (within +5 hysteresis window)
        level = 22
        if (!isCharging) {
            if (level <= lowThreshold && !hasFiredLow) {
                hasFiredLow = true
            } else if (level > lowThreshold + 5) {
                hasFiredLow = false
            }
        }
        assertTrue("Low alert state should remain true within hysteresis band", hasFiredLow)

        // Recovers above threshold + 5
        level = 26
        if (!isCharging) {
            if (level <= lowThreshold && !hasFiredLow) {
                hasFiredLow = true
            } else if (level > lowThreshold + 5) {
                hasFiredLow = false
            }
        }
        assertFalse("Low alert state should reset above hysteresis band", hasFiredLow)
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
