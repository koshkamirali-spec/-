package com.risefit.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.risefit.app.alarm.AlarmScheduler
import com.risefit.app.alarm.ReminderScheduler
import com.risefit.app.data.AlarmEntity
import com.risefit.app.data.AppSettings
import com.risefit.app.data.FutsalPlan
import com.risefit.app.data.GoalStage
import com.risefit.app.data.GymExercise
import com.risefit.app.data.HomeDone
import com.risefit.app.data.HomeExercise
import com.risefit.app.data.PlusRepository
import com.risefit.app.data.Repository
import com.risefit.app.data.ScheduleItem
import com.risefit.app.data.SettingsState
import com.risefit.app.data.WorkoutLog
import com.risefit.app.util.LogTypes
import com.risefit.app.util.describeDuration
import com.risefit.app.util.todayEpochDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    val repo = Repository(app)
    private val plusRepo = PlusRepository(app)
    private val settingsStore = AppSettings(app)

    private fun <T> kotlinx.coroutines.flow.Flow<List<T>>.state(): StateFlow<List<T>> =
        stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val alarms = repo.alarms.state()
    val schedule = repo.schedule.state()
    val gym = repo.gym.state()
    val home = repo.home.state()
    val homeDone = repo.homeDone.state()
    val futsal = repo.futsal.state()
    val logs = repo.logs.state()
    val goals = repo.goals.state()

    private val _settings = MutableStateFlow(settingsStore.load())
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    /** Which tab the workout screen opens on: 0 gym, 1 home/abs, 2 futsal. */
    val workoutTab = MutableStateFlow(0)

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private fun say(text: String) {
        _messages.tryEmit(text)
    }

    init {
        viewModelScope.launch {
            repo.seedIfNeeded()
            try {
                AlarmScheduler.rescheduleAll(app)
                ReminderScheduler.rescheduleAll(app)
            } catch (_: Exception) {
            }
            _ready.value = true
        }
    }

    // ---------------- alarms ----------------

    fun saveAlarm(a: AlarmEntity) {
        viewModelScope.launch {
            val id = if (a.id == 0) repo.alarmDao.insert(a).toInt() else {
                repo.alarmDao.update(a)
                a.id
            }
            val saved = a.copy(id = id)
            AlarmScheduler.schedule(app, saved)
            announceAlarm(saved)
        }
    }

    fun toggleAlarm(a: AlarmEntity, on: Boolean) {
        viewModelScope.launch {
            val updated = a.copy(enabled = on)
            repo.alarmDao.update(updated)
            if (on) {
                AlarmScheduler.schedule(app, updated)
                announceAlarm(updated)
            } else {
                AlarmScheduler.cancel(app, a.id)
            }
        }
    }

    fun deleteAlarm(a: AlarmEntity) {
        viewModelScope.launch {
            AlarmScheduler.cancel(app, a.id)
            repo.alarmDao.delete(a)
        }
    }

    private fun announceAlarm(a: AlarmEntity) {
        if (!a.enabled) return
        val next = AlarmScheduler.nextTrigger(a) ?: return
        val minutes = ChronoUnit.MINUTES.between(LocalDateTime.now(), next)
        say("آلارم تنظیم شد؛ ${describeDuration(minutes)} دیگر")
    }

    // ---------------- daily schedule ----------------

    private fun reminderFor(item: ScheduleItem) {
        if (settings.value.activityReminders && item.enabled && item.category != "WAKE") {
            ReminderScheduler.scheduleItem(app, item.id, item.hour, item.minute, item.daysMask)
        } else {
            ReminderScheduler.cancelItem(app, item.id)
        }
    }

    fun saveScheduleItem(item: ScheduleItem) {
        viewModelScope.launch {
            val id = if (item.id == 0) repo.scheduleDao.insert(item).toInt() else {
                repo.scheduleDao.update(item)
                item.id
            }
            reminderFor(item.copy(id = id))
        }
    }

    fun deleteScheduleItem(item: ScheduleItem) {
        viewModelScope.launch {
            ReminderScheduler.cancelItem(app, item.id)
            repo.scheduleDao.delete(item)
        }
    }

    // ---------------- gym ----------------

    fun saveGym(ex: GymExercise) {
        viewModelScope.launch {
            if (ex.id == 0) repo.gymDao.insert(ex) else repo.gymDao.update(ex)
        }
    }

    fun deleteGym(ex: GymExercise) {
        viewModelScope.launch { repo.gymDao.delete(ex) }
    }

    // ---------------- home / abs ----------------

    fun saveHome(ex: HomeExercise) {
        viewModelScope.launch {
            if (ex.id == 0) repo.homeDao.insert(ex) else repo.homeDao.update(ex)
        }
    }

    fun deleteHome(ex: HomeExercise) {
        viewModelScope.launch {
            repo.homeDao.deleteDoneFor(ex.id)
            repo.homeDao.delete(ex)
        }
    }

    fun setHomeDone(ex: HomeExercise, done: Boolean) {
        viewModelScope.launch {
            val day = todayEpochDay()
            if (done) repo.homeDao.markDone(HomeDone(day, ex.id)) else repo.homeDao.unmarkDone(day, ex.id)
        }
    }

    // ---------------- futsal ----------------

    fun saveFutsal(p: FutsalPlan) {
        viewModelScope.launch {
            if (p.id == 0) repo.futsalDao.insert(p) else repo.futsalDao.update(p)
        }
    }

    fun deleteFutsal(p: FutsalPlan) {
        viewModelScope.launch { repo.futsalDao.delete(p) }
    }

    // ---------------- logs ----------------

    fun addLog(type: String, title: String, durationMin: Int, notes: String, epochDay: Long = todayEpochDay()) {
        viewModelScope.launch {
            repo.logDao.insert(
                WorkoutLog(
                    epochDay = epochDay, type = type, title = title,
                    durationMin = durationMin, notes = notes, points = LogTypes.points(type)
                )
            )
            say("جلسه ثبت شد. آفرین!")
        }
    }

    fun updateLog(log: WorkoutLog) {
        viewModelScope.launch { repo.logDao.update(log) }
    }

    fun deleteLog(log: WorkoutLog) {
        viewModelScope.launch { repo.logDao.delete(log) }
    }

    // ---------------- goal ----------------

    private fun stageOf(stage: Int): GoalStage = goals.value.firstOrNull { it.stage == stage } ?: GoalStage(stage = stage)

    fun saveStageText(stage: Int, goalText: String, note: String, progress: Int) {
        viewModelScope.launch {
            repo.goalDao.upsert(stageOf(stage).copy(goalText = goalText, note = note, progress = progress.coerceIn(0, 100)))
        }
    }

    fun setStagePhoto(stage: Int, uri: Uri) {
        viewModelScope.launch {
            val path = repo.importPhoto(uri)
            if (path == null) {
                say("خواندن عکس ممکن نشد")
                return@launch
            }
            val old = stageOf(stage)
            repo.goalDao.upsert(old.copy(photoUri = path))
            repo.deletePhotoFile(old.photoUri)
        }
    }

    fun setStagePreset(stage: Int, presetIndex: Int) {
        viewModelScope.launch {
            val old = stageOf(stage)
            repo.goalDao.upsert(old.copy(photoUri = "preset:$presetIndex"))
            repo.deletePhotoFile(old.photoUri)
        }
    }

    fun clearStage(stage: Int) {
        viewModelScope.launch {
            val old = stageOf(stage)
            repo.deletePhotoFile(old.photoUri)
            repo.goalDao.upsert(GoalStage(stage = stage))
            say("اطلاعات این مرحله حذف شد")
        }
    }

    // ---------------- settings ----------------

    fun updateSettings(s: SettingsState) {
        _settings.value = s
        settingsStore.save(s)
        viewModelScope.launch {
            try {
                ReminderScheduler.rescheduleAll(app)
            } catch (_: Exception) {
            }
        }
    }

    // ---------------- backup / restore / wipe ----------------

    private suspend fun cancelAllScheduled() {
        for (a in repo.alarmDao.getAll()) AlarmScheduler.cancel(app, a.id)
        for (s in repo.scheduleDao.getAll()) ReminderScheduler.cancelItem(app, s.id)
        for (r in plusRepo.dao.allReminders()) ReminderScheduler.cancelCustom(app, r.id)
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                // one file: the original data plus the planner, habits, goals, focus log, reminders and journal
                val text = JSONObject(repo.exportJson()).put("plus", plusRepo.exportJson()).toString()
                withContext(Dispatchers.IO) {
                    val out = app.contentResolver.openOutputStream(uri, "wt") ?: throw IllegalStateException("no stream")
                    out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                }
                say("پشتیبان‌گیری انجام شد")
            } catch (e: Exception) {
                say("پشتیبان‌گیری ناموفق بود")
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    val ins = app.contentResolver.openInputStream(uri) ?: throw IllegalStateException("no stream")
                    ins.use { String(it.readBytes(), Charsets.UTF_8) }
                }
                cancelAllScheduled()
                repo.importJson(text)
                JSONObject(text).optJSONObject("plus")?.let { plusRepo.importJson(it) }
                AlarmScheduler.rescheduleAll(app)
                ReminderScheduler.rescheduleAll(app)
                say("اطلاعات بازیابی شد")
            } catch (e: Exception) {
                say("این فایل پشتیبان معتبر RiseFit نیست")
            }
        }
    }

    fun wipePhotosAndGoals() {
        viewModelScope.launch {
            repo.wipePhotosAndGoals()
            say("عکس‌ها و هدف‌ها حذف شدند")
        }
    }

    fun wipeEverything() {
        viewModelScope.launch {
            cancelAllScheduled()
            repo.wipeEverything()
            plusRepo.wipeAll()
            settingsStore.clear()
            _settings.value = settingsStore.load()
            say("همه اطلاعات حذف شد")
        }
    }
}
