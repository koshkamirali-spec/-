package com.risefit.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    suspend fun getAll(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getById(id: Int): AlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: AlarmEntity): Long

    @Update
    suspend fun update(alarm: AlarmEntity)

    @Delete
    suspend fun delete(alarm: AlarmEntity)

    @Query("DELETE FROM alarms")
    suspend fun clear()
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_items ORDER BY hour, minute")
    fun observeAll(): Flow<List<ScheduleItem>>

    @Query("SELECT * FROM schedule_items ORDER BY hour, minute")
    suspend fun getAll(): List<ScheduleItem>

    @Query("SELECT COUNT(*) FROM schedule_items")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ScheduleItem): Long

    @Update
    suspend fun update(item: ScheduleItem)

    @Delete
    suspend fun delete(item: ScheduleItem)

    @Query("DELETE FROM schedule_items")
    suspend fun clear()
}

@Dao
interface GymDao {
    @Query("SELECT * FROM gym_exercises ORDER BY dayIndex, id")
    fun observeAll(): Flow<List<GymExercise>>

    @Query("SELECT * FROM gym_exercises ORDER BY dayIndex, id")
    suspend fun getAll(): List<GymExercise>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ex: GymExercise): Long

    @Update
    suspend fun update(ex: GymExercise)

    @Delete
    suspend fun delete(ex: GymExercise)

    @Query("DELETE FROM gym_exercises")
    suspend fun clear()
}

@Dao
interface HomeDao {
    @Query("SELECT * FROM home_exercises ORDER BY `group`, id")
    fun observeAll(): Flow<List<HomeExercise>>

    @Query("SELECT * FROM home_exercises ORDER BY `group`, id")
    suspend fun getAll(): List<HomeExercise>

    @Query("SELECT COUNT(*) FROM home_exercises")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ex: HomeExercise): Long

    @Update
    suspend fun update(ex: HomeExercise)

    @Delete
    suspend fun delete(ex: HomeExercise)

    @Query("DELETE FROM home_exercises")
    suspend fun clear()

    @Query("SELECT * FROM home_done WHERE epochDay = :day")
    fun observeDone(day: Long): Flow<List<HomeDone>>

    @Query("SELECT * FROM home_done")
    suspend fun getAllDone(): List<HomeDone>

    @Query("SELECT * FROM home_done")
    fun observeAllDone(): Flow<List<HomeDone>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markDone(done: HomeDone)

    @Query("DELETE FROM home_done WHERE epochDay = :day AND exerciseId = :exerciseId")
    suspend fun unmarkDone(day: Long, exerciseId: Int)

    @Query("DELETE FROM home_done WHERE exerciseId = :exerciseId")
    suspend fun deleteDoneFor(exerciseId: Int)

    @Query("DELETE FROM home_done")
    suspend fun clearDone()
}

@Dao
interface FutsalDao {
    @Query("SELECT * FROM futsal_plans ORDER BY dayIndex, hour, minute")
    fun observeAll(): Flow<List<FutsalPlan>>

    @Query("SELECT * FROM futsal_plans ORDER BY dayIndex, hour, minute")
    suspend fun getAll(): List<FutsalPlan>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: FutsalPlan): Long

    @Update
    suspend fun update(plan: FutsalPlan)

    @Delete
    suspend fun delete(plan: FutsalPlan)

    @Query("DELETE FROM futsal_plans")
    suspend fun clear()
}

@Dao
interface LogDao {
    @Query("SELECT * FROM workout_logs ORDER BY epochDay DESC, id DESC")
    fun observeAll(): Flow<List<WorkoutLog>>

    @Query("SELECT * FROM workout_logs ORDER BY epochDay DESC, id DESC")
    suspend fun getAll(): List<WorkoutLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: WorkoutLog): Long

    @Update
    suspend fun update(log: WorkoutLog)

    @Delete
    suspend fun delete(log: WorkoutLog)

    @Query("DELETE FROM workout_logs")
    suspend fun clear()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goal_stages")
    fun observeAll(): Flow<List<GoalStage>>

    @Query("SELECT * FROM goal_stages")
    suspend fun getAll(): List<GoalStage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stage: GoalStage)

    @Query("DELETE FROM goal_stages")
    suspend fun clear()
}
