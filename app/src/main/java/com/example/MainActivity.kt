package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.FitBuddyViewModel
import com.example.ui.Screen
import com.example.ui.screens.AdminPlansScreen
import com.example.ui.screens.HealthDiagnosticsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlanDetailScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.FitBuddyTheme
import com.example.ui.theme.LimePrimary

data class NavItem(
    val title: String,
    val screen: Screen,
    val icon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    private val viewModel: FitBuddyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitBuddyTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val activePlan by viewModel.activePlan.collectAsState()

                val navItems = listOf(
                    NavItem("Generator", Screen.Home, Icons.Default.FitnessCenter, "nav_generator"),
                    NavItem(
                        title = if (activePlan != null) "Plan" else "Plan (Empty)",
                        screen = Screen.PlanDetail,
                        icon = Icons.Default.CalendarMonth,
                        testTag = "nav_plan"
                    ),
                    NavItem("Admin", Screen.Admin, Icons.Default.AdminPanelSettings, "nav_admin"),
                    NavItem("Health", Screen.Diagnostics, Icons.Default.HealthAndSafety, "nav_health")
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DarkBackground,
                    bottomBar = {
                        NavigationBar(
                            containerColor = DarkSurface,
                            tonalElevation = 8.dp,
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            navItems.forEach { item ->
                                val selected = currentScreen == item.screen
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.navigateTo(item.screen) },
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title
                                        )
                                    },
                                    label = { Text(item.title) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = DarkBackground,
                                        selectedTextColor = LimePrimary,
                                        indicatorColor = LimePrimary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    when (currentScreen) {
                        is Screen.Home -> HomeScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                        is Screen.PlanDetail -> PlanDetailScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                        is Screen.Admin -> AdminPlansScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                        is Screen.Diagnostics -> HealthDiagnosticsScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
