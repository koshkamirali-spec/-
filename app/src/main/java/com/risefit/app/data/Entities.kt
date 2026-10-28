package com.risefit.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** daysMask: bit i (0..6) = DayOfWeek.value - 1 (Monday = 0 ... Sunday = 6). */

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int = 6,
    val minute: Int = 0,
    val daysMask: Int = 0b1111111,
    val label: String = "",
    val enabled: Boolean = true,
    val vibrate: Boolean = true,
    val snoozeMinutes: Int = 5,
    /** null = default system alarm sound */
    val soundUri: String? = null
)

@Entity(tableName = "schedule_items")
data class ScheduleItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val hour: Int,
    val minute: Int,
    /** WAKE, SCHOOL, GYM, HOME, FUTSAL, MEAL, SLEEP, OTHER */
    val category: String = "OTHER",
    val daysMask: Int = 0b1111111,
    val enabled: Boolean = true
)

@Entity(tableName = "gym_exercises")
data class GymExercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    /** DayOfWeek.value - 1 (0..6) */
    val dayIndex: Int,
    val name: String,
    val sets: Int = 3,
    val reps: Int = 10,
    val weightKg: Float = 0f,
    val restSec: Int = 90,
    val guide: String = "",
    val mediaUri: String? = null
)

@Entity(tableName = "home_exercises")
data class HomeExercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    /** ABS or BODY */
    val group: String,
    val name: String,
    val howTo: String,
    val reps: Int = 0,
    val durationSec: Int = 0,
    val restSec: Int = 45
)

@Entity(tableName = "home_done", primaryKeys = ["epochDay", "exerciseId"])
data class HomeDone(
    val epochDay: Long,
    val exerciseId: Int
)

@Entity(tableName = "futsal_plans")
data class FutsalPlan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dayIndex: Int,
    val hour: Int,
    val minute: Int,
    val durationMin: Int = 60
)

@Entity(tableName = "workout_logs")
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val epochDay: Long,
    /** GYM, ABS, HOME, FUTSAL */
    val type: String,
    val title: String = "",
    val durationMin: Int = 0,
    val notes: String = "",
    val points: Int = 10
)

@Entity(tableName = "goal_stages")
data class GoalStage(
    /** 0 = main target image, 30 / 60 / 100 = milestone days */
    @PrimaryKey val stage: Int,
    val photoUri: String? = null,
    val goalText: String = "",
    val note: String = "",
    val progress: Int = 0
)
