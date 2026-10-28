package com.risefit.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.risefit.app.MainActivity
import com.risefit.app.data.AlarmEntity
import com.risefit.app.data.AppDatabase
import com.risefit.app.util.nextOccurrence
import java.time.LocalDateTime
import java.time.ZoneId

object AlarmScheduler {

    const val EXTRA_ALARM_ID = "alarm_id"
    const val EXTRA_SNOOZE = "is_snooze"
    const val EXTRA_TEST = "is_test"
    const val TEST_ID = -2
    private const val TEST_CODE = 999999
    private const val SNOOZE_BASE = 100000
    private const val ALL_DAYS = 0b1111111

    private fun alarmManager(c: Context) = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pending(c: Context, id: Int, snooze: Boolean): PendingIntent {
        val intent = Intent(c, AlarmReceiver::class.java)
            .putExtra(EXTRA_ALARM_ID, id)
            .putExtra(EXTRA_SNOOZE, snooze)
        val code = if (snooze) SNOOZE_BASE + id else id
        return PendingIntent.getBroadcast(
            c, code, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun showIntent(c: Context): PendingIntent =
        PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** Next trigger time for an alarm. Alarms with no day selected are one-shot: next time the clock hits. */
    fun nextTrigger(alarm: AlarmEntity, from: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        val mask = if ((alarm.daysMask and ALL_DAYS) == 0) ALL_DAYS else alarm.daysMask
        return nextOccurrence(alarm.hour, alarm.minute, mask, from)
    }

    /** Earliest upcoming trigger among enabled alarms (used by the home screen). */
    fun nextAmong(alarms: List<AlarmEntity>): Pair<AlarmEntity, LocalDateTime>? =
        alarms.filter { it.enabled }
            .mapNotNull { a -> nextTrigger(a)?.let { a to it } }
            .minByOrNull { it.second }

    fun schedule(c: Context, alarm: AlarmEntity) {
        // Only the regular pending alarm is replaced; a pending snooze keeps counting down.
        alarmManager(c).cancel(pending(c, alarm.id, false))
        if (!alarm.enabled) return
        val next = nextTrigger(alarm) ?: return
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // setAlarmClock is exempt from the exact-alarm permission and is the correct API for wake-up alarms.
        alarmManager(c).setAlarmClock(
            AlarmManager.AlarmClockInfo(millis, showIntent(c)),
            pending(c, alarm.id, false)
        )
    }

    fun scheduleSnooze(c: Context, id: Int, minutes: Int) {
        val millis = System.currentTimeMillis() + minutes.coerceAtLeast(1) * 60_000L
        alarmManager(c).setAlarmClock(
            AlarmManager.AlarmClockInfo(millis, showIntent(c)),
            pending(c, id, true)
        )
    }

    /** Rings a test alarm after [seconds] using the same path as a real alarm. */
    fun scheduleTest(c: Context, seconds: Int) {
        val millis = System.currentTimeMillis() + seconds.coerceAtLeast(3) * 1000L
        val intent = Intent(c, AlarmReceiver::class.java)
            .putExtra(EXTRA_ALARM_ID, TEST_ID)
            .putExtra(EXTRA_TEST, true)
        val pi = PendingIntent.getBroadcast(
            c, TEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager(c).setAlarmClock(AlarmManager.AlarmClockInfo(millis, showIntent(c)), pi)
    }

    fun cancel(c: Context, id: Int) {
        val am = alarmManager(c)
        am.cancel(pending(c, id, false))
        am.cancel(pending(c, id, true))
    }

    suspend fun rescheduleAll(c: Context) {
        val alarms = AppDatabase.get(c).alarmDao().getAll()
        for (a in alarms) {
            if (a.enabled) schedule(c, a) else cancel(c, a.id)
        }
    }
}
