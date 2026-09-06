package pl.example.bateria.service

import android.app.KeyguardManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import pl.example.bateria.R
import pl.example.bateria.WatchBatteryApp
import pl.example.bateria.core.constants.WearConstants
import pl.example.bateria.core.model.AppSettings
import pl.example.bateria.receiver.NotificationActionReceiver
import pl.example.bateria.ui.MainActivity
import pl.example.bateria.ui.QuickAlarmActivity
import java.util.*

class NotificationPlayerService : Service(), TextToSpeech.OnInitListener {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var ttsInitialized = false
    private var pendingTtsText: String? = null

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MainApplication:PlayerServiceLock").apply {
            setReferenceCounted(false)
            acquire(15000L) // 15 seconds wake lock
        }
        tts = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        val eventType = intent.getStringExtra(WearConstants.EXTRA_EVENT_TYPE) ?: WearConstants.EVENT_FULLY_CHARGED
        val batteryLevel = intent.getIntExtra(WearConstants.EXTRA_BATTERY_LEVEL, 82)
        val watchModel = intent.getStringExtra(WearConstants.EXTRA_WATCH_MODEL) ?: "Galaxy Watch 6"

        // Put service into foreground immediately with status notification
        val foregroundNotification = buildForegroundNotification(eventType, batteryLevel, watchModel)
        startForeground(1001, foregroundNotification)

        serviceScope.launch {
            val settings = WatchBatteryApp.instance.repository.settingsFlow.value
            executeAlertPipeline(eventType, batteryLevel, settings)
        }

        return START_NOT_STICKY
    }

    private fun executeAlertPipeline(eventType: String, batteryLevel: Int, settings: AppSettings) {
        val (soundEnabled, voiceEnabled, customUri, ttsTemplate, rawResId) = when (eventType) {
            WearConstants.EVENT_CHARGING_STARTED -> AlertConfig(
                settings.phoneStartSound,
                settings.phoneStartVoice,
                settings.customSoundUriStart,
                settings.customTtsTextStart,
                R.raw.charging_started
            )
            WearConstants.EVENT_CHARGING_STOPPED -> AlertConfig(
                settings.phoneStopSound,
                settings.phoneStopVoice,
                settings.customSoundUriStop,
                settings.customTtsTextStop,
                R.raw.charging_stopped
            )
            WearConstants.EVENT_FULLY_CHARGED -> AlertConfig(
                settings.phoneFullSound,
                settings.phoneFullVoice,
                settings.customSoundUriFull,
                settings.customTtsTextFull,
                R.raw.fully_charged
            )
            else -> AlertConfig(
                settings.phoneLowSound,
                settings.phoneLowVoice,
                settings.customSoundUriLow,
                settings.customTtsTextLow,
                R.raw.low_battery
            )
        }

        // 1. Audio Focus and Routing
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val isLocked = keyguardManager.isKeyguardLocked

        // Check speaker routing
        if (settings.volumeForcePhoneSpeaker && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val devices = audioManager.availableCommunicationDevices
            val speakerDevice = devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            if (speakerDevice != null) {
                audioManager.setCommunicationDevice(speakerDevice)
            }
        }

        // Determine playback volume
        val targetVolume = if (settings.smartVolumeEnabled && !isLocked) {
            val currentMediaVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxMediaVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            (currentMediaVol.toFloat() / maxMediaVol.coerceAtLeast(1)).coerceIn(0.1f, 1.0f)
        } else {
            (settings.notificationVolume.coerceIn(0, 100) / 100.0f)
        }

        // 2. Play Sound
        if (soundEnabled && targetVolume > 0f) {
            playSound(customUri, rawResId, settings.volumeBypassDnd, targetVolume)
        }

        // 3. Play TTS Voice
        if (voiceEnabled) {
            val textToSpeak = ttsTemplate.replace("#bat", batteryLevel.toString())
            speakVoice(textToSpeak)
        }

        // 4. Trigger Haptics
        if (settings.vibrationEnabled) {
            triggerVibration(settings.vibrationPattern.timings)
        }

        // Stop foreground service after playback finishes
        serviceScope.launch {
            delay(5000L)
            stopSelf()
        }
    }

    private fun playSound(customUri: String?, rawResId: Int, bypassDnd: Boolean, volume: Float) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(if (bypassDnd) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .apply {
                        if (bypassDnd) setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                    }
                    .build()

                setAudioAttributes(audioAttributes)
                setVolume(volume, volume)

                if (customUri != null) {
                    setDataSource(applicationContext, Uri.parse(customUri))
                } else {
                    val afd = resources.openRawResourceFd(rawResId)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                }

                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun speakVoice(text: String) {
        if (ttsInitialized && tts != null) {
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "watch_battery_voice")
        } else {
            pendingTtsText = text
        }
    }

    private fun triggerVibration(timings: LongArray) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, -1)
        }
    }

    private fun buildForegroundNotification(eventType: String, batteryLevel: Int, watchModel: String): Notification {
        val title = when (eventType) {
            WearConstants.EVENT_CHARGING_STARTED -> "Watch charging started: $batteryLevel%"
            WearConstants.EVENT_CHARGING_STOPPED -> "Watch unplugged: $batteryLevel%"
            WearConstants.EVENT_FULLY_CHARGED -> "Watch charge target reached: $batteryLevel%"
            else -> "Low watch battery: $batteryLevel%"
        }

        val openAppIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        // Action 1: Snooze (5 min)
        val snoozeIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = WearConstants.ACTION_SNOOZE
            putExtra(WearConstants.EXTRA_EVENT_TYPE, eventType)
            putExtra(WearConstants.EXTRA_BATTERY_LEVEL, batteryLevel)
            putExtra(WearConstants.EXTRA_WATCH_MODEL, watchModel)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(this, 1, snoozeIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        // Action 2: Stop Repeating
        val stopIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = WearConstants.ACTION_STOP_REPEAT
        }
        val stopPendingIntent = PendingIntent.getBroadcast(this, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        // Action 3: Set Reminder (QuickAlarmActivity)
        val reminderIntent = Intent(this, QuickAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val reminderPendingIntent = PendingIntent.getActivity(this, 3, reminderIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(this, WearConstants.CHANNEL_ID_PLAYER)
            .setContentTitle(title)
            .setContentText("$watchModel is currently at $batteryLevel%")
            .setSmallIcon(R.drawable.ic_stat_watch_battery)
            .setContentIntent(openAppIntent)
            .setOngoing(false)
            .setAutoCancel(true)
            .addAction(0, "Snooze 5 min", snoozePendingIntent)
            .addAction(0, "Stop", stopPendingIntent)
            .addAction(0, "Set reminder", reminderPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            ttsInitialized = true
            pendingTtsText?.let {
                speakVoice(it)
                pendingTtsText = null
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        serviceScope.cancel()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private data class AlertConfig(
        val sound: Boolean,
        val voice: Boolean,
        val customUri: String?,
        val ttsTemplate: String,
        val rawResId: Int
    )
}
