package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldGreenDark,
    onPrimary = Color(0xFF022C22),
    primaryContainer = EmeraldGreenContainerDark,
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = CashFlowBlue,
    onSecondary = Color.White,
    background = DeepNavyBackground,
    onBackground = NavyTextPrimaryDark,
    surface = NavySurfaceDark,
    onSurface = NavyTextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = NavyTextSecondaryDark,
    error = CashFlowRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldGreenLight,
    onPrimary = Color.White,
    primaryContainer = EmeraldGreenContainerLight,
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF0F172A),
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = NavyTextPrimaryLight,
    surface = LightSurface,
    onSurface = NavyTextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = NavyTextSecondaryLight,
    error = CashFlowRed,
    onError = Color.White
)

@Composable
fun DompetPintarTheme(
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
