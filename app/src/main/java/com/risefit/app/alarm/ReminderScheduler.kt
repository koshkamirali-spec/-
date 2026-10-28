package com.risefit.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.risefit.app.data.AppDatabase
import com.risefit.app.data.AppSettings
import com.risefit.app.data.CustomReminder
import com.risefit.app.data.PlusDatabase
import com.risefit.app.util.nextOccurrence
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Soft (inexact) reminders: planned activities, water and sleep. They need no special permission. */
object ReminderScheduler {

    const val EXTRA_KIND = "kind"
    const val EXTRA_ID = "rid"
    const val KIND_ITEM = "item"
    const val KIND_WATER = "water"
    const val KIND_SLEEP = "sleep"
    const val KIND_CUSTOM = "custom"
    const val KIND_FOCUS = "focus"
    const val EXTRA_END_AT = "end_at"
    const val EXTRA_MINUTES = "minutes"
    const val EXTRA_LABEL = "label"
    const val EXTRA_BREAK = "is_break"

    val WATER_HOURS = listOf(10, 13, 16, 19)

    private const val ITEM_BASE = 200000
    private const val WATER_BASE = 300000
    private const val SLEEP_CODE = 300100
    private const val FOCUS_CODE = 300200
    private const val CUSTOM_BASE = 400000
    private const val ALL_DAYS = 0b1111111

    private fun am(c: Context) = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pending(c: Context, code: Int, kind: String, id: Int): PendingIntent {
        val i = Intent(c, ReminderReceiver::class.java)
            .putExtra(EXTRA_KIND, kind)
            .putExtra(EXTRA_ID, id)
        return PendingIntent.getBroadcast(
            c, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun setAt(c: Context, code: Int, kind: String, id: Int, at: LocalDateTime) {
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        am(c).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending(c, code, kind, id))
    }

    private fun cancelCode(c: Context, code: Int, kind: String, id: Int) {
        am(c).cancel(pending(c, code, kind, id))
    }

    /** Exact when the phone allows it, otherwise the soft version. Used for reminders the user set by hand. */
    private fun setBestEffort(c: Context, millis: Long, pi: PendingIntent) {
        try {
            am(c).setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        } catch (e: SecurityException) {
            am(c).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        }
    }

    fun cancelCustom(c: Context, id: Int) = cancelCode(c, CUSTOM_BASE + id, KIND_CUSTOM, id)

    /** The moment a one-time reminder is meant to fire. */
    fun onceTarget(r: CustomReminder): LocalDateTime? =
        if (r.onceEpochDay >= 0) LocalDateTime.of(LocalDate.ofEpochDay(r.onceEpochDay), LocalTime.of(r.hour, r.minute))
        else nextOccurrence(r.hour, r.minute, ALL_DAYS)

    fun scheduleCustom(c: Context, r: CustomReminder) {
        cancelCustom(c, r.id)
        if (!r.enabled) return
        val recurring = (r.daysMask and ALL_DAYS) != 0
        val at = (if (recurring) nextOccurrence(r.hour, r.minute, r.daysMask) else onceTarget(r)) ?: return
        val now = LocalDateTime.now()
        // a one-time reminder that was just missed (phone was off) fires right away
        val fire = if (at.isAfter(now)) at else now.plusSeconds(3)
        val millis = fire.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        setBestEffort(c, millis, pending(c, CUSTOM_BASE + r.id, KIND_CUSTOM, r.id))
    }

    fun cancelFocusEnd(c: Context) = cancelCode(c, FOCUS_CODE, KIND_FOCUS, 0)

    fun scheduleFocusEnd(c: Context, endAtMillis: Long, minutes: Int, label: String, isBreak: Boolean) {
        val i = Intent(c, ReminderReceiver::class.java)
            .putExtra(EXTRA_KIND, KIND_FOCUS)
            .putExtra(EXTRA_ID, 0)
            .putExtra(EXTRA_END_AT, endAtMillis)
            .putExtra(EXTRA_MINUTES, minutes)
            .putExtra(EXTRA_LABEL, label)
            .putExtra(EXTRA_BREAK, isBreak)
        val pi = PendingIntent.getBroadcast(
            c, FOCUS_CODE, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setBestEffort(c, endAtMillis, pi)
    }

    fun cancelItem(c: Context, itemId: Int) = cancelCode(c, ITEM_BASE + itemId, KIND_ITEM, itemId)

    fun scheduleItem(c: Context, itemId: Int, hour: Int, minute: Int, daysMask: Int) {
        cancelItem(c, itemId)
        val mask = if ((daysMask and ALL_DAYS) == 0) ALL_DAYS else daysMask
        val next = nextOccurrence(hour, minute, mask) ?: return
        setAt(c, ITEM_BASE + itemId, KIND_ITEM, itemId, next)
    }

    fun scheduleWater(c: Context, slot: Int) {
        val hour = WATER_HOURS.getOrNull(slot) ?: return
        val next = nextOccurrence(hour, 0, ALL_DAYS) ?: return
        setAt(c, WATER_BASE + slot, KIND_WATER, slot, next)
    }

    fun scheduleSleep(c: Context, hour: Int, minute: Int) {
        val next = nextOccurrence(hour, minute, ALL_DAYS) ?: return
        setAt(c, SLEEP_CODE, KIND_SLEEP, 0, next)
    }

    suspend fun rescheduleAll(c: Context) {
        val app = c.applicationContext
        val settings = AppSettings(app).load()
        val items = AppDatabase.get(app).scheduleDao().getAll()
        for (item in items) {
            if (settings.activityReminders && item.enabled && item.category != "WAKE") {
                scheduleItem(app, item.id, item.hour, item.minute, item.daysMask)
            } else {
                cancelItem(app, item.id)
            }
        }
        for (slot in WATER_HOURS.indices) {
            if (settings.waterReminders) scheduleWater(app, slot)
            else cancelCode(app, WATER_BASE + slot, KIND_WATER, slot)
        }
        if (settings.sleepReminder) scheduleSleep(app, settings.sleepHour, settings.sleepMinute)
        else cancelCode(app, SLEEP_CODE, KIND_SLEEP, 0)

        // reminders the user created by hand
        val plusDao = PlusDatabase.get(app).dao()
        val now = LocalDateTime.now()
        for (r in plusDao.allReminders()) {
            val once = (r.daysMask and ALL_DAYS) == 0
            if (r.enabled && once) {
                val target = onceTarget(r)
                if (target == null || target.isBefore(now.minusHours(12))) {
                    plusDao.updateReminder(r.copy(enabled = false)) // long past, do not fire it late
                    cancelCustom(app, r.id)
                    continue
                }
            }
            if (r.enabled) scheduleCustom(app, r) else cancelCustom(app, r.id)
        }
    }
}
