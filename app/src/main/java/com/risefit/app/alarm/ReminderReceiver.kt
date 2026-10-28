package com.risefit.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.risefit.app.data.AppDatabase
import com.risefit.app.data.AppSettings
import com.risefit.app.data.PlusDatabase
import com.risefit.app.util.fa
import com.risefit.app.util.formatTime
import com.risefit.app.util.maskHas
import com.risefit.app.util.dayIndexOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(ReminderScheduler.EXTRA_KIND) ?: return
        val id = intent.getIntExtra(ReminderScheduler.EXTRA_ID, 0)
        val app = context.applicationContext
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = AppSettings(app).load()
                when (kind) {
                    ReminderScheduler.KIND_ITEM -> {
                        val item = AppDatabase.get(app).scheduleDao().getAll().firstOrNull { it.id == id }
                        if (item != null && item.enabled && settings.activityReminders) {
                            val mask = if ((item.daysMask and 0b1111111) == 0) 0b1111111 else item.daysMask
                            if (maskHas(mask, dayIndexOf(LocalDate.now()))) {
                                Notifier.showReminder(
                                    app, 5000 + id,
                                    item.title,
                                    "برنامه‌ات برای ساعت ${formatTime(item.hour, item.minute)} آماده است. هر وقت آماده بودی شروع کن."
                                )
                            }
                            ReminderScheduler.scheduleItem(app, item.id, item.hour, item.minute, item.daysMask)
                        }
                    }
                    ReminderScheduler.KIND_WATER -> {
                        if (settings.waterReminders) {
                            Notifier.showReminder(app, 6000 + id, "وقت آب خوردن", "یک لیوان آب بنوش و بدنت را سرحال نگه دار.")
                            ReminderScheduler.scheduleWater(app, id)
                        }
                    }
                    ReminderScheduler.KIND_CUSTOM -> {
                        val dao = PlusDatabase.get(app).dao()
                        val r = dao.reminderById(id)
                        if (r != null && r.enabled) {
                            val recurring = (r.daysMask and 0b1111111) != 0
                            if (!recurring || maskHas(r.daysMask, dayIndexOf(LocalDate.now()))) {
                                Notifier.showReminder(
                                    app, 10000 + id, r.title,
                                    "یادآوری ساعت ${formatTime(r.hour, r.minute)}"
                                )
                            }
                            if (recurring) ReminderScheduler.scheduleCustom(app, r)
                            else dao.updateReminder(r.copy(enabled = false))
                        }
                    }
                    ReminderScheduler.KIND_FOCUS -> {
                        val endAt = intent.getLongExtra(ReminderScheduler.EXTRA_END_AT, 0L)
                        val minutes = intent.getIntExtra(ReminderScheduler.EXTRA_MINUTES, 0)
                        val label = intent.getStringExtra(ReminderScheduler.EXTRA_LABEL) ?: ""
                        val isBreak = intent.getBooleanExtra(ReminderScheduler.EXTRA_BREAK, false)
                        if (isBreak) {
                            Notifier.showReminder(
                                app, 9100, "استراحت تمام شد",
                                "آماده‌ای برای جلسه بعدی؟ هر وقت خواستی شروع کن."
                            )
                        } else {
                            FocusLogger.logOnce(app, endAt, minutes, label)
                            Notifier.showReminder(
                                app, 9100, "جلسه تمرکز تمام شد",
                                "آفرین! ${minutes.fa()} دقیقه تمرکز ثبت شد. کمی استراحت کن."
                            )
                        }
                    }
                    ReminderScheduler.KIND_SLEEP -> {
                        if (settings.sleepReminder) {
                            Notifier.showReminder(
                                app, 7000, "وقت آرام‌سازی",
                                "خواب کافی بخشی از تمرین است. آماده خواب شو و آلارم فردا را چک کن."
                            )
                            ReminderScheduler.scheduleSleep(app, settings.sleepHour, settings.sleepMinute)
                        }
                    }
                }
            } finally {
                result.finish()
            }
        }
    }
}
