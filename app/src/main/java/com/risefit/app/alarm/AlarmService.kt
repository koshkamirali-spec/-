package com.risefit.app.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import com.risefit.app.data.AppDatabase
import com.risefit.app.util.formatTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Foreground service that plays the alarm sound and vibration until dismissed or snoozed. */
class AlarmService : Service() {

    companion object {
        const val ACTION_START = "com.risefit.app.ALARM_START"
        const val ACTION_STOP = "com.risefit.app.ALARM_STOP"
        const val ACTION_SNOOZE = "com.risefit.app.ALARM_SNOOZE"
        private const val NOTIF_ID = 4242
        private const val AUTO_STOP_MS = 5 * 60_000L

        fun stopIntent(c: Context) = Intent(c, AlarmService::class.java).setAction(ACTION_STOP)
        fun snoozeIntent(c: Context) = Intent(c, AlarmService::class.java).setAction(ACTION_SNOOZE)

        /**
         * Starts the ringing service. If Android refuses to start it from the background,
         * a loud notification with a full-screen intent is shown instead so the alarm never stays silent.
         */
        fun startRinging(c: Context, id: Int) {
            val svc = Intent(c, AlarmService::class.java)
                .setAction(ACTION_START)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, id)
            try {
                ContextCompat.startForegroundService(c, svc)
            } catch (e: Exception) {
                Notifier.showFallbackAlarm(c, id)
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var startJob: Job? = null
    private var autoStopJob: Job? = null
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var currentId = -1
    private var snoozeMinutes = 5
    private var savedVolume = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> finishRinging()
            ACTION_SNOOZE -> {
                if (currentId >= 0) AlarmScheduler.scheduleSnooze(applicationContext, currentId, snoozeMinutes)
                finishRinging()
            }
            else -> beginRinging(intent)
        }
        return START_NOT_STICKY
    }

    private fun beginRinging(intent: Intent?) {
        val id = intent?.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1) ?: -1
        currentId = id
        AlarmState.set(id)
        Notifier.ensureChannels(this)
        Notifier.cancelFallback(this)

        // Must call startForeground quickly after startForegroundService().
        if (!enterForeground(buildNotification(id, "آلارم RiseFit", "وقت بیدار شدن است"))) {
            Notifier.showFallbackAlarm(this, id)
            stopSelf()
            return
        }

        // Try to open the full-screen alarm directly (works when the phone allows it); otherwise the
        // notification's full-screen intent does it.
        try {
            startActivity(
                Intent(this, AlarmRingActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(AlarmScheduler.EXTRA_ALARM_ID, id)
            )
        } catch (_: Exception) {
        }

        // Sound and vibration start immediately, they do not wait for the database.
        boostVolume()
        playSound(null)
        startVibration(true)

        startJob?.cancel()
        startJob = scope.launch {
            val alarm = if (id >= 0) {
                withContext(Dispatchers.IO) {
                    try {
                        AppDatabase.get(applicationContext).alarmDao().getById(id)
                    } catch (e: Exception) {
                        null
                    }
                }
            } else null
            snoozeMinutes = alarm?.snoozeMinutes ?: 5
            val title = when {
                id == AlarmScheduler.TEST_ID -> "تست آلارم"
                alarm != null && alarm.label.isNotBlank() -> alarm.label
                else -> "آلارم RiseFit"
            }
            val text = if (alarm != null) "ساعت ${formatTime(alarm.hour, alarm.minute)}" else "وقت بیدار شدن است"
            enterForeground(buildNotification(id, title, text))
            // switch to the user's chosen sound / vibration setting
            if (alarm != null) {
                if (!alarm.soundUri.isNullOrBlank()) playSound(alarm.soundUri)
                if (!alarm.vibrate) {
                    try {
                        vibrator?.cancel()
                    } catch (_: Exception) {
                    }
                }
            }
        }
        autoStopJob?.cancel()
        autoStopJob = scope.launch {
            delay(AUTO_STOP_MS)
            finishRinging()
        }
    }

    private fun enterForeground(n: Notification): Boolean =
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIF_ID, n)
            }
            true
        } catch (e: Exception) {
            false
        }

    private fun buildNotification(id: Int, title: String, text: String): Notification {
        val ringIntent = Intent(this, AlarmRingActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(AlarmScheduler.EXTRA_ALARM_ID, id)
        val full = PendingIntent.getActivity(
            this, 1, ringIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 2, stopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snooze = PendingIntent.getService(
            this, 3, snoozeIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, Notifier.CHANNEL_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(full)
            .setFullScreenIntent(full, true)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, android.R.drawable.ic_lock_idle_alarm), "چرت", snooze
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel), "خاموش", stop
                ).build()
            )
            .build()
    }

    /** Makes sure the alarm stream is audible (many phones keep it at zero). */
    private fun boostVolume() {
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val current = am.getStreamVolume(AudioManager.STREAM_ALARM)
            val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val target = (max * 0.8f).toInt().coerceAtLeast(1)
            if (current < target) {
                savedVolume = current
                am.setStreamVolume(AudioManager.STREAM_ALARM, target, 0)
            }
        } catch (_: Exception) {
        }
    }

    private fun restoreVolume() {
        if (savedVolume < 0) return
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.setStreamVolume(AudioManager.STREAM_ALARM, savedVolume, 0)
        } catch (_: Exception) {
        }
        savedVolume = -1
    }

    private fun playSound(soundUri: String?) {
        releasePlayer()
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val candidates = ArrayList<Uri>()
        if (!soundUri.isNullOrBlank()) candidates.add(Uri.parse(soundUri))
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.let { candidates.add(it) }
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.let { candidates.add(it) }
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)?.let { candidates.add(it) }
        for (uri in candidates) {
            try {
                val mp = MediaPlayer()
                mp.setAudioAttributes(attrs)
                mp.setDataSource(this, uri)
                mp.isLooping = true
                mp.prepare()
                mp.start()
                player = mp
                return
            } catch (_: Exception) {
                // try next candidate
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun startVibration(enabled: Boolean) {
        if (!enabled) return
        val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator = v
        try {
            v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0))
        } catch (_: Exception) {
        }
    }

    private fun releasePlayer() {
        try {
            player?.stop()
        } catch (_: Exception) {
        }
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null
    }

    private fun finishRinging() {
        startJob?.cancel()
        autoStopJob?.cancel()
        releasePlayer()
        restoreVolume()
        try {
            vibrator?.cancel()
        } catch (_: Exception) {
        }
        Notifier.cancelFallback(this)
        AlarmState.set(-1)
        currentId = -1
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        releasePlayer()
        restoreVolume()
        try {
            vibrator?.cancel()
        } catch (_: Exception) {
        }
        scope.cancel()
        super.onDestroy()
    }
}
