package com.risefit.app.alarm

import android.content.Context
import com.risefit.app.data.FocusSession
import com.risefit.app.data.PlusDatabase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Saves a finished focus session exactly once. Both the running screen and the alarm receiver
 * may notice the end of a session; the end time is used to make sure it is stored a single time.
 */
object FocusLogger {
    private val lock = Any()

    suspend fun logOnce(c: Context, endAtMillis: Long, minutes: Int, label: String): Boolean {
        if (minutes <= 0) return false
        val app = c.applicationContext
        val first = synchronized(lock) {
            val prefs = app.getSharedPreferences("risefit_focus", Context.MODE_PRIVATE)
            if (endAtMillis != 0L && prefs.getLong("lastLoggedEnd", -1L) == endAtMillis) {
                false
            } else {
                prefs.edit().putLong("lastLoggedEnd", endAtMillis).commit()
                true
            }
        }
        if (!first) return false
        val day = if (endAtMillis > 0L) {
            Instant.ofEpochMilli(endAtMillis).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
        } else {
            LocalDate.now().toEpochDay()
        }
        PlusDatabase.get(app).dao().insertFocus(FocusSession(epochDay = day, minutes = minutes, label = label))
        return true
    }
}
