package com.mybird.game.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MyBirdColorScheme = lightColorScheme(
    primary = Color(0xFFFFA726),
    onPrimary = Color(0xFF2B1B07),
    secondary = Color(0xFF26A69A),
    onSecondary = Color.White,
    tertiary = Color(0xFF66BB6A),
    background = Color(0xFF4EC0CA),
    surface = Color(0xFFFFF8E7),
    onSurface = Color(0xFF2D2114),
    surfaceVariant = Color(0xFFF3E7C9),
    onSurfaceVariant = Color(0xFF5D4B36),
    error = Color(0xFFE53935)
)

@Composable
fun MyBirdTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MyBirdColorScheme,
        content = content
    )
}
