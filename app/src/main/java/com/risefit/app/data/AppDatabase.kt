package com.risefit.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AlarmEntity::class,
        ScheduleItem::class,
        GymExercise::class,
        HomeExercise::class,
        HomeDone::class,
        FutsalPlan::class,
        WorkoutLog::class,
        GoalStage::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun gymDao(): GymDao
    abstract fun homeDao(): HomeDao
    abstract fun futsalDao(): FutsalDao
    abstract fun logDao(): LogDao
    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "risefit.db"
                ).build().also { instance = it }
            }
    }
}
