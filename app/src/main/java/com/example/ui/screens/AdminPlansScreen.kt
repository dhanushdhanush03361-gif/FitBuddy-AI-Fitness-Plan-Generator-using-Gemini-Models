package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WorkoutPlan
import com.example.ui.FitBuddyViewModel
import com.example.ui.Screen
import com.example.ui.theme.*

@Composable
fun AdminPlansScreen(
    viewModel: FitBuddyViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo(Screen.Home)
    }

    val isAuthorized by viewModel.isAdminAuthenticated.collectAsState()
    val allPlans by viewModel.allSavedPlans.collectAsState()

    var tokenInput by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("admin_screen_container")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Admin Dashboard",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp
                    ),
                    color = DarkTextPrimary
                )
                Text(
                    text = "Saved User Plans & Database Records",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkTextSecondary
                )
            }

            Surface(
                color = if (isAuthorized) LimePrimary.copy(alpha = 0.2f) else AccentRose.copy(alpha = 0.2f),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = if (isAuthorized) "Authorized" else "Locked",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isAuthorized) LimePrimary else AccentRose,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (!isAuthorized) {
            // Admin Authentication Card
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = AccentAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admin Access Protected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = DarkTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enter the ADMIN_TOKEN to inspect saved plans (default: fitbuddy-admin-secret-2026 or 'admin').",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkTextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = {
                            tokenInput = it
                            authError = null
                        },
                        label = { Text("Admin Token") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LimePrimary,
                            unfocusedBorderColor = DarkOutline,
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("admin_token_input")
                    )

                    if (authError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = authError!!,
                            color = AccentRose,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val success = viewModel.authenticateAdmin(tokenInput)
                            if (!success) {
                                authError = "Invalid admin token."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LimePrimary,
                            contentColor = DarkBackground
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_login_button")
                    ) {
                        Text("Authenticate Session", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Plan List View
            Text(
                text = "SAVED PLANS IN DATABASE (${allPlans.size})",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = LimePrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (allPlans.isEmpty()) {
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📭", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No plans generated yet.", color = DarkTextSecondary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.navigateTo(Screen.Home) },
                            colors = ButtonDefaults.buttonColors(containerColor = LimePrimary, contentColor = DarkBackground)
                        ) {
                            Text("Create First Plan")
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(allPlans, key = { it.planId }) { plan ->
                        PlanSummaryCard(
                            plan = plan,
                            onSelect = { viewModel.selectPlan(plan) },
                            onDelete = { viewModel.deletePlan(plan.planId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlanSummaryCard(
    plan: WorkoutPlan,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("admin_plan_card_${plan.planId}")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plan.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = DarkTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = AccentCyan.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = plan.fitnessGoal,
                            color = AccentCyan,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "User: ${plan.userName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkTextSecondary
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Plan", tint = AccentRose)
            }
        }
    }
}
