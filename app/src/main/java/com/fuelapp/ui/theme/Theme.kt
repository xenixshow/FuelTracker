package com.fuelapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    onPrimary = Color.White,
    primaryContainer = Green80,
    onPrimaryContainer = Green10,
    secondary = Blue40,
    onSecondary = Color.White,
    secondaryContainer = Blue80,
    onSecondaryContainer = Blue20,
    tertiary = Orange40,
    onTertiary = Color.White,
    tertiaryContainer = Orange80,
    onTertiaryContainer = Orange20,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = Neutral95,
    onSurfaceVariant = Neutral10,
)

private val DarkColorScheme = darkColorScheme(
    primary = Green60,
    onPrimary = Green10,
    primaryContainer = Green20,
    onPrimaryContainer = Green80,
    secondary = Blue60,
    onSecondary = Blue20,
    secondaryContainer = Blue20,
    onSecondaryContainer = Blue80,
    tertiary = Orange60,
    onTertiary = Orange20,
    tertiaryContainer = Orange20,
    onTertiaryContainer = Orange80,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Neutral90,
)

@Composable
fun FuelTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
