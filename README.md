# Watch Battery Notification (Wear Battery Notification v2.5.6)

A professional, high-precision battery monitoring and alert utility for Android smartphones and Wear OS smartwatches.

Designed in accordance with **Google Stitch Design System** (`projects/16886798371268845664`), `Design.md`, `logo.md`, and `WEAR_BATTERY_NOTIFICATION_COMPLETE_SPECIFICATION.txt`.

---

## 📱 Deliverables & Release APKs

Compiled ready-to-install Android and Wear OS application packages located in `release_apks/`:

| Target | Package Name | Application Name | Min SDK | Target SDK | Artifact Location | Size |
|---|---|---|---|---|---|---|
| **Android Handheld** | `com.watchbattery.notification` | Watch Battery Notification | 26 (Android 8.0+) | 36 (Android 16) | [`release_apks/WatchBatteryNotification-Phone.apk`](file:///home/ubuntu/.antigravity-rc/Wear%20Battery%20Notification/release_apks/WatchBatteryNotification-Phone.apk) | 20 MB |
| **Wear OS Smartwatch** | `com.watchbattery.notification.wear` | Watch Battery Notification | 30 (Wear OS 3+) | 36 (Wear OS 5+) | [`release_apks/WatchBatteryNotification-WearOS.apk`](file:///home/ubuntu/.antigravity-rc/Wear%20Battery%20Notification/release_apks/WatchBatteryNotification-WearOS.apk) | 30 MB |

---

## 🚀 Key Features

### 1. Two-Way Real-Time Smartwatch Telemetry
- Connected via Google Play Services Wearable Data Layer (`/watch_status`).
- Zero-drain power-efficient updates:
  - While charging: dispatches telemetry every 1% increment for live phone progress monitoring.
  - While discharging: dispatches telemetry on percentage decrements and state changes.
- Immediate hardware event dispatching for `/watch_charging_started`, `/watch_charging_stopped` (charger slippage), `/watch_fully_charged`, and `/watch_low_battery`.

### 2. Lithium-Ion Non-Linear Predictive Engine (`ChargeBlock`)
- **20 Piecewise 5% Blocks:** Accurately models the CC-CV (Constant Current - Constant Voltage) charging curve from 0% to 100%.
- **Exponential Moving Average (EMA):** Adapts to individual battery age, dock output, and charging cables.
- **Discharge Clustering:** Differentiates daytime active usage from night sleep idle drain.
- **Anomaly Budgeting:** Filters out battery level jumps (e.g. device reboot) without corrupting learned historical weights.
- **Reset Battery Habits:** Instantly re-calibrates when user purchases a new charger or watch.

### 3. Audio & Notification Alert Pipeline
- **Foreground Service:** `NotificationPlayerService` runs with `mediaPlayback` type and partial WakeLock.
- **Smart Volume (Screen-Aware):** High volume when screen is locked in pocket; gentle volume when holding the phone.
- **Do Not Disturb (DND) Bypass:** Uses `AudioAttributes.USAGE_ALARM` with `FLAG_AUDIBILITY_ENFORCED`.
- **Physical Phone Speaker Routing:** Forces playback through phone speakers even when Bluetooth headphones are connected.
- **Dynamic Text-to-Speech (TTS):** Replaces `#bat` tokens dynamically (e.g., *"Your watch has reached #bat percent"* -> *"Your watch has reached 80 percent"*).
- **Vibration Waveforms:** Standard, Heartbeat, and Alarm haptic rhythms.

### 4. Repetition, Snooze & Smart Reminders
- **5-Minute Snooze:** Silences current alert and schedules an exact alarm using `AlarmManager.setExactAndAllowWhileIdle`.
- **Automated Kill Switches:** Repeating low-battery alarms are automatically canceled when the watch docks onto the charger; high-battery alarms are canceled when unplugged.
- **Quick Reminder Activity:** Transparent time-picker dialog for 10m, 15m, 30m, or 1h follow-up alarms.

### 5. UI & Design System (Google Stitch `Modern Slate`)
- **Modern Slate Palette:** Balanced slate-graphite surfaces (`#101214`, `#181B1F`, `#20242A`) with crisp micro-borders (`#2B3138`).
- **5 User-Selectable Accents:** Professional Blue (`#3B82C4`), Amber Gold (`#D49A2A`), Lava Red (`#C95158`), Balanced Green (`#3E9B68`), Refined Purple (`#8064B5`).
- **Semantic Priority:** Status colors (Green = Connected/High Battery, Amber = Low Battery, Red = Disconnected/Critical) never get overridden by accent selections.
- **Dashboard:** Hero circular battery gauge, animated charging sweep, tabular percentages, live connection status pill, quick action buttons.
- **Analytics:** Interactive 24-hour / 7-day velocity curves, discharge clusters, session counter, and confidence score.
- **Settings:** Sliders with live value badges, 4 expandable event cards with voice preview, volume, DND, speaker routing, vibration selectors.
- **Troubleshooting & FAQ:** Samsung Galaxy Watch "Never sleeping apps" step-by-step exclusion instructions, Doze battery optimization guide, developer contact, and tip jar.
- **Onboarding Wizard:** 4 clean introductory screens guiding the user on first launch.

### 6. Wear OS Companion App
- **High-Contrast Dark UI:** Large 42sp central percentage, charging lightning bolt, watch model name, "Synced with Phone" badge, and large touch-friendly action buttons ("Open on Phone" and "Force Sync").
- **Wear OS Glanceable Tile:** Swipeable battery status tile implemented with `androidx.wear.tiles`.
- **Watch Face Complication Provider:** Exposes `RangedValueComplicationData` (0-100% arc) and `ShortTextComplicationData` for third-party watch faces.

---

## 🛠 Project Structure

```
Wear Battery Notification/
├── Design.md                                # Google Stitch design tokens & UI principles
├── logo.md                                  # Vector logo and branding specification
├── WEAR_BATTERY_NOTIFICATION_COMPLETE_SPECIFICATION.txt # System blueprint
├── settings.gradle.kts                      # Multi-module Gradle configuration
├── build.gradle.kts                         # Root build file
├── gradle/libs.versions.toml                # Version catalog (AGP 9.2.1, Kotlin 2.3.21, Compose)
├── release_apks/                            # Finalized compiled APK binaries
│   ├── WatchBatteryNotification-Phone.apk
│   └── WatchBatteryNotification-WearOS.apk
├── core/                                    # Shared business logic and telemetry models
│   └── src/main/java/pl/example/bateria/core/
│       ├── constants/WearConstants.kt       # Data layer URIs, channels, intent actions
│       ├── model/BatteryTelemetry.kt        # BatteryStatus, WatchSettingsSync, ConnectionStatus
│       ├── model/AppSettings.kt             # Full persistent configuration data class
│       └── physics/ChargeBlock.kt           # Piecewise CC-CV Lithium-Ion predictive physics
├── phone/                                   # Handheld Android companion app (com.watchbattery.notification)
│   └── src/main/java/pl/example/bateria/
│       ├── WatchBatteryApp.kt               # Application class & notification channels
│       ├── data/AppSettingsRepository.kt    # Reactive DataStore Preferences repository
│       ├── service/PhoneWearableListenerService.kt # Telemetry listener & threshold evaluator
│       ├── service/NotificationPlayerService.kt    # Foreground service: Audio, TTS, Haptics
│       ├── service/ChargingNotificationManager.kt  # Persistent rainbow charging notification
│       ├── receiver/SmartAlarmReceiver.kt          # Exact alarm repetition scheduler
│       ├── receiver/NotificationActionReceiver.kt  # Snooze (+5m) & stop actions
│       ├── receiver/BootCompletedReceiver.kt       # Boot restoration
│       ├── ui/MainActivity.kt                      # Jetpack Compose navigation scaffold
│       ├── ui/QuickAlarmActivity.kt                # Transparent reminder overlay
│       ├── ui/theme/                               # Colors, tokens, typography, and styling
│       ├── ui/components/                          # Hero gauge, status pill, cards, event cards
│       └── ui/screens/                             # Dashboard, Analytics, Settings, FAQ, Onboarding
└── wear/                                    # Smartwatch Wear OS companion app (com.watchbattery.notification.wear)
    └── src/main/java/pl/example/bateria/wear/
        ├── WearBatteryApp.kt                # Watch application class & local cache
        ├── receiver/WatchBatteryReceiver.kt # Sticky BATTERY_CHANGED listener & power dispatcher
        ├── service/WatchWearableListenerService.kt # Incoming sync & launch service
        ├── tile/BatteryTileService.kt       # Glanceable Wear OS Tile
        ├── complication/BatteryComplicationService.kt # Watch face battery complication
        └── ui/WearMainActivity.kt           # Wear OS Compose circular UI
```

---

## 🔨 Building from Source

Build both Phone and Wear OS APKs using Gradle:

```bash
# Compile and test core library
./gradlew :core:assembleDebug :core:test --no-daemon

# Compile Android handheld app
./gradlew :phone:assembleDebug --no-daemon

# Compile Wear OS smartwatch companion app
./gradlew :wear:assembleDebug --no-daemon

# Run all unit tests
./gradlew test --no-daemon
```
