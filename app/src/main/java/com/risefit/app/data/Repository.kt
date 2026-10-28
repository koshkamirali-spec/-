package com.risefit.app.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class Repository(context: Context) {

    private val app = context.applicationContext
    private val db = AppDatabase.get(app)
    private val meta = app.getSharedPreferences("risefit_meta", Context.MODE_PRIVATE)

    val alarmDao = db.alarmDao()
    val scheduleDao = db.scheduleDao()
    val gymDao = db.gymDao()
    val homeDao = db.homeDao()
    val futsalDao = db.futsalDao()
    val logDao = db.logDao()
    val goalDao = db.goalDao()

    val alarms: Flow<List<AlarmEntity>> = alarmDao.observeAll()
    val schedule: Flow<List<ScheduleItem>> = scheduleDao.observeAll()
    val gym: Flow<List<GymExercise>> = gymDao.observeAll()
    val home: Flow<List<HomeExercise>> = homeDao.observeAll()
    val futsal: Flow<List<FutsalPlan>> = futsalDao.observeAll()
    val logs: Flow<List<WorkoutLog>> = logDao.observeAll()
    val goals: Flow<List<GoalStage>> = goalDao.observeAll()

    val homeDone: Flow<List<HomeDone>> = homeDao.observeAllDone()

    /** Creates the starter data once, on the first launch only. */
    suspend fun seedIfNeeded() = withContext(Dispatchers.IO) {
        if (meta.getBoolean("seeded", false)) return@withContext
        if (scheduleDao.count() == 0) Seed.schedule().forEach { scheduleDao.insert(it) }
        if (homeDao.count() == 0) Seed.homeExercises().forEach { homeDao.insert(it) }
        if (gymDao.getAll().isEmpty()) Seed.gymExercises().forEach { gymDao.insert(it) }
        meta.edit().putBoolean("seeded", true).apply()
    }

    // ---------- photos ----------

    private fun photoDir(): File = File(app.filesDir, "photos").apply { mkdirs() }

    /** Copies the picked photo byte-for-byte into private app storage. The original is never touched. */
    suspend fun importPhoto(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val out = File(photoDir(), "p_${System.currentTimeMillis()}.img")
            val input = app.contentResolver.openInputStream(uri) ?: return@withContext null
            input.use { ins -> out.outputStream().use { ins.copyTo(it) } }
            out.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun deletePhotoFile(path: String?) {
        if (path.isNullOrBlank()) return
        if (path.startsWith("content://")) return
        try {
            val f = File(path)
            if (f.exists() && f.parentFile?.absolutePath == photoDir().absolutePath) f.delete()
        } catch (_: Exception) {
        }
    }

    // ---------- delete all ----------

    suspend fun wipeEverything() = withContext(Dispatchers.IO) {
        alarmDao.clear()
        scheduleDao.clear()
        gymDao.clear()
        homeDao.clearDone()
        homeDao.clear()
        futsalDao.clear()
        logDao.clear()
        goalDao.clear()
        photoDir().listFiles()?.forEach { it.delete() }
        meta.edit().putBoolean("seeded", true).apply() // do not re-create starter data after a wipe
    }

    suspend fun wipePhotosAndGoals() = withContext(Dispatchers.IO) {
        goalDao.clear()
        photoDir().listFiles()?.forEach { it.delete() }
    }

    // ---------- backup / restore ----------

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key) || !has(key)) null else getString(key)

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "RiseFit")
        root.put("version", 1)

        val alarmArr = JSONArray()
        for (a in alarmDao.getAll()) {
            alarmArr.put(
                JSONObject()
                    .put("id", a.id).put("hour", a.hour).put("minute", a.minute)
                    .put("daysMask", a.daysMask).put("label", a.label)
                    .put("enabled", a.enabled).put("vibrate", a.vibrate)
                    .put("snoozeMinutes", a.snoozeMinutes)
                    .put("soundUri", a.soundUri ?: JSONObject.NULL)
            )
        }
        root.put("alarms", alarmArr)

        val schedArr = JSONArray()
        for (s in scheduleDao.getAll()) {
            schedArr.put(
                JSONObject()
                    .put("id", s.id).put("title", s.title).put("hour", s.hour).put("minute", s.minute)
                    .put("category", s.category).put("daysMask", s.daysMask).put("enabled", s.enabled)
            )
        }
        root.put("schedule", schedArr)

        val gymArr = JSONArray()
        for (g in gymDao.getAll()) {
            gymArr.put(
                JSONObject()
                    .put("id", g.id).put("dayIndex", g.dayIndex).put("name", g.name)
                    .put("sets", g.sets).put("reps", g.reps).put("weightKg", g.weightKg.toDouble())
                    .put("restSec", g.restSec).put("guide", g.guide)
                    .put("mediaUri", g.mediaUri ?: JSONObject.NULL)
            )
        }
        root.put("gym", gymArr)

        val homeArr = JSONArray()
        for (h in homeDao.getAll()) {
            homeArr.put(
                JSONObject()
                    .put("id", h.id).put("group", h.group).put("name", h.name).put("howTo", h.howTo)
                    .put("reps", h.reps).put("durationSec", h.durationSec).put("restSec", h.restSec)
            )
        }
        root.put("home", homeArr)

        val doneArr = JSONArray()
        for (d in homeDao.getAllDone()) {
            doneArr.put(JSONObject().put("epochDay", d.epochDay).put("exerciseId", d.exerciseId))
        }
        root.put("homeDone", doneArr)

        val futsalArr = JSONArray()
        for (f in futsalDao.getAll()) {
            futsalArr.put(
                JSONObject()
                    .put("id", f.id).put("dayIndex", f.dayIndex).put("hour", f.hour)
                    .put("minute", f.minute).put("durationMin", f.durationMin)
            )
        }
        root.put("futsal", futsalArr)

        val logArr = JSONArray()
        for (l in logDao.getAll()) {
            logArr.put(
                JSONObject()
                    .put("id", l.id).put("epochDay", l.epochDay).put("type", l.type).put("title", l.title)
                    .put("durationMin", l.durationMin).put("notes", l.notes).put("points", l.points)
            )
        }
        root.put("logs", logArr)

        val goalArr = JSONArray()
        for (g in goalDao.getAll()) {
            var b64: Any? = JSONObject.NULL
            val p = g.photoUri
            if (p != null && !p.startsWith("content://")) {
                try {
                    val f = File(p)
                    if (f.exists()) b64 = Base64.encodeToString(f.readBytes(), Base64.NO_WRAP)
                } catch (_: Exception) {
                }
            }
            goalArr.put(
                JSONObject()
                    .put("stage", g.stage).put("goalText", g.goalText).put("note", g.note)
                    .put("progress", g.progress).put("photoB64", b64 ?: JSONObject.NULL)
            )
        }
        root.put("goals", goalArr)
        root.toString()
    }

    /** Replaces all current data with the backup. Throws if the file is not a RiseFit backup. */
    suspend fun importJson(text: String) = withContext(Dispatchers.IO) {
        val root = JSONObject(text)
        require(root.optString("app") == "RiseFit") { "not a RiseFit backup" }

        // parse everything first so a broken file never leaves us with half-wiped data
        val alarmList = ArrayList<AlarmEntity>()
        root.optJSONArray("alarms")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                alarmList.add(
                    AlarmEntity(
                        id = o.getInt("id"), hour = o.getInt("hour"), minute = o.getInt("minute"),
                        daysMask = o.getInt("daysMask"), label = o.optString("label", ""),
                        enabled = o.optBoolean("enabled", true), vibrate = o.optBoolean("vibrate", true),
                        snoozeMinutes = o.optInt("snoozeMinutes", 5), soundUri = o.optStringOrNull("soundUri")
                    )
                )
            }
        }
        val schedList = ArrayList<ScheduleItem>()
        root.optJSONArray("schedule")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                schedList.add(
                    ScheduleItem(
                        id = o.getInt("id"), title = o.getString("title"), hour = o.getInt("hour"),
                        minute = o.getInt("minute"), category = o.optString("category", "OTHER"),
                        daysMask = o.optInt("daysMask", 0b1111111), enabled = o.optBoolean("enabled", true)
                    )
                )
            }
        }
        val gymList = ArrayList<GymExercise>()
        root.optJSONArray("gym")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                gymList.add(
                    GymExercise(
                        id = o.getInt("id"), dayIndex = o.getInt("dayIndex"), name = o.getString("name"),
                        sets = o.optInt("sets", 3), reps = o.optInt("reps", 10),
                        weightKg = o.optDouble("weightKg", 0.0).toFloat(), restSec = o.optInt("restSec", 90),
                        guide = o.optString("guide", ""), mediaUri = o.optStringOrNull("mediaUri")
                    )
                )
            }
        }
        val homeList = ArrayList<HomeExercise>()
        root.optJSONArray("home")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                homeList.add(
                    HomeExercise(
                        id = o.getInt("id"), group = o.getString("group"), name = o.getString("name"),
                        howTo = o.optString("howTo", ""), reps = o.optInt("reps", 0),
                        durationSec = o.optInt("durationSec", 0), restSec = o.optInt("restSec", 45)
                    )
                )
            }
        }
        val doneList = ArrayList<HomeDone>()
        root.optJSONArray("homeDone")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                doneList.add(HomeDone(epochDay = o.getLong("epochDay"), exerciseId = o.getInt("exerciseId")))
            }
        }
        val futsalList = ArrayList<FutsalPlan>()
        root.optJSONArray("futsal")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                futsalList.add(
                    FutsalPlan(
                        id = o.getInt("id"), dayIndex = o.getInt("dayIndex"), hour = o.getInt("hour"),
                        minute = o.getInt("minute"), durationMin = o.optInt("durationMin", 60)
                    )
                )
            }
        }
        val logList = ArrayList<WorkoutLog>()
        root.optJSONArray("logs")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                logList.add(
                    WorkoutLog(
                        id = o.getInt("id"), epochDay = o.getLong("epochDay"), type = o.getString("type"),
                        title = o.optString("title", ""), durationMin = o.optInt("durationMin", 0),
                        notes = o.optString("notes", ""), points = o.optInt("points", 10)
                    )
                )
            }
        }
        val goalList = ArrayList<GoalStage>()
        root.optJSONArray("goals")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                var path: String? = null
                val b64 = o.optStringOrNull("photoB64")
                if (b64 != null) {
                    try {
                        val f = File(photoDir(), "r_${o.getInt("stage")}_${System.currentTimeMillis()}.img")
                        f.writeBytes(Base64.decode(b64, Base64.NO_WRAP))
                        path = f.absolutePath
                    } catch (_: Exception) {
                    }
                }
                goalList.add(
                    GoalStage(
                        stage = o.getInt("stage"), photoUri = path, goalText = o.optString("goalText", ""),
                        note = o.optString("note", ""), progress = o.optInt("progress", 0)
                    )
                )
            }
        }

        // replace data
        val oldPhotos = photoDir().listFiles()?.map { it.absolutePath }?.toSet() ?: emptySet()
        val keep = goalList.mapNotNull { it.photoUri }.toSet()
        alarmDao.clear(); scheduleDao.clear(); gymDao.clear(); homeDao.clearDone(); homeDao.clear()
        futsalDao.clear(); logDao.clear(); goalDao.clear()
        alarmList.forEach { alarmDao.insert(it) }
        schedList.forEach { scheduleDao.insert(it) }
        gymList.forEach { gymDao.insert(it) }
        homeList.forEach { homeDao.insert(it) }
        doneList.forEach { homeDao.markDone(it) }
        futsalList.forEach { futsalDao.insert(it) }
        logList.forEach { logDao.insert(it) }
        goalList.forEach { goalDao.upsert(it) }
        oldPhotos.filter { it !in keep }.forEach { File(it).delete() }
        meta.edit().putBoolean("seeded", true).apply()
    }
}
