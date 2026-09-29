package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DayWorkoutPlan
import com.example.data.model.WorkoutPlan
import com.example.ui.FitBuddyViewModel
import com.example.ui.Screen
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailScreen(
    viewModel: FitBuddyViewModel,
    modifier: Modifier = Modifier
) {
    // Handle back button to return to Home screen
    BackHandler {
        viewModel.navigateTo(Screen.Home)
    }

    val plan by viewModel.activePlan.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val feedbackText by viewModel.feedbackText.collectAsState()
    val scrollState = rememberScrollState()

    var selectedDayIndex by remember { mutableIntStateOf(0) }

    if (plan == null) {
        Box(
            modifier = modifier.fillMaxSize().background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No active workout plan found.", color = DarkTextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.navigateTo(Screen.Home) },
                    colors = ButtonDefaults.buttonColors(containerColor = LimePrimary, contentColor = DarkBackground)
                ) {
                    Text("Go to Generator")
                }
            }
        }
        return
    }

    val activePlan = plan!!

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("plan_detail_container")
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(Screen.Home) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .testTag("back_to_home_button")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back to Generator", tint = DarkTextPrimary)
            }

            Surface(
                color = AccentCyan.copy(alpha = 0.15f),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = "${activePlan.fitnessGoal} • ${activePlan.intensity}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AccentCyan,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Plan Header Card
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, LimePrimary.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = activePlan.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp
                    ),
                    color = DarkTextPrimary
                )
                Text(
                    text = "Prepared for ${activePlan.userName} • ${activePlan.createdAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkTextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = activePlan.overview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkTextPrimary,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                if (activePlan.appliedFeedback != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚡ Refined: \"${activePlan.appliedFeedback}\"",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = LimePrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Day Selector Tabs
        Text(
            text = "7-DAY WORKOUT SCHEDULE",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = LimePrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(activePlan.days) { index, day ->
                val isSelected = selectedDayIndex == index
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) LimePrimary else DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) LimePrimary else DarkOutline
                    ),
                    modifier = Modifier
                        .clickable { selectedDayIndex = index }
                        .testTag("day_tab_$index")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "DAY ${day.dayNumber}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) DarkBackground else DarkTextMuted
                        )
                        Text(
                            text = if (day.isRestDay) "Rest" else "Train",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) DarkBackground else DarkTextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Day Details
        val currentDay = activePlan.days.getOrNull(selectedDayIndex) ?: activePlan.days.firstOrNull()
        if (currentDay != null) {
            DayDetailCard(day = currentDay)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Nutrition & Recovery Cards
        NutritionAndRecoverySection(plan = activePlan)

        Spacer(modifier = Modifier.height(24.dp))

        // AI Feedback & Refinement Box
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = AccentCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Refine Workout with AI",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DarkTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Request adjustments (e.g. \"lower intensity on Day 3\", \"swap dumbbell squats\", \"more core focus\").",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkTextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { viewModel.setFeedbackText(it) },
                    placeholder = { Text("What would you like adjusted?") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = DarkOutline,
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_feedback_text")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { viewModel.submitFeedback() },
                    enabled = !isLoading && feedbackText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCyan,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_feedback_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = DarkBackground,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Refining Plan...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Update Plan with AI", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun DayDetailCard(day: DayWorkoutPlan) {
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = day.dayTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LimePrimary
                    )
                    Text(
                        text = day.focus,
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkTextSecondary
                    )
                }

                if (day.isRestDay) {
                    Surface(
                        color = AccentAmber.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Rest Day",
                            color = AccentAmber,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Warm-up
            if (day.warmUp.isNotBlank()) {
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🔥", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Warm-Up: ${day.warmUp}",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkTextPrimary
                        )
                    }
                }
            }

            // Exercise List
            if (day.exercises.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    day.exercises.forEach { ex ->
                        Surface(
                            color = DarkSurfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = ex.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = DarkTextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        color = LimePrimary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${ex.sets} sets • ${ex.repsOrDuration}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = LimePrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Target: ${ex.targetMuscle}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AccentCyan
                                    )
                                    Text(
                                        text = "Rest: ${ex.restSeconds}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DarkTextSecondary
                                    )
                                }

                                if (ex.formTip.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "💡 ${ex.formTip}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DarkTextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "🌱 Scheduled rest & recovery. Hydrate, eat nutrient-dense meals, and recharge.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentAmber
                )
            }

            // Cool-down
            if (day.coolDown.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("❄️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cool-Down: ${day.coolDown}",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkTextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NutritionAndRecoverySection(plan: WorkoutPlan) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Nutrition Card
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🥗", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nutrition Protocol",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LimePrimary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                plan.nutritionTips.forEach { tip ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("•", color = LimePrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                        Text(text = tip, style = MaterialTheme.typography.bodySmall, color = DarkTextPrimary)
                    }
                }
            }
        }

        // Recovery Card
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💤", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sleep & Recovery Protocol",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AccentCyan
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                plan.recoveryTips.forEach { tip ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("•", color = AccentCyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                        Text(text = tip, style = MaterialTheme.typography.bodySmall, color = DarkTextPrimary)
                    }
                }
            }
        }
    }
}
