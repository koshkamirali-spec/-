package com.risefit.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Small local settings for the plus features (journal lock, RiseAI server address). */
class PlusPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("risefit_plus", Context.MODE_PRIVATE)

    var journalLock: Boolean
        get() = prefs.getBoolean("journalLock", false)
        set(v) {
            prefs.edit().putBoolean("journalLock", v).apply()
        }

    /** Address of the user's own RiseAI intermediary server (https). Empty = offline assistant only. */
    var aiUrl: String
        get() = prefs.getString("aiUrl", "") ?: ""
        set(v) {
            prefs.edit().putString("aiUrl", v.trim()).apply()
        }

    /** Optional shared password that the intermediary server checks. It is NOT the AI key. */
    var aiToken: String
        get() = prefs.getString("aiToken", "") ?: ""
        set(v) {
            prefs.edit().putString("aiToken", v.trim()).apply()
        }

    fun clear() {
        prefs.edit().clear().apply()
    }
}

class PlusRepository(context: Context) {

    private val app = context.applicationContext
    val dao: PlusDao = PlusDatabase.get(app).dao()

    val tasks: Flow<List<TaskItem>> = dao.observeTasks()
    val habits: Flow<List<Habit>> = dao.observeHabits()
    val habitLogs: Flow<List<HabitLog>> = dao.observeHabitLogs()
    val goals: Flow<List<GoalItem>> = dao.observeGoals()
    val steps: Flow<List<GoalStep>> = dao.observeSteps()
    val focus: Flow<List<FocusSession>> = dao.observeFocus()
    val reminders: Flow<List<CustomReminder>> = dao.observeReminders()
    val notes: Flow<List<NoteItem>> = dao.observeNotes()

    suspend fun wipeAll() = withContext(Dispatchers.IO) {
        dao.clearTasks()
        dao.clearHabits()
        dao.clearHabitLogs()
        dao.clearGoals()
        dao.clearSteps()
        dao.clearFocus()
        dao.clearReminders()
        dao.clearNotes()
    }

    // ---------- backup ----------

    suspend fun exportJson(): JSONObject = withContext(Dispatchers.IO) {
        val root = JSONObject()

        val tasks = JSONArray()
        for (t in dao.allTasks()) {
            tasks.put(
                JSONObject().put("id", t.id).put("epochDay", t.epochDay).put("title", t.title)
                    .put("startMin", t.startMin).put("endMin", t.endMin)
                    .put("category", t.category).put("done", t.done)
            )
        }
        root.put("tasks", tasks)

        val habits = JSONArray()
        for (h in dao.allHabits()) habits.put(JSONObject().put("id", h.id).put("name", h.name))
        root.put("habits", habits)

        val logs = JSONArray()
        for (l in dao.allHabitLogs()) logs.put(JSONObject().put("habitId", l.habitId).put("epochDay", l.epochDay))
        root.put("habitLogs", logs)

        val goals = JSONArray()
        for (g in dao.allGoals()) {
            goals.put(
                JSONObject().put("id", g.id).put("title", g.title)
                    .put("startEpochDay", g.startEpochDay).put("deadlineEpochDay", g.deadlineEpochDay)
                    .put("progress", g.progress).put("status", g.status)
            )
        }
        root.put("goals", goals)

        val steps = JSONArray()
        for (s in dao.allSteps()) {
            steps.put(JSONObject().put("id", s.id).put("goalId", s.goalId).put("text", s.text).put("done", s.done))
        }
        root.put("steps", steps)

        val focus = JSONArray()
        for (f in dao.allFocus()) {
            focus.put(
                JSONObject().put("id", f.id).put("epochDay", f.epochDay)
                    .put("minutes", f.minutes).put("label", f.label)
            )
        }
        root.put("focus", focus)

        val reminders = JSONArray()
        for (r in dao.allReminders()) {
            reminders.put(
                JSONObject().put("id", r.id).put("title", r.title).put("hour", r.hour).put("minute", r.minute)
                    .put("daysMask", r.daysMask).put("enabled", r.enabled).put("onceEpochDay", r.onceEpochDay)
            )
        }
        root.put("reminders", reminders)

        val notes = JSONArray()
        for (n in dao.allNotes()) {
            notes.put(
                JSONObject().put("id", n.id).put("epochDay", n.epochDay).put("createdAt", n.createdAt)
                    .put("title", n.title).put("body", n.body).put("mood", n.mood)
            )
        }
        root.put("notes", notes)
        root
    }

    /** Replaces the plus data with the backup content. Everything is parsed before anything is deleted. */
    suspend fun importJson(root: JSONObject) = withContext(Dispatchers.IO) {
        val tasks = ArrayList<TaskItem>()
        root.optJSONArray("tasks")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                tasks.add(
                    TaskItem(
                        id = o.getInt("id"), epochDay = o.getLong("epochDay"), title = o.getString("title"),
                        startMin = o.getInt("startMin"), endMin = o.getInt("endMin"),
                        category = o.optString("category", "PERSONAL"), done = o.optBoolean("done", false)
                    )
                )
            }
        }
        val habits = ArrayList<Habit>()
        root.optJSONArray("habits")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                habits.add(Habit(id = o.getInt("id"), name = o.getString("name")))
            }
        }
        val logs = ArrayList<HabitLog>()
        root.optJSONArray("habitLogs")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                logs.add(HabitLog(habitId = o.getInt("habitId"), epochDay = o.getLong("epochDay")))
            }
        }
        val goals = ArrayList<GoalItem>()
        root.optJSONArray("goals")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                goals.add(
                    GoalItem(
                        id = o.getInt("id"), title = o.getString("title"),
                        startEpochDay = o.getLong("startEpochDay"), deadlineEpochDay = o.getLong("deadlineEpochDay"),
                        progress = o.optInt("progress", 0), status = o.optString("status", "ACTIVE")
                    )
                )
            }
        }
        val steps = ArrayList<GoalStep>()
        root.optJSONArray("steps")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                steps.add(
                    GoalStep(
                        id = o.getInt("id"), goalId = o.getInt("goalId"),
                        text = o.getString("text"), done = o.optBoolean("done", false)
                    )
                )
            }
        }
        val focus = ArrayList<FocusSession>()
        root.optJSONArray("focus")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                focus.add(
                    FocusSession(
                        id = o.getInt("id"), epochDay = o.getLong("epochDay"),
                        minutes = o.getInt("minutes"), label = o.optString("label", "")
                    )
                )
            }
        }
        val reminders = ArrayList<CustomReminder>()
        root.optJSONArray("reminders")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                reminders.add(
                    CustomReminder(
                        id = o.getInt("id"), title = o.getString("title"),
                        hour = o.getInt("hour"), minute = o.getInt("minute"),
                        daysMask = o.optInt("daysMask", 0b1111111), enabled = o.optBoolean("enabled", true),
                        onceEpochDay = o.optLong("onceEpochDay", -1)
                    )
                )
            }
        }
        val notes = ArrayList<NoteItem>()
        root.optJSONArray("notes")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                notes.add(
                    NoteItem(
                        id = o.getInt("id"), epochDay = o.getLong("epochDay"), createdAt = o.getLong("createdAt"),
                        title = o.optString("title", ""), body = o.optString("body", ""),
                        mood = o.optString("mood", "")
                    )
                )
            }
        }

        dao.clearTasks(); dao.clearHabits(); dao.clearHabitLogs(); dao.clearGoals()
        dao.clearSteps(); dao.clearFocus(); dao.clearReminders(); dao.clearNotes()
        tasks.forEach { dao.insertTask(it) }
        habits.forEach { dao.insertHabit(it) }
        logs.forEach { dao.insertHabitLog(it) }
        goals.forEach { dao.insertGoal(it) }
        steps.forEach { dao.insertStep(it) }
        focus.forEach { dao.insertFocus(it) }
        reminders.forEach { dao.insertReminder(it) }
        notes.forEach { dao.insertNote(it) }
    }
}
