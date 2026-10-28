package com.risefit.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.risefit.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val isTest = intent.getBooleanExtra(AlarmScheduler.EXTRA_TEST, false)
        val id = if (isTest) AlarmScheduler.TEST_ID else intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        if (id < 0 && !isTest) return
        val isSnooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_SNOOZE, false)

        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (isTest) {
                    AlarmService.startRinging(app, id)
                    return@launch
                }
                val dao = AppDatabase.get(app).alarmDao()
                val alarm = dao.getById(id)
                if (alarm == null || !(alarm.enabled || isSnooze)) return@launch

                // Register the next occurrence first, so a problem while ringing can never lose tomorrow's alarm.
                if (!isSnooze) {
                    if ((alarm.daysMask and 0b1111111) == 0) {
                        dao.update(alarm.copy(enabled = false)) // one-shot alarm
                    } else {
                        AlarmScheduler.schedule(app, alarm)
                    }
                }
                AlarmService.startRinging(app, id)
            } catch (e: Exception) {
                // last resort: never stay silent
                Notifier.showFallbackAlarm(app, id)
            } finally {
                result.finish()
            }
        }
    }
}
