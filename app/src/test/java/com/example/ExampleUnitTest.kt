package com.example

import com.example.data.model.UserProfile
import com.example.data.remote.GeminiService
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun fallbackPlan_generatesSevenDays() {
        val profile = UserProfile(
            name = "Test User",
            fitnessGoal = "Muscle Gain",
            intensity = "High"
        )
        val plan = GeminiService.getOfflineFallbackPlan(profile)
        assertEquals("Test User", plan.userName)
        assertEquals(7, plan.days.size)
        assertTrue(plan.nutritionTips.isNotEmpty())
        assertTrue(plan.recoveryTips.isNotEmpty())
    }

    @Test
    fun workoutPlanJson_roundTripSerialization() {
        val profile = UserProfile(
            name = "Alex",
            fitnessGoal = "Weight Loss",
            intensity = "Medium"
        )
        val plan = GeminiService.getOfflineFallbackPlan(profile)
        val json = GeminiService.workoutPlanToJson(plan)
        assertTrue(json.contains("Alex"))
        assertTrue(json.contains("Weight Loss"))

        val deserialized = GeminiService.jsonToWorkoutPlan(json)
        assertNotNull(deserialized)
        assertEquals(plan.planId, deserialized?.planId)
        assertEquals(plan.days.size, deserialized?.days?.size)
    }
}
