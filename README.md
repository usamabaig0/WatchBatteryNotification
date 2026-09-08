# Watch Battery Notification

Watch Battery Notification alerts your phone when your smartwatch reaches your target battery level, finishes charging, drops low, or accidentally disconnects from its charger.

The app works with Wear OS smartwatches (Samsung Galaxy Watch, Google Pixel Watch, OnePlus Watch, and others) and Android smartphones.

---

## Downloads

Download the latest version directly from the [Releases page](https://github.com/ononymuos/WatchBatteryNotification/releases/tag/v2.5.6):

| Device | Package | Download Link | Requirements |
|---|---|---|---|
| Android Phone | Phone App | [Download Phone APK](https://github.com/ononymuos/WatchBatteryNotification/releases/download/v2.5.6/WatchBatteryNotification-Phone.apk) | Android 8.0 or newer (tested through Android 16) |
| Wear OS Smartwatch | Watch Companion | [Download Wear OS APK](https://github.com/ononymuos/WatchBatteryNotification/releases/download/v2.5.6/WatchBatteryNotification-WearOS.apk) | Wear OS 3, 4, 5, or 6 |

---

## What problems this app solves

Most smartwatches do not notify your phone when they finish charging. That leads to a few common frustrations that this app fixes:

### 1. Forgetting your watch on the charger
Leaving a smartwatch on its charging dock for hours after reaching 100% keeps the battery warm and wears it down faster over time. If you prefer to stop charging at 80% or 85% to protect long-term battery health, you usually have to get up and check the watch face manually. This app rings your phone the moment your watch reaches your chosen percentage, so you can unplug it right away.

### 2. The watch slipping off the magnetic dock
Smartwatch charging pucks are small and light. A slight bump to your nightstand, a pet walking by, or cable tension can break the magnetic contact. When that happens, charging stops without any sound. You only discover it hours later when you pick up a dead watch. The app detects unexpected charging stops and sounds an alert immediately so you can put the watch back on the charger.

### 3. Leaving home with a low battery
If your watch battery drops low during the day, you might not notice the small battery icon until the watch shuts down. You can set a low-battery alert (such as 20%) that rings your phone so you have time to charge before heading out.

### 4. Delayed notifications when your phone is locked
Android includes battery savers (such as Doze mode) that pause background apps when the phone is sitting on a desk with the screen turned off. Many battery monitors get delayed by 10 to 30 minutes, only ringing once you pick up your phone. This app uses exact system alarms and foreground audio to deliver your notification at the exact second the battery threshold is crossed.

### 5. Silent alerts when Bluetooth headphones are connected
If you leave wireless earbuds in their charging case nearby, your phone might stay connected to them over Bluetooth. Standard alerts often play through the earbuds on your desk where you cannot hear them. An optional setting forces the alarm to play through your phone physical speakers, ensuring you hear the alert across the room.

---

## Supported smartwatches and phones

### Smartwatches
The companion watch app works on any smartwatch running Wear OS 3 or higher, including:

- Samsung Galaxy Watch 4, Watch 4 Classic
- Samsung Galaxy Watch 5, Watch 5 Pro
- Samsung Galaxy Watch 6, Watch 6 Classic
- Samsung Galaxy Watch 7, Watch Ultra
- Google Pixel Watch, Pixel Watch 2, Pixel Watch 3
- OnePlus Watch 2, OnePlus Watch 2R
- Xiaomi Watch 2, Xiaomi Watch 2 Pro
- TicWatch Pro 3, Pro 5

### Phones
The phone app works on any smartphone running Android 8.0 up to Android 16.

---

## How to install

Installing the app requires putting the phone app on your phone and the watch app on your watch.

### Step 1: Install the phone app
1. On your Android phone, download [WatchBatteryNotification-Phone.apk](https://github.com/ononymuos/WatchBatteryNotification/releases/download/v2.5.6/WatchBatteryNotification-Phone.apk).
2. Tap the downloaded file to install it. If prompted, allow your browser or file manager to install apps from this source.
3. Open the app and grant notification and alarm permissions when asked.

### Step 2: Install the watch companion app
Because the watch app is distributed as an APK file, you install it on your smartwatch using wireless debugging. You do not need a computer if you use your phone.

#### Option A: Using your phone (no computer needed)
1. On your smartwatch, go to **Settings > About watch > Software info** and tap **Software version** 7 times to turn on Developer options.
2. In **Settings > Developer options**, turn on **Wireless debugging** and connect to your home Wi-Fi network.
3. Install a free tool like **Bugjaeger Mobile ADB** or **Geminiman Wear OS Manager** from the Google Play Store on your phone.
4. Pair your phone to your watch using the IP address and pairing code shown on your watch screen.
5. In the tool, select [WatchBatteryNotification-WearOS.apk](https://github.com/ononymuos/WatchBatteryNotification/releases/download/v2.5.6/WatchBatteryNotification-WearOS.apk) and click install.

#### Option B: Using a computer with ADB
If you have the Android platform tools installed on your PC or Mac:
```bash
adb connect <watch-ip-address>:<port>
adb install -r WatchBatteryNotification-WearOS.apk
```

Once installed on both devices, the phone and watch pair automatically over Bluetooth.

---

## Settings and customization

You can customize each alert from the settings screen on your phone:

- **Target charge level:** Set the alert trigger anywhere between 50% and 100%.
- **Low battery level:** Choose when you want a low-battery alert, between 5% and 40%.
- **Charger disconnected alert:** Toggle alerts when the watch is unplugged or slips off the charging puck.
- **Repeating alarm:** Choose how often the alarm repeats (every 10 seconds, 30 seconds, 1 minute, 5 minutes, up to 10 hours) until you acknowledge it.
- **Snooze:** Tap snooze on any notification to pause alerts for 5 minutes.
- **Voice alerts:** Use text to speech to announce the watch battery level out loud.
- **Physical speaker playback:** Play alerts out loud even if Bluetooth headphones or speakers are connected.
- **Do Not Disturb bypass:** Allow critical battery alerts to sound even when your phone is in Do Not Disturb mode.
- **Light and dark theme:** Choose between dark, light, or system appearance.

---

## Keeping notifications reliable on Samsung and Pixel phones

Android aggressive battery savers can sometimes put background apps to sleep. To make sure you never miss an alert:

### On Samsung Galaxy phones
1. Go to **Settings > Apps > Watch Battery Notification > Battery**.
2. Select **Unrestricted**.
3. Go to **Settings > Battery > Background usage limits > Never sleeping apps**, tap the plus icon, and add Watch Battery Notification.

### On Google Pixel phones
1. Go to **Settings > Apps > See all apps > Watch Battery Notification > App battery usage**.
2. Select **Unrestricted**.

---

## For developers

<details>
<summary>Click to view project architecture and build steps</summary>

### Architecture
The project is built with Kotlin and Jetpack Compose across three modules:
- `:core`: Shared models, battery physics calculation, and communication constants.
- `:phone`: Android handheld app, background services, AlarmManager scheduling, and audio playback.
- `:wear`: Wear OS companion app, Wearable Data Layer listeners, and battery broadcast receivers.

### Building from source
Clone the repository and run:
```bash
git clone https://github.com/ononymuos/WatchBatteryNotification.git
cd WatchBatteryNotification
./gradlew assembleRelease
```

Compiled APKs will be output to:
- `phone/build/outputs/apk/release/`
- `wear/build/outputs/apk/release/`

</details>

---

## License

This project is licensed under the Apache License 2.0. See the LICENSE file for details.
