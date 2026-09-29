package com.example.data.repository

import com.example.data.local.FitBuddyDatabase
import com.example.data.local.WorkoutPlanDao
import com.example.data.local.WorkoutPlanEntity
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutPlan
import com.example.data.remote.GeminiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FitBuddyRepository(private val database: FitBuddyDatabase) {
    private val dao: WorkoutPlanDao = database.workoutPlanDao()

    val allSavedPlans: Flow<List<WorkoutPlan>> = dao.getAllPlans().map { entities ->
        entities.mapNotNull { entity ->
            GeminiService.jsonToWorkoutPlan(entity.jsonContent)
        }
    }

    suspend fun generatePlan(profile: UserProfile): WorkoutPlan {
        val plan = GeminiService.generateWorkoutPlan(profile)
        savePlan(plan)
        return plan
    }

    suspend fun refinePlan(currentPlan: WorkoutPlan, feedback: String): WorkoutPlan {
        val updated = GeminiService.refineWorkoutPlan(currentPlan, feedback)
        savePlan(updated)
        return updated
    }

    suspend fun savePlan(plan: WorkoutPlan) {
        val json = GeminiService.workoutPlanToJson(plan)
        val entity = WorkoutPlanEntity(
            planId = plan.planId,
            userName = plan.userName,
            fitnessGoal = plan.fitnessGoal,
            intensity = plan.intensity,
            title = plan.title,
            jsonContent = json,
            timestamp = System.currentTimeMillis()
        )
        dao.insertPlan(entity)
    }

    suspend fun deletePlan(planId: String) {
        dao.deletePlan(planId)
    }

    suspend fun getPlanCount(): Int {
        return dao.getPlanCount()
    }
}
