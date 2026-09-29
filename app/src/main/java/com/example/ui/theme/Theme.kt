package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = LimePrimary,
    onPrimary = LimeOnPrimary,
    primaryContainer = LimePrimaryContainer,
    onPrimaryContainer = LimeOnPrimaryContainer,
    secondary = AccentCyan,
    onSecondary = DarkBackground,
    tertiary = AccentAmber,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = LimePrimary,
    onPrimary = LimeOnPrimary,
    primaryContainer = LimePrimaryContainer,
    onPrimaryContainer = LimeOnPrimaryContainer,
    secondary = AccentCyan,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary
)

@Composable
fun FitBuddyTheme(
    content: @Composable () -> Unit
) {
    // Fitness apps look best in intentional athletic dark mode
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    FitBuddyTheme(content = content)
}
