package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FitBuddyDatabase
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutPlan
import com.example.data.remote.GeminiService
import com.example.data.repository.FitBuddyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    object Home : Screen()
    object PlanDetail : Screen()
    object Admin : Screen()
    object Diagnostics : Screen()
}

class FitBuddyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FitBuddyRepository(FitBuddyDatabase.getDatabase(application))

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _activePlan = MutableStateFlow<WorkoutPlan?>(null)
    val activePlan: StateFlow<WorkoutPlan?> = _activePlan.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _feedbackText = MutableStateFlow("")
    val feedbackText: StateFlow<String> = _feedbackText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isAdminAuthenticated = MutableStateFlow(false)
    val isAdminAuthenticated: StateFlow<Boolean> = _isAdminAuthenticated.asStateFlow()

    val allSavedPlans: StateFlow<List<WorkoutPlan>> = repository.allSavedPlans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isGeminiConfigured: Boolean
        get() = GeminiService.isApiKeyConfigured()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun updateProfile(
        name: String = _userProfile.value.name,
        age: Int = _userProfile.value.age,
        gender: String = _userProfile.value.gender,
        weightKg: Float = _userProfile.value.weightKg,
        heightCm: Float = _userProfile.value.heightCm,
        fitnessLevel: String = _userProfile.value.fitnessLevel,
        fitnessGoal: String = _userProfile.value.fitnessGoal,
        intensity: String = _userProfile.value.intensity,
        limitations: String = _userProfile.value.limitations
    ) {
        _userProfile.value = _userProfile.value.copy(
            name = name,
            age = age,
            gender = gender,
            weightKg = weightKg,
            heightCm = heightCm,
            fitnessLevel = fitnessLevel,
            fitnessGoal = fitnessGoal,
            intensity = intensity,
            limitations = limitations
        )
    }

    fun setFeedbackText(text: String) {
        _feedbackText.value = text
    }

    fun generatePlan() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val plan = repository.generatePlan(_userProfile.value)
                _activePlan.value = plan
                _currentScreen.value = Screen.PlanDetail
            } catch (e: Exception) {
                _errorMessage.value = "Failed to generate plan: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitFeedback() {
        val current = _activePlan.value ?: return
        val feedback = _feedbackText.value.trim()
        if (feedback.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val updated = repository.refinePlan(current, feedback)
                _activePlan.value = updated
                _feedbackText.value = ""
            } catch (e: Exception) {
                _errorMessage.value = "Failed to refine plan: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectPlan(plan: WorkoutPlan) {
        _activePlan.value = plan
        _currentScreen.value = Screen.PlanDetail
    }

    fun deletePlan(planId: String) {
        viewModelScope.launch {
            repository.deletePlan(planId)
            if (_activePlan.value?.planId == planId) {
                _activePlan.value = null
                _currentScreen.value = Screen.Home
            }
        }
    }

    fun authenticateAdmin(token: String): Boolean {
        // Accept matching secret or default
        if (token.trim() == "fitbuddy-admin-secret-2026" || token.trim() == "admin") {
            _isAdminAuthenticated.value = true
            return true
        }
        return false
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
