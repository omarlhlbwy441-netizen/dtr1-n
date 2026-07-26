package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RafeeqDarkColorScheme = darkColorScheme(
    primary = CyanGlow,
    onPrimary = Color.Black,
    secondary = GoldAccent,
    onSecondary = Color.Black,
    tertiary = PurpleAccent,
    background = CyberDarkBg,
    onBackground = TextPrimary,
    surface = CyberCardBg,
    onSurface = TextPrimary,
    surfaceVariant = CyberCardBorder,
    onSurfaceVariant = TextSecondary
)

@Composable
fun RafeeqTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RafeeqDarkColorScheme,
        typography = Typography,
        content = content
    )
}

