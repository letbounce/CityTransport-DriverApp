package com.example.cityapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AppTeal,
    onPrimary = Color.Black,
    primaryContainer = AppNavy,
    onPrimaryContainer = AppSurface,
    secondary = AppCyan,
    onSecondary = Color.Black,
    tertiary = AppTealDark,
    background = AppNavyDeep,
    onBackground = Color(0xFFE8EEF3),
    surface = AppNavy,
    onSurface = Color(0xFFE8EEF3),
    surfaceVariant = Color(0xFF2A3F56),
    onSurfaceVariant = Color(0xFFB8C5D0),
    outline = AppOutlineSoft
)

private val LightColorScheme = lightColorScheme(
    primary = AppTealDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F7F4),
    onPrimaryContainer = AppNavyDeep,
    secondary = AppNavy,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EEF5),
    onSecondaryContainer = AppNavyDeep,
    tertiary = AppCyan,
    onTertiary = AppNavyDeep,
    background = AppSurfaceMuted,
    onBackground = AppOnSurface,
    surface = AppSurface,
    onSurface = AppOnSurface,
    surfaceVariant = AppSurfaceCardHighlight,
    onSurfaceVariant = AppOnSurfaceMuted,
    outline = AppOutlineSoft,
    outlineVariant = Color(0xFFDDE5EB)
)

val CityAppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CityAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = CityAppShapes,
        content = content
    )
}
