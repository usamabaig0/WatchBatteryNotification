package com.watchbattery.notification.core.constants

object WearConstants {
    const val PACKAGE_NAME = "com.watchbattery.notification"
    const val CAPABILITY_WEAR_APP = "watch_battery_notification_capability"
    const val CAPABILITY_PHONE_APP = "watch_battery_phone_capability"

    // Wearable Data Layer Message URIs
    const val PATH_WATCH_STATUS = "/watch_status"
    const val PATH_WATCH_CHARGING_STARTED = "/watch_charging_started"
    const val PATH_WATCH_CHARGING_STOPPED = "/watch_charging_stopped"
    const val PATH_WATCH_FULLY_CHARGED = "/watch_fully_charged"
    const val PATH_WATCH_LOW_BATTERY = "/watch_low_battery"
    const val PATH_REQUEST_SETTINGS_SYNC = "/request_settings_sync"
    const val PATH_SETTINGS_SYNC_RESPONSE = "/settings_sync_response"
    const val PATH_REQUEST_STATUS_UPDATE = "/request_status_update"
    const val PATH_OPEN_APP_ON_PHONE = "/open_app_on_phone"
    const val PATH_OPEN_APP_ON_WATCH = "/open_app_on_watch"

    // Intent Actions
    const val ACTION_SNOOZE = "com.watchbattery.notification.ACTION_SNOOZE"
    const val ACTION_STOP_REPEAT = "com.watchbattery.notification.ACTION_STOP_REPEAT"
    const val ACTION_TRIGGER_SMART_ALARM = "com.watchbattery.notification.ACTION_TRIGGER_SMART_ALARM"
    const val ACTION_PLAY_NOTIFICATION = "com.watchbattery.notification.ACTION_PLAY_NOTIFICATION"
    const val ACTION_RESTORE_CHARGING_BAR = "com.watchbattery.notification.ACTION_RESTORE_CHARGING_BAR"
    const val ACTION_DISMISS_CHARGING_BAR = "com.watchbattery.notification.ACTION_DISMISS_CHARGING_BAR"
    const val ACTION_CHECK_CHARGING_BATTERY = "com.watchbattery.notification.ACTION_CHECK_CHARGING_BATTERY"
    const val ACTION_PHONE_POLL_WATCH_CHARGING = "com.watchbattery.notification.ACTION_PHONE_POLL_WATCH_CHARGING"

    // Extras
    const val EXTRA_EVENT_TYPE = "extra_event_type"
    const val EXTRA_BATTERY_LEVEL = "extra_battery_level"
    const val EXTRA_WATCH_MODEL = "extra_watch_model"
    const val EXTRA_CHARGING_STATUS = "extra_charging_status"
    const val EXTRA_TIMESTAMP = "extra_timestamp"

    // Deep Link URIs
    const val DEEP_LINK_SCHEME = "watchbattery"
    const val DEEP_LINK_HOST = "open"
    const val DEEP_LINK_URI = "watchbattery://open"

    // Event Types
    const val EVENT_CHARGING_STARTED = "CHARGING_STARTED"
    const val EVENT_CHARGING_STOPPED = "CHARGING_STOPPED"
    const val EVENT_FULLY_CHARGED = "FULLY_CHARGED"
    const val EVENT_LOW_BATTERY = "LOW_BATTERY"

    // Notification Channel IDs
    const val CHANNEL_ID_PLAYER = "NotificationPlayerChannel_v8"
    const val CHANNEL_ID_ALARM = "SmartAlarmChannel_v2"
    const val CHANNEL_ID_CHARGING_MONITOR = "ChargingMonitorChannel_v3"
}
