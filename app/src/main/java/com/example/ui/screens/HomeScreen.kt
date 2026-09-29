package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.FitBuddyViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: FitBuddyViewModel,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val scrollState = rememberScrollState()

    val goals = listOf(
        "Weight Loss" to "🔥",
        "Muscle Gain" to "💪",
        "General Wellness" to "⚡",
        "Flexibility" to "🧘"
    )

    val intensities = listOf("Low", "Medium", "High")
    val levels = listOf("Beginner", "Intermediate", "Advanced")
    val genders = listOf("Male", "Female", "Other")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("home_screen_container")
    ) {
        // Hero Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(LimePrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "FitBuddy Logo",
                    tint = DarkBackground,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "FitBuddy",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = DarkTextPrimary
                )
                Text(
                    text = "Gemini 2.5 Flash Fitness Engine",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Key Status Card
        if (!viewModel.isGeminiConfigured) {
            Surface(
                color = AccentAmber.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentAmber.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Notice",
                        tint = AccentAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Demo Mode: Running smart built-in routines. Add GEMINI_API_KEY in Secrets for personalized generative AI!",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkTextPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Error message banner if any
        AnimatedVisibility(visible = errorMessage != null) {
            errorMessage?.let { error ->
                Surface(
                    color = AccentRose.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentRose),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = DarkTextPrimary)
                        }
                    }
                }
            }
        }

        // Fitness Profile Card
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "ATHLETE PROFILE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = LimePrimary
                )
                Text(
                    text = "Personal Information",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DarkTextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Name Input
                OutlinedTextField(
                    value = profile.name,
                    onValueChange = { viewModel.updateProfile(name = it) },
                    label = { Text("Name / Nickname") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = DarkTextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LimePrimary,
                        unfocusedBorderColor = DarkOutline,
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_name")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Age and Gender
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = profile.age.toString(),
                        onValueChange = {
                            val newAge = it.toIntOrNull() ?: profile.age
                            viewModel.updateProfile(age = newAge)
                        },
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LimePrimary,
                            unfocusedBorderColor = DarkOutline,
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_age")
                    )

                    // Gender Selector
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text("Gender", style = MaterialTheme.typography.bodySmall, color = DarkTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            genders.forEach { g ->
                                val selected = profile.gender == g
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) LimePrimary else DarkSurfaceVariant)
                                        .clickable { viewModel.updateProfile(gender = g) }
                                        .padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = g,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (selected) DarkBackground else DarkTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Weight & Height
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = profile.weightKg.toString(),
                        onValueChange = {
                            val w = it.toFloatOrNull() ?: profile.weightKg
                            viewModel.updateProfile(weightKg = w)
                        },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LimePrimary,
                            unfocusedBorderColor = DarkOutline,
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_weight")
                    )

                    OutlinedTextField(
                        value = profile.heightCm.toString(),
                        onValueChange = {
                            val h = it.toFloatOrNull() ?: profile.heightCm
                            viewModel.updateProfile(heightCm = h)
                        },
                        label = { Text("Height (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LimePrimary,
                            unfocusedBorderColor = DarkOutline,
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_height")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Fitness Level
                Text(
                    text = "EXPERIENCE LEVEL",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = DarkTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    levels.forEach { lvl ->
                        val selected = profile.fitnessLevel == lvl
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) LimePrimary else DarkSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) LimePrimary else DarkOutline),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clickable { viewModel.updateProfile(fitnessLevel = lvl) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = lvl,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (selected) DarkBackground else DarkTextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Fitness Goal Selection
                Text(
                    text = "PRIMARY FITNESS GOAL",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = DarkTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    goals.forEach { (goalName, icon) ->
                        val selected = profile.fitnessGoal == goalName
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) DarkSurfaceVariant else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                if (selected) 2.dp else 1.dp,
                                if (selected) LimePrimary else DarkOutline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable { viewModel.updateProfile(fitnessGoal = goalName) }
                                .testTag("goal_option_${goalName.replace(" ", "_")}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = icon, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = goalName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (selected) LimePrimary else DarkTextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = LimePrimary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Intensity Selection
                Text(
                    text = "TARGET INTENSITY",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = DarkTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    intensities.forEach { intensityName ->
                        val selected = profile.intensity == intensityName
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) AccentCyan.copy(alpha = 0.2f) else DarkSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selected) AccentCyan else DarkOutline
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clickable { viewModel.updateProfile(intensity = intensityName) }
                                .testTag("intensity_${intensityName}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = intensityName,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (selected) AccentCyan else DarkTextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Physical Limitations
                OutlinedTextField(
                    value = profile.limitations,
                    onValueChange = { viewModel.updateProfile(limitations = it) },
                    label = { Text("Injuries / Equipment Notes") },
                    placeholder = { Text("e.g. Mild knee soreness, dumbbells only") },
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LimePrimary,
                        unfocusedBorderColor = DarkOutline,
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_limitations")
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Generate Plan Button
                Button(
                    onClick = { viewModel.generatePlan() },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LimePrimary,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("generate_workout_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = DarkBackground,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Architecting 7-Day Plan...",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    } else {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generate 7-Day Workout Plan",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
