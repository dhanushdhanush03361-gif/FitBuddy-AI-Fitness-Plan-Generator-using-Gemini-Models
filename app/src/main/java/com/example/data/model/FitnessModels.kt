package com.example.data.model

data class UserProfile(
    val name: String = "Athlete",
    val age: Int = 26,
    val gender: String = "Male",
    val weightKg: Float = 72f,
    val heightCm: Float = 175f,
    val fitnessLevel: String = "Intermediate",
    val fitnessGoal: String = "Muscle Gain",
    val intensity: String = "Medium",
    val limitations: String = "None"
)

data class ExerciseItem(
    val name: String,
    val sets: String,
    val repsOrDuration: String,
    val restSeconds: String,
    val targetMuscle: String,
    val formTip: String
)

data class DayWorkoutPlan(
    val dayNumber: Int,
    val dayTitle: String,
    val focus: String,
    val warmUp: String,
    val exercises: List<ExerciseItem>,
    val coolDown: String,
    val isRestDay: Boolean = false
)

data class WorkoutPlan(
    val planId: String,
    val userName: String,
    val fitnessGoal: String,
    val intensity: String,
    val title: String,
    val overview: String,
    val days: List<DayWorkoutPlan>,
    val nutritionTips: List<String>,
    val recoveryTips: List<String>,
    val createdAt: String,
    val updatedAt: String? = null,
    val appliedFeedback: String? = null
)
