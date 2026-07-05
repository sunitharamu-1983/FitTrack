package com.sunitha.fittrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary          = Green80,
    secondary        = Orange80,
    tertiary         = GreenGrey80,
    background       = Color(0xFF121212),
    surface          = Color(0xFF1E1E1E),
    surfaceVariant   = Color(0xFF2A2A2A),
    onPrimary        = Color(0xFF1B5E20),
    onSecondary      = Color(0xFF3E2723),
    onBackground     = Color(0xFFEEEEEE),
    onSurface        = Color(0xFFEEEEEE),
)

private val LightColorScheme = lightColorScheme(
    primary          = Green40,
    secondary        = Orange40,
    tertiary         = GreenGrey40,
    background       = Color(0xFFF4F6F4),
    surface          = Color(0xFFFFFFFF),
    surfaceVariant   = Color(0xFFEEEEEE),
    onPrimary        = Color.White,
    onSecondary      = Color.White,
    onBackground     = Color(0xFF1C1C1C),
    onSurface        = Color(0xFF1C1C1C),
)

@Composable
fun FitTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
