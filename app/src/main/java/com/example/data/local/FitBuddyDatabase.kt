package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WorkoutPlanEntity::class], version = 1, exportSchema = false)
abstract class FitBuddyDatabase : RoomDatabase() {
    abstract fun workoutPlanDao(): WorkoutPlanDao

    companion object {
        @Volatile
        private var INSTANCE: FitBuddyDatabase? = null

        fun getDatabase(context: Context): FitBuddyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FitBuddyDatabase::class.java,
                    "fitbuddy_room.db"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
