package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_plans")
data class WorkoutPlanEntity(
    @PrimaryKey
    val planId: String,
    val userName: String,
    val fitnessGoal: String,
    val intensity: String,
    val title: String,
    val jsonContent: String,
    val timestamp: Long = System.currentTimeMillis()
)
