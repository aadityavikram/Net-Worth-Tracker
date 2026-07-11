package com.networth.tracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GreenPrimary = Color(0xFF2E7D32)
private val GreenDark = Color(0xFF1B5E20)
private val RedLiability = Color(0xFFC62828)
private val SurfaceLight = Color(0xFFF5F7FA)
private val SurfaceDark = Color(0xFF121212)

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = GreenDark,
    secondary = Color(0xFF1565C0),
    error = RedLiability,
    background = SurfaceLight,
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF003300),
    primaryContainer = GreenDark,
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFF90CAF9),
    error = Color(0xFFEF5350),
    background = SurfaceDark,
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE3E2E6)
)

@Composable
fun NetWorthTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}

val LiabilityColor = RedLiability
val AssetPositiveColor = GreenPrimary
