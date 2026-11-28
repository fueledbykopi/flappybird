package com.mybird.game.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.mybird.game.engine.GameTheme

/**
 * Immutable modern UI palette tailored to each [GameTheme] so menus, HUD pills,
 * buttons, and dialogs harmonize seamlessly with the live animated canvas.
 */
@Immutable
data class GameThemeUiStyle(
    val accent: Color,
    val accentSecondary: Color,
    val onAccent: Color,
    val glassSurface: Color,
    val glassBorder: Color,
    val chipGradientTop: Color,
    val chipGradientBottom: Color,
    val titleGlow: Color,
    val badgeBg: Color,
    val badgeText: Color
)

fun GameTheme.uiStyle(): GameThemeUiStyle = when (this) {
    GameTheme.BLOK_M -> GameThemeUiStyle(
        accent = Color(0xFFFFB74D),
        accentSecondary = Color(0xFF26C6DA),
        onAccent = Color(0xFF1A1208),
        glassSurface = Color(0xCC11162B),
        glassBorder = Color(0x55FFB74D),
        chipGradientTop = Color(0xFF28204F),
        chipGradientBottom = Color(0xFFE07A5F),
        titleGlow = Color(0xFFFFB74D),
        badgeBg = Color(0x38FFB74D),
        badgeText = Color(0xFFFFE0B2)
    )

    GameTheme.GTA6 -> GameThemeUiStyle(
        accent = Color(0xFFFF4D94),
        accentSecondary = Color(0xFF00F5D4),
        onAccent = Color.White,
        glassSurface = Color(0xCC1D0B33),
        glassBorder = Color(0x66FF4D94),
        chipGradientTop = Color(0xFF8E1B75),
        chipGradientBottom = Color(0xFFFF9F43),
        titleGlow = Color(0xFFFF4D94),
        badgeBg = Color(0x38FF4D94),
        badgeText = Color(0xFFFFD6E8)
    )

    GameTheme.CYBERPUNK -> GameThemeUiStyle(
        accent = Color(0xFFFCEE0A),
        accentSecondary = Color(0xFF05D9E8),
        onAccent = Color(0xFF090C18),
        glassSurface = Color(0xD2080D1F),
        glassBorder = Color(0x6605D9E8),
        chipGradientTop = Color(0xFF0E1B3E),
        chipGradientBottom = Color(0xFFFF2A6D),
        titleGlow = Color(0xFF05D9E8),
        badgeBg = Color(0x3305D9E8),
        badgeText = Color(0xFFBDF7FF)
    )

    GameTheme.KARHUTLA_BORNEO -> GameThemeUiStyle(
        accent = Color(0xFFFF7A1A),
        accentSecondary = Color(0xFFFFCA28),
        onAccent = Color(0xFF1F0E06),
        glassSurface = Color(0xD220110B),
        glassBorder = Color(0x66FF7A1A),
        chipGradientTop = Color(0xFF4A1D0D),
        chipGradientBottom = Color(0xFFFF7A1A),
        titleGlow = Color(0xFFFF7A1A),
        badgeBg = Color(0x38FF7A1A),
        badgeText = Color(0xFFFFDFC4)
    )

    GameTheme.ORIGINAL -> GameThemeUiStyle(
        accent = Color(0xFF4CAF50),
        accentSecondary = Color(0xFFFFCA28),
        onAccent = Color.White,
        glassSurface = Color(0xCC132B34),
        glassBorder = Color(0x667BE0EA),
        chipGradientTop = Color(0xFF29B6D0),
        chipGradientBottom = Color(0xFF5FC743),
        titleGlow = Color(0xFFFFD54F),
        badgeBg = Color(0x387BE0EA),
        badgeText = Color(0xFFE0F7FA)
    )
}
