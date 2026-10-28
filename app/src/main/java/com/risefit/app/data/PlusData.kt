package com.risefit.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// The "plus" features live in their own small database so the original data is never touched.

/** Daily planner entry. Times are minutes since midnight. */
@Entity(tableName = "plus_tasks")
data class TaskItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val epochDay: Long,
    val title: String,
    val startMin: Int,
    val endMin: Int,
    /** STUDY, SCHOOL, GYM, FUTSAL, REST, PERSONAL */
    val category: String = "PERSONAL",
    val done: Boolean = false
)

@Entity(tableName = "plus_habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

@Entity(tableName = "plus_habit_logs", primaryKeys = ["habitId", "epochDay"])
data class HabitLog(
    val habitId: Int,
    val epochDay: Long
)

@Entity(tableName = "plus_goals")
data class GoalItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val startEpochDay: Long,
    val deadlineEpochDay: Long,
    /** manual progress, used when the goal has no steps */
    val progress: Int = 0,
    /** ACTIVE, PAUSED, DONE */
    val status: String = "ACTIVE"
)

@Entity(tableName = "plus_goal_steps")
data class GoalStep(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val goalId: Int,
    val text: String,
    val done: Boolean = false
)

@Entity(tableName = "plus_focus")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val epochDay: Long,
    val minutes: Int,
    val label: String = ""
)

/** daysMask = weekly repeat; onceEpochDay >= 0 means a one-time reminder on that date. */
@Entity(tableName = "plus_reminders")
data class CustomReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val hour: Int,
    val minute: Int,
    val daysMask: Int = 0b1111111,
    val enabled: Boolean = true,
    val onceEpochDay: Long = -1
)

@Entity(tableName = "plus_notes")
data class NoteItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val epochDay: Long,
    val createdAt: Long,
    val title: String,
    val body: String,
    val mood: String = ""
)

@Dao
interface PlusDao {
    // ---- tasks ----
    @Query("SELECT * FROM plus_tasks ORDER BY epochDay, startMin")
    fun observeTasks(): Flow<List<TaskItem>>

    @Query("SELECT * FROM plus_tasks WHERE epochDay = :day ORDER BY startMin")
    suspend fun tasksOn(day: Long): List<TaskItem>

    @Query("SELECT * FROM plus_tasks")
    suspend fun allTasks(): List<TaskItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(t: TaskItem): Long

    @Update
    suspend fun updateTask(t: TaskItem)

    @Delete
    suspend fun deleteTask(t: TaskItem)

    // ---- habits ----
    @Query("SELECT * FROM plus_habits ORDER BY id")
    fun observeHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM plus_habits")
    suspend fun allHabits(): List<Habit>

    @Query("SELECT * FROM plus_habit_logs")
    fun observeHabitLogs(): Flow<List<HabitLog>>

    @Query("SELECT * FROM plus_habit_logs")
    suspend fun allHabitLogs(): List<HabitLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(h: Habit): Long

    @Update
    suspend fun updateHabit(h: Habit)

    @Delete
    suspend fun deleteHabit(h: Habit)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitLog(l: HabitLog)

    @Query("DELETE FROM plus_habit_logs WHERE habitId = :habitId AND epochDay = :day")
    suspend fun deleteHabitLog(habitId: Int, day: Long)

    @Query("DELETE FROM plus_habit_logs WHERE habitId = :habitId")
    suspend fun deleteLogsOfHabit(habitId: Int)

    // ---- goals ----
    @Query("SELECT * FROM plus_goals ORDER BY id DESC")
    fun observeGoals(): Flow<List<GoalItem>>

    @Query("SELECT * FROM plus_goals")
    suspend fun allGoals(): List<GoalItem>

    @Query("SELECT * FROM plus_goal_steps ORDER BY id")
    fun observeSteps(): Flow<List<GoalStep>>

    @Query("SELECT * FROM plus_goal_steps")
    suspend fun allSteps(): List<GoalStep>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(g: GoalItem): Long

    @Update
    suspend fun updateGoal(g: GoalItem)

    @Delete
    suspend fun deleteGoal(g: GoalItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(s: GoalStep): Long

    @Update
    suspend fun updateStep(s: GoalStep)

    @Delete
    suspend fun deleteStep(s: GoalStep)

    @Query("DELETE FROM plus_goal_steps WHERE goalId = :goalId")
    suspend fun deleteStepsOfGoal(goalId: Int)

    // ---- focus ----
    @Query("SELECT * FROM plus_focus ORDER BY id DESC")
    fun observeFocus(): Flow<List<FocusSession>>

    @Query("SELECT * FROM plus_focus")
    suspend fun allFocus(): List<FocusSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocus(f: FocusSession): Long

    @Delete
    suspend fun deleteFocus(f: FocusSession)

    // ---- reminders ----
    @Query("SELECT * FROM plus_reminders ORDER BY hour, minute")
    fun observeReminders(): Flow<List<CustomReminder>>

    @Query("SELECT * FROM plus_reminders")
    suspend fun allReminders(): List<CustomReminder>

    @Query("SELECT * FROM plus_reminders WHERE id = :id")
    suspend fun reminderById(id: Int): CustomReminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(r: CustomReminder): Long

    @Update
    suspend fun updateReminder(r: CustomReminder)

    @Delete
    suspend fun deleteReminder(r: CustomReminder)

    // ---- notes ----
    @Query("SELECT * FROM plus_notes ORDER BY createdAt DESC")
    fun observeNotes(): Flow<List<NoteItem>>

    @Query("SELECT * FROM plus_notes")
    suspend fun allNotes(): List<NoteItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(n: NoteItem): Long

    @Update
    suspend fun updateNote(n: NoteItem)

    @Delete
    suspend fun deleteNote(n: NoteItem)

    // ---- wipe ----
    @Query("DELETE FROM plus_tasks")
    suspend fun clearTasks()

    @Query("DELETE FROM plus_habits")
    suspend fun clearHabits()

    @Query("DELETE FROM plus_habit_logs")
    suspend fun clearHabitLogs()

    @Query("DELETE FROM plus_goals")
    suspend fun clearGoals()

    @Query("DELETE FROM plus_goal_steps")
    suspend fun clearSteps()

    @Query("DELETE FROM plus_focus")
    suspend fun clearFocus()

    @Query("DELETE FROM plus_reminders")
    suspend fun clearReminders()

    @Query("DELETE FROM plus_notes")
    suspend fun clearNotes()
}

@Database(
    entities = [
        TaskItem::class, Habit::class, HabitLog::class, GoalItem::class, GoalStep::class,
        FocusSession::class, CustomReminder::class, NoteItem::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PlusDatabase : RoomDatabase() {
    abstract fun dao(): PlusDao

    companion object {
        @Volatile
        private var instance: PlusDatabase? = null

        fun get(context: Context): PlusDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PlusDatabase::class.java,
                    "risefit_plus.db"
                ).build().also { instance = it }
            }
    }
}

object TaskCats {
    val all = listOf("STUDY", "SCHOOL", "GYM", "FUTSAL", "REST", "PERSONAL")

    fun label(c: String): String = when (c) {
        "STUDY" -> "درس"
        "SCHOOL" -> "مدرسه"
        "GYM" -> "بدنسازی"
        "FUTSAL" -> "فوتسال"
        "REST" -> "استراحت"
        else -> "شخصی"
    }
}
