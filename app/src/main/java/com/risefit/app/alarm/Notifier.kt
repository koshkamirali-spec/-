package com.risefit.app.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import com.risefit.app.MainActivity

object Notifier {
    const val CHANNEL_ALARM = "risefit_alarm"
    const val CHANNEL_ALARM_LOUD = "risefit_alarm_loud"
    const val CHANNEL_REMINDER = "risefit_reminder"
    const val FALLBACK_NOTIF_ID = 4243

    fun ensureChannels(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ALARM) == null) {
            val ch = NotificationChannel(CHANNEL_ALARM, "آلارم صبحگاهی", NotificationManager.IMPORTANCE_HIGH)
            ch.description = "نمایش آلارم‌های RiseFit"
            ch.setSound(null, null) // the sound is played by AlarmService itself
            ch.enableVibration(false)
            ch.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            nm.createNotificationChannel(ch)
        }
        if (nm.getNotificationChannel(CHANNEL_ALARM_LOUD) == null) {
            // Backup channel: the notification itself rings, used only if the alarm service cannot start.
            val ch = NotificationChannel(CHANNEL_ALARM_LOUD, "آلارم (پشتیبان)", NotificationManager.IMPORTANCE_HIGH)
            ch.description = "اگر سرویس آلارم اجرا نشود، خود اعلان زنگ می‌خورد"
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ch.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), attrs)
            ch.enableVibration(true)
            ch.vibrationPattern = longArrayOf(0, 700, 500, 700)
            ch.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            nm.createNotificationChannel(ch)
        }
        if (nm.getNotificationChannel(CHANNEL_REMINDER) == null) {
            val ch = NotificationChannel(CHANNEL_REMINDER, "یادآوری‌ها", NotificationManager.IMPORTANCE_DEFAULT)
            ch.description = "یادآوری تمرین، آب و خواب"
            nm.createNotificationChannel(ch)
        }
    }

    /** Last-resort alarm: a loud, repeating notification with a full-screen intent. */
    fun showFallbackAlarm(c: Context, id: Int) {
        try {
            ensureChannels(c)
            val nm = c.getSystemService(NotificationManager::class.java) ?: return
            AlarmState.set(id)
            val ringIntent = Intent(c, AlarmRingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, id)
            val full = PendingIntent.getActivity(
                c, 1, ringIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val n = Notification.Builder(c, CHANNEL_ALARM_LOUD)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("وقت بیدار شدن است")
                .setContentText("برای خاموش کردن آلارم اینجا را بزن")
                .setCategory(Notification.CATEGORY_ALARM)
                .setOngoing(true)
                .setContentIntent(full)
                .setFullScreenIntent(full, true)
                .build()
            n.flags = n.flags or Notification.FLAG_INSISTENT
            nm.notify(FALLBACK_NOTIF_ID, n)
        } catch (_: Exception) {
        }
    }

    fun cancelFallback(c: Context) {
        try {
            c.getSystemService(NotificationManager::class.java)?.cancel(FALLBACK_NOTIF_ID)
        } catch (_: Exception) {
        }
    }

    fun showReminder(c: Context, id: Int, title: String, text: String) {
        ensureChannels(c)
        val nm = c.getSystemService(NotificationManager::class.java) ?: return
        val open = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(c, CHANNEL_REMINDER)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            nm.notify(id, n)
        } catch (_: Exception) {
            // notification permission denied: reminders are optional, never crash
        }
    }
}
