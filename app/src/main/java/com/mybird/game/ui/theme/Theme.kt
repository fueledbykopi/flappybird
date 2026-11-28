package com.mybird.game.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.mybird.game.engine.GameTheme

@Composable
fun MyBirdTheme(
    selectedTheme: GameTheme = GameTheme.DEFAULT,
    content: @Composable () -> Unit
) {
    val style = selectedTheme.uiStyle()
    val colorScheme = darkColorScheme(
        primary = style.accent,
        onPrimary = style.onAccent,
        secondary = style.accentSecondary,
        onSecondary = Color(0xFF0A1018),
        background = Color(0xFF0D1120),
        surface = Color(0xFF14192B),
        onSurface = Color.White,
        surfaceVariant = Color(0xFF1E253D),
        onSurfaceVariant = Color(0xFFC9D1E3),
        error = Color(0xFFFF5252)
    )
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
