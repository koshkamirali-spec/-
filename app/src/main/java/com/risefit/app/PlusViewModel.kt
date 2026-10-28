package com.risefit.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.risefit.app.ai.AiClient
import com.risefit.app.ai.AiIntent
import com.risefit.app.ai.LocalAssistant
import com.risefit.app.alarm.FocusLogger
import com.risefit.app.alarm.ReminderScheduler
import com.risefit.app.data.CustomReminder
import com.risefit.app.data.FocusSession
import com.risefit.app.data.GoalItem
import com.risefit.app.data.GoalStep
import com.risefit.app.data.Habit
import com.risefit.app.data.HabitLog
import com.risefit.app.data.NoteItem
import com.risefit.app.data.PlusPrefs
import com.risefit.app.data.PlusRepository
import com.risefit.app.data.TaskItem
import com.risefit.app.util.GoalStats
import com.risefit.app.util.HabitStats
import com.risefit.app.util.Jalali
import com.risefit.app.util.PlannerStats
import com.risefit.app.util.Quotes
import com.risefit.app.util.fa
import com.risefit.app.util.formatTime
import com.risefit.app.util.nextOccurrence
import com.risefit.app.util.todayEpochDay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

enum class FocusPhase { FOCUS, BREAK }

data class FocusUi(
    val phase: FocusPhase = FocusPhase.FOCUS,
    val running: Boolean = false,
    val focusMin: Int = 25,
    val breakMin: Int = 5,
    /** valid while [running] */
    val endAtMillis: Long = 0L,
    /** time left while not running */
    val remainingMs: Long = 25 * 60_000L,
    val label: String = ""
) {
    val phaseMs: Long get() = (if (phase == FocusPhase.FOCUS) focusMin else breakMin) * 60_000L
}

data class ChatMsg(val fromUser: Boolean, val text: String)

/** State and actions of the "plus" features: planner, reminders, habits, goals, focus, journal and RiseAI. */
class PlusViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    val repo = PlusRepository(app)
    private val prefs = PlusPrefs(app)
    private val dao = repo.dao

    private fun <T> Flow<List<T>>.state(): StateFlow<List<T>> =
        stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val tasks = repo.tasks.state()
    val habits = repo.habits.state()
    val habitLogs = repo.habitLogs.state()
    val goals = repo.goals.state()
    val steps = repo.steps.state()
    val focusSessions = repo.focus.state()
    val reminders = repo.reminders.state()
    val notes = repo.notes.state()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private fun say(text: String) {
        _messages.tryEmit(text)
    }

    // ---------------- planner ----------------

    fun saveTask(t: TaskItem) {
        viewModelScope.launch {
            if (t.id == 0) dao.insertTask(t) else dao.updateTask(t)
        }
    }

    fun setTaskDone(t: TaskItem, done: Boolean) {
        viewModelScope.launch { dao.updateTask(t.copy(done = done)) }
    }

    fun deleteTask(t: TaskItem) {
        viewModelScope.launch { dao.deleteTask(t) }
    }

    /** Moves everything not finished on [day] to the next day. No guilt, just a fresh start. */
    fun moveUnfinishedToNextDay(day: Long) {
        viewModelScope.launch {
            val open = dao.tasksOn(day).filter { !it.done }
            open.forEach { dao.updateTask(it.copy(epochDay = day + 1)) }
            say(if (open.isEmpty()) "کار نیمه‌تمامی نبود" else "${open.size.fa()} کار به روز بعد منتقل شد")
        }
    }

    // ---------------- reminders ----------------

    /** Mask 0 means one-time: the date is the next time the clock reaches that hour and minute. */
    private fun normalized(r: CustomReminder): CustomReminder =
        if ((r.daysMask and 0b1111111) == 0) {
            val day = nextOccurrence(r.hour, r.minute, 0b1111111)?.toLocalDate()?.toEpochDay() ?: todayEpochDay()
            r.copy(daysMask = 0, onceEpochDay = day)
        } else {
            r.copy(daysMask = r.daysMask and 0b1111111, onceEpochDay = -1)
        }

    fun saveReminder(r: CustomReminder) {
        viewModelScope.launch {
            val fixed = normalized(r)
            val id = if (fixed.id == 0) dao.insertReminder(fixed).toInt() else {
                dao.updateReminder(fixed)
                fixed.id
            }
            val saved = fixed.copy(id = id)
            ReminderScheduler.scheduleCustom(app, saved)
            say("یادآور ذخیره شد")
        }
    }

    fun toggleReminder(r: CustomReminder, on: Boolean) {
        viewModelScope.launch {
            val updated = if (on) normalized(r.copy(enabled = true)) else r.copy(enabled = false)
            dao.updateReminder(updated)
            if (on) ReminderScheduler.scheduleCustom(app, updated) else ReminderScheduler.cancelCustom(app, r.id)
        }
    }

    fun deleteReminder(r: CustomReminder) {
        viewModelScope.launch {
            ReminderScheduler.cancelCustom(app, r.id)
            dao.deleteReminder(r)
        }
    }

    // ---------------- habits ----------------

    fun saveHabit(h: Habit) {
        viewModelScope.launch {
            if (h.id == 0) dao.insertHabit(h) else dao.updateHabit(h)
        }
    }

    fun deleteHabit(h: Habit) {
        viewModelScope.launch {
            dao.deleteLogsOfHabit(h.id)
            dao.deleteHabit(h)
        }
    }

    fun setHabitDay(habitId: Int, day: Long, done: Boolean) {
        viewModelScope.launch {
            if (done) dao.insertHabitLog(HabitLog(habitId, day)) else dao.deleteHabitLog(habitId, day)
        }
    }

    // ---------------- goals ----------------

    fun saveGoal(g: GoalItem) {
        viewModelScope.launch {
            if (g.id == 0) dao.insertGoal(g) else dao.updateGoal(g)
        }
    }

    fun deleteGoal(g: GoalItem) {
        viewModelScope.launch {
            dao.deleteStepsOfGoal(g.id)
            dao.deleteGoal(g)
        }
    }

    fun addStep(goalId: Int, text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        viewModelScope.launch { dao.insertStep(GoalStep(goalId = goalId, text = t.take(80))) }
    }

    fun setStepDone(s: GoalStep, done: Boolean) {
        viewModelScope.launch { dao.updateStep(s.copy(done = done)) }
    }

    fun deleteStep(s: GoalStep) {
        viewModelScope.launch { dao.deleteStep(s) }
    }

    fun setGoalProgress(g: GoalItem, percent: Int) {
        viewModelScope.launch { dao.updateGoal(g.copy(progress = percent.coerceIn(0, 100))) }
    }

    fun setGoalStatus(g: GoalItem, status: String) {
        viewModelScope.launch {
            dao.updateGoal(g.copy(status = status))
            if (status == "DONE") say("آفرین! یک هدف کامل شد")
        }
    }

    // ---------------- focus timer ----------------

    private val _focus = MutableStateFlow(FocusUi())
    val focusUi: StateFlow<FocusUi> = _focus.asStateFlow()
    private var focusJob: Job? = null

    private fun idleFocus(s: FocusUi): FocusUi =
        s.copy(phase = FocusPhase.FOCUS, running = false, endAtMillis = 0L, remainingMs = s.focusMin * 60_000L)

    fun setFocusLength(focusMin: Int, breakMin: Int) {
        val s = _focus.value
        if (s.running) return
        _focus.value = idleFocus(s.copy(focusMin = focusMin, breakMin = breakMin))
    }

    fun setFocusLabel(text: String) {
        _focus.value = _focus.value.copy(label = text.take(40))
    }

    fun startFocus() {
        val s = _focus.value
        if (s.running) return
        val end = System.currentTimeMillis() + s.remainingMs.coerceAtLeast(1_000L)
        _focus.value = s.copy(running = true, endAtMillis = end)
        ReminderScheduler.scheduleFocusEnd(app, end, s.focusMin, s.label, s.phase == FocusPhase.BREAK)
        startTicker()
    }

    fun pauseFocus() {
        val s = _focus.value
        if (!s.running) return
        focusJob?.cancel()
        ReminderScheduler.cancelFocusEnd(app)
        val left = (s.endAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        _focus.value = s.copy(running = false, endAtMillis = 0L, remainingMs = left)
    }

    fun resetFocus() {
        focusJob?.cancel()
        ReminderScheduler.cancelFocusEnd(app)
        _focus.value = idleFocus(_focus.value)
    }

    /** Stops a focus session early and saves the minutes that were really spent. */
    fun finishFocusEarly() {
        val s = _focus.value
        if (s.phase != FocusPhase.FOCUS) return
        val left = if (s.running) (s.endAtMillis - System.currentTimeMillis()).coerceAtLeast(0L) else s.remainingMs
        val minutes = ((s.phaseMs - left) / 60_000L).toInt()
        focusJob?.cancel()
        ReminderScheduler.cancelFocusEnd(app)
        _focus.value = idleFocus(s)
        if (minutes >= 1) {
            viewModelScope.launch {
                dao.insertFocus(FocusSession(epochDay = todayEpochDay(), minutes = minutes, label = s.label))
                say("${minutes.fa()} دقیقه تمرکز ثبت شد")
            }
        } else {
            say("کمتر از یک دقیقه بود و ثبت نشد")
        }
    }

    fun deleteFocusSession(f: FocusSession) {
        viewModelScope.launch { dao.deleteFocus(f) }
    }

    private fun startTicker() {
        focusJob?.cancel()
        focusJob = viewModelScope.launch {
            while (true) {
                delay(500)
                val s = _focus.value
                if (!s.running) break
                if (System.currentTimeMillis() >= s.endAtMillis) {
                    finishPhase(s)
                    break
                }
            }
        }
    }

    private suspend fun finishPhase(s: FocusUi) {
        if (s.phase == FocusPhase.FOCUS) {
            val saved = FocusLogger.logOnce(app, s.endAtMillis, s.focusMin, s.label)
            if (saved) say("یک جلسه تمرکز ثبت شد. آفرین!")
            _focus.value = s.copy(
                phase = FocusPhase.BREAK, running = false, endAtMillis = 0L, remainingMs = s.breakMin * 60_000L
            )
        } else {
            _focus.value = idleFocus(s)
        }
    }

    // ---------------- journal ----------------

    private val _journalLock = MutableStateFlow(prefs.journalLock)
    val journalLock: StateFlow<Boolean> = _journalLock.asStateFlow()
    private val _journalUnlocked = MutableStateFlow(false)
    val journalUnlocked: StateFlow<Boolean> = _journalUnlocked.asStateFlow()

    fun setJournalLock(on: Boolean) {
        prefs.journalLock = on
        _journalLock.value = on
        _journalUnlocked.value = true // the person is here right now; the lock applies from the next visit
    }

    fun unlockJournal() {
        _journalUnlocked.value = true
    }

    fun lockJournal() {
        _journalUnlocked.value = false
    }

    /** Called after "delete everything": forgets the lock, the server address and the chat. */
    fun resetPrefs() {
        prefs.clear()
        _journalLock.value = false
        _journalUnlocked.value = true
        _aiUrl.value = ""
        _aiToken.value = ""
        clearChat()
        resetFocus()
    }

    fun saveNote(n: NoteItem) {
        viewModelScope.launch {
            if (n.id == 0) {
                dao.insertNote(n.copy(createdAt = System.currentTimeMillis(), epochDay = todayEpochDay()))
            } else {
                dao.updateNote(n)
            }
        }
    }

    fun deleteNote(n: NoteItem) {
        viewModelScope.launch { dao.deleteNote(n) }
    }

    // ---------------- RiseAI ----------------

    private val _aiUrl = MutableStateFlow(prefs.aiUrl)
    val aiUrl: StateFlow<String> = _aiUrl.asStateFlow()
    private val _aiToken = MutableStateFlow(prefs.aiToken)
    val aiToken: StateFlow<String> = _aiToken.asStateFlow()

    private val _chat = MutableStateFlow(
        listOf(ChatMsg(false, "سلام! من RiseAI هستم، دستیار فارسی RiseFit. بنویس «راهنما» تا ببینی چه کارهایی بلدم."))
    )
    val chat: StateFlow<List<ChatMsg>> = _chat.asStateFlow()
    private val _thinking = MutableStateFlow(false)
    val thinking: StateFlow<Boolean> = _thinking.asStateFlow()
    private val aiHistory = ArrayList<Pair<String, String>>()

    fun saveAiSettings(url: String, token: String) {
        val u = url.trim()
        if (u.isNotEmpty() && !u.startsWith("https://")) {
            say("آدرس باید با https:// شروع شود")
            return
        }
        prefs.aiUrl = u
        prefs.aiToken = token
        _aiUrl.value = prefs.aiUrl
        _aiToken.value = prefs.aiToken
        say(if (u.isEmpty()) "RiseAI فقط به‌صورت آفلاین کار می‌کند" else "آدرس سرور RiseAI ذخیره شد")
    }

    fun clearChat() {
        aiHistory.clear()
        _chat.value = _chat.value.take(1)
    }

    private fun push(m: ChatMsg) {
        _chat.value = _chat.value + m
    }

    fun ask(text: String) {
        val q = text.trim()
        if (q.isEmpty() || _thinking.value) return
        push(ChatMsg(true, q.take(500)))
        viewModelScope.launch {
            when (val intent = LocalAssistant.parse(q)) {
                AiIntent.Unknown -> askServer(q.take(500))
                else -> push(ChatMsg(false, runLocal(intent)))
            }
        }
    }

    private suspend fun askServer(q: String) {
        val url = _aiUrl.value
        if (url.isBlank()) {
            push(
                ChatMsg(
                    false,
                    "این سؤال را بدون اینترنت نمی‌توانم جواب بدهم. اگر سرور RiseAI داری، آدرسش را از آیکن چرخ‌دنده بالای همین صفحه وارد کن. " +
                        "در این حالت فقط همین سؤال‌های آزاد به سرور می‌روند و برنامه و دفترچه‌ات هرگز فرستاده نمی‌شوند. بنویس «راهنما» برای دیدن کارهای آفلاین."
                )
            )
            return
        }
        _thinking.value = true
        aiHistory.add("user" to q)
        try {
            var h = aiHistory.takeLast(10)
            while (h.isNotEmpty() && h.first().first != "user") h = h.drop(1)
            val reply = AiClient.ask(url, _aiToken.value, h)
            aiHistory.add("assistant" to reply)
            while (aiHistory.size > 20) aiHistory.removeAt(0)
            push(ChatMsg(false, reply))
        } catch (e: CancellationException) {
            throw e
        } catch (e: AiClient.AiException) {
            aiHistory.removeAt(aiHistory.lastIndex)
            push(ChatMsg(false, "اتصال به RiseAI انجام نشد: ${e.message}"))
        } catch (e: Exception) {
            aiHistory.removeAt(aiHistory.lastIndex)
            push(ChatMsg(false, "اتصال به RiseAI برقرار نشد. اینترنت و آدرس سرور را بررسی کن."))
        } finally {
            _thinking.value = false
        }
    }

    private fun dayLabel(epochDay: Long): String = Jalali.formatWithWeekday(LocalDate.ofEpochDay(epochDay))

    private suspend fun runLocal(intent: AiIntent): String {
        val today = todayEpochDay()
        return when (intent) {
            AiIntent.Help -> LocalAssistant.HELP_TEXT
            AiIntent.Greeting -> "سلام! امروز چه کاری از دستم برمیاد؟ «راهنما» را بنویس تا کارهایی که بلدم را ببینی."
            AiIntent.Motivation -> "${Quotes.today()}\n${Quotes.MAIN_SENTENCE}"
            AiIntent.Today -> {
                val list = tasks.value.filter { it.epochDay == today }
                if (list.isEmpty()) {
                    "برای امروز کاری در پلنر ثبت نشده. مثلاً بنویس «درس ریاضی ساعت ۱۶ اضافه کن»."
                } else {
                    val done = list.count { it.done }
                    val lines = list.joinToString("\n") {
                        "• ${if (it.done) "✓" else "○"} ${PlannerStats.time(it.startMin)} ${it.title}"
                    }
                    "امروز ${list.size.fa()} کار داری و ${done.fa()} تا انجام شده (${PlannerStats.percent(list).fa()}٪):\n$lines"
                }
            }
            AiIntent.Habits -> {
                val hs = habits.value
                if (hs.isEmpty()) {
                    "هنوز عادتی اضافه نکرده‌ای. از بخش «عادت‌ها» شروع کن."
                } else {
                    hs.joinToString("\n") { h ->
                        val days = habitLogs.value.filter { it.habitId == h.id }.map { it.epochDay }.toSet()
                        val streak = HabitStats.streak(days, today)
                        "• ${if (today in days) "✓" else "○"} ${h.name} — ${streak.fa()} روز پشت‌سرهم"
                    }
                }
            }
            AiIntent.Focus -> {
                val todays = focusSessions.value.filter { it.epochDay == today }
                if (todays.isEmpty()) {
                    "امروز هنوز جلسه تمرکزی نداشته‌ای. از بخش «تمرکز» یک جلسه ۲۵ دقیقه‌ای شروع کن."
                } else {
                    "امروز ${todays.size.fa()} جلسه و ${todays.sumOf { it.minutes }.fa()} دقیقه تمرکز داشتی. عالیه!"
                }
            }
            AiIntent.Goals -> {
                val active = goals.value.filter { it.status != "DONE" }
                if (active.isEmpty()) {
                    "هدف فعالی نداری. از بخش «هدف‌ها» یک هدف ۳۰، ۶۰ یا ۱۰۰ روزه بساز."
                } else {
                    active.joinToString("\n") { g ->
                        val p = GoalStats.percent(g, steps.value.filter { it.goalId == g.id })
                        "• ${g.title} — ${p.fa()}٪، ${GoalStats.daysLeft(g, today).coerceAtLeast(0).fa()} روز مانده"
                    }
                }
            }
            is AiIntent.AddTask -> {
                if (intent.title.isBlank()) {
                    "اسم کار را هم بنویس؛ مثلاً «مرور زیست ساعت ۱۸ اضافه کن»."
                } else {
                    val day = if (intent.tomorrow) today + 1 else today
                    val start = if (intent.startMin >= 0) intent.startMin
                    else (LocalTime.now().hour + 1).coerceAtMost(22) * 60
                    val end = (start + 60).coerceAtMost(23 * 60 + 59)
                    dao.insertTask(TaskItem(epochDay = day, title = intent.title, startMin = start, endMin = end))
                    val note = if (intent.startMin < 0) " (ساعت را نگفتی، یک ساعتی گذاشتم؛ از پلنر عوضش کن)" else ""
                    "«${intent.title}» برای ${dayLabel(day)} ساعت ${PlannerStats.time(start)} به پلنر اضافه شد.$note"
                }
            }
            is AiIntent.AddReminder -> {
                when {
                    intent.hour < 0 -> "ساعت چند یادآوری کنم؟ مثلاً بنویس «یادم بنداز آب بخورم ساعت ۱۵»."
                    else -> {
                        val title = intent.title.ifBlank { "یادآوری" }
                        val day = if (intent.tomorrow) today + 1
                        else nextOccurrence(intent.hour, intent.minute, 0b1111111)?.toLocalDate()?.toEpochDay() ?: today
                        val r = CustomReminder(
                            title = title, hour = intent.hour, minute = intent.minute,
                            daysMask = 0, enabled = true, onceEpochDay = day
                        )
                        val id = dao.insertReminder(r).toInt()
                        ReminderScheduler.scheduleCustom(app, r.copy(id = id))
                        "باشه! ${dayLabel(day)} ساعت ${formatTime(intent.hour, intent.minute)} یادآوری می‌کنم: «$title»."
                    }
                }
            }
            AiIntent.Unknown -> ""
        }
    }
}
