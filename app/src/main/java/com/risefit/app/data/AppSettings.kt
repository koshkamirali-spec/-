package com.risefit.app.data

import android.content.Context

/** Plain snapshot of user settings. */
data class SettingsState(
    val activityReminders: Boolean = true,
    val waterReminders: Boolean = true,
    val sleepReminder: Boolean = true,
    val sleepHour: Int = 22,
    val sleepMinute: Int = 0,
    /** 0 purple, 1 blue, 2 green, 3 pink */
    val accentIndex: Int = 0
)

/** SharedPreferences-backed settings (local only, readable from receivers too). */
class AppSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("risefit_settings", Context.MODE_PRIVATE)

    fun load(): SettingsState = SettingsState(
        activityReminders = prefs.getBoolean("activityReminders", true),
        waterReminders = prefs.getBoolean("waterReminders", true),
        sleepReminder = prefs.getBoolean("sleepReminder", true),
        sleepHour = prefs.getInt("sleepHour", 22),
        sleepMinute = prefs.getInt("sleepMinute", 0),
        accentIndex = prefs.getInt("accentIndex", 0)
    )

    fun save(s: SettingsState) {
        prefs.edit()
            .putBoolean("activityReminders", s.activityReminders)
            .putBoolean("waterReminders", s.waterReminders)
            .putBoolean("sleepReminder", s.sleepReminder)
            .putInt("sleepHour", s.sleepHour)
            .putInt("sleepMinute", s.sleepMinute)
            .putInt("accentIndex", s.accentIndex)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
