package com.mybird.game.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mybird.game.engine.GamePhase
import com.mybird.game.engine.GameTheme
import com.mybird.game.engine.Medal
import com.mybird.game.ui.canvas.drawThemeVectorCharacter
import com.mybird.game.ui.theme.GameThemeUiStyle
import com.mybird.game.ui.theme.uiStyle

@Composable
fun TopHudBar(
    phase: GamePhase,
    selectedTheme: GameTheme,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onTogglePause: () -> Unit,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = selectedTheme.uiStyle()
    val inGameplay = phase == GamePhase.READY || phase == GamePhase.PLAYING || phase == GamePhase.PAUSED

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (inGameplay) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HudIconButton(
                    onClick = onBackToMenu,
                    imageVector = Icons.Rounded.Home,
                    contentDescription = "Menu Utama",
                    borderColor = style.glassBorder
                )
                if (phase == GamePhase.PLAYING || phase == GamePhase.PAUSED) {
                    HudIconButton(
                        onClick = onTogglePause,
                        imageVector = if (phase == GamePhase.PAUSED) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = if (phase == GamePhase.PAUSED) "Lanjutkan" else "Jeda",
                        borderColor = style.glassBorder
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.size(44.dp))
        }

        HudIconButton(
            onClick = onToggleSound,
            imageVector = if (soundEnabled) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
            contentDescription = if (soundEnabled) "Matikan Suara" else "Nyalakan Suara",
            borderColor = style.glassBorder
        )
    }
}

@Composable
private fun HudIconButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String,
    borderColor: Color
) {
    Surface(
        shape = CircleShape,
        color = Color(0x960D1224),
        border = BorderStroke(1.2.dp, borderColor),
        modifier = Modifier.size(44.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun LiveScoreCounter(
    score: Int,
    selectedTheme: GameTheme,
    modifier: Modifier = Modifier
) {
    val style = selectedTheme.uiStyle()
    Text(
        text = score.toString(),
        style = TextStyle(
            color = Color.White,
            fontSize = 64.sp,
            fontWeight = FontWeight.Black,
            shadow = Shadow(
                color = style.titleGlow.copy(alpha = 0.65f),
                offset = Offset(0f, 4f),
                blurRadius = 14f
            )
        ),
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 56.dp)
    )
}

@Composable
fun ReadyTapHint(
    selectedTheme: GameTheme,
    modifier: Modifier = Modifier
) {
    val style = selectedTheme.uiStyle()
    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = style.glassSurface,
            border = BorderStroke(1.2.dp, style.accent.copy(alpha = 0.75f)),
            modifier = Modifier.padding(bottom = 110.dp)
        ) {
            Text(
                text = "Ketuk layar untuk terbang",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = 0.3.sp,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)
            )
        }
    }
}

/**
 * Clean, modern, uncluttered main menu overlay that keeps the center viewport open
 * so the selected theme's animated world and signature character take center stage.
 */
@Composable
fun MainMenuOverlay(
    bestScore: Int,
    selectedTheme: GameTheme,
    customBirdBitmap: ImageBitmap?,
    customBackgroundBitmap: ImageBitmap?,
    isLoadingBird: Boolean,
    isLoadingBackground: Boolean,
    mediaError: String?,
    onSelectTheme: (GameTheme) -> Unit,
    onStartGame: () -> Unit,
    onPickBirdPhoto: () -> Unit,
    onResetBirdPhoto: () -> Unit,
    onPickBackgroundPhoto: () -> Unit,
    onResetBackgroundPhoto: () -> Unit,
    onDismissMediaError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = selectedTheme.uiStyle()
    var showCustomizeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top minimal header row: High Score pill aligned left (TopHudBar sound sits right)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0x960D1224),
                    border = BorderStroke(1.2.dp, style.glassBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = style.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = bestScore.toString(),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Clean modern title
            Text(
                text = "My Burung",
                maxLines = 1,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    shadow = Shadow(
                        color = style.titleGlow.copy(alpha = 0.7f),
                        offset = Offset(0f, 5f),
                        blurRadius = 18f
                    )
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Minimal active theme + character pill
            Surface(
                shape = RoundedCornerShape(50),
                color = style.glassSurface,
                border = BorderStroke(1.dp, style.glassBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(style.accent)
                    )
                    Text(
                        text = "${selectedTheme.displayName}  •  ${
                            if (customBirdBitmap != null) "Foto Custom" else selectedTheme.characterName
                        }",
                        color = style.badgeText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Open breathing room in center for the live animated world & hovering character
        Spacer(modifier = Modifier.weight(1f))

        // Compact floating glass bottom dock
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (mediaError != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xE0331216),
                    border = BorderStroke(1.dp, Color(0x99FF5252)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mediaError,
                            color = Color(0xFFFFCDD2),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismissMediaError,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Tutup",
                                tint = Color(0xFFFFCDD2),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(26.dp),
                color = style.glassSurface,
                border = BorderStroke(1.2.dp, style.glassBorder),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Sleek horizontal 5-theme selector
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(GameTheme.entries, key = { it.id }) { theme ->
                            ThemeSelectorChip(
                                theme = theme,
                                selected = theme == selectedTheme,
                                onClick = { onSelectTheme(theme) }
                            )
                        }
                    }

                    // 2. Primary Play CTA
                    Button(
                        onClick = onStartGame,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = style.accent,
                            contentColor = style.onAccent
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mulai Main",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.4.sp
                        )
                    }

                    // 3. Minimal 2-pill secondary row (Karakter & Background)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompactCustomPill(
                            label = if (customBirdBitmap != null) "Karakter • Custom" else "Karakter",
                            isLoading = isLoadingBird,
                            style = style,
                            onClick = { showCustomizeDialog = true },
                            modifier = Modifier.weight(1f),
                            leading = {
                                BirdPreviewBadge(
                                    theme = selectedTheme,
                                    bitmap = customBirdBitmap,
                                    isLoading = isLoadingBird,
                                    sizeDp = 28.dp
                                )
                            }
                        )

                        CompactCustomPill(
                            label = if (customBackgroundBitmap != null) "BG • Custom" else "Background",
                            isLoading = isLoadingBackground,
                            style = style,
                            onClick = { showCustomizeDialog = true },
                            modifier = Modifier.weight(1f),
                            leading = {
                                BackgroundPreviewBadge(
                                    theme = selectedTheme,
                                    bitmap = customBackgroundBitmap,
                                    isLoading = isLoadingBackground,
                                    sizeDp = 28.dp
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCustomizeDialog) {
        CustomizeMediaDialog(
            selectedTheme = selectedTheme,
            customBirdBitmap = customBirdBitmap,
            customBackgroundBitmap = customBackgroundBitmap,
            isLoadingBird = isLoadingBird,
            isLoadingBackground = isLoadingBackground,
            onPickBirdPhoto = {
                showCustomizeDialog = false
                onPickBirdPhoto()
            },
            onResetBirdPhoto = onResetBirdPhoto,
            onPickBackgroundPhoto = {
                showCustomizeDialog = false
                onPickBackgroundPhoto()
            },
            onResetBackgroundPhoto = onResetBackgroundPhoto,
            onDismiss = { showCustomizeDialog = false }
        )
    }
}

@Composable
private fun ThemeSelectorChip(
    theme: GameTheme,
    selected: Boolean,
    onClick: () -> Unit
) {
    val chipStyle = theme.uiStyle()
    val border = if (selected) {
        BorderStroke(2.dp, chipStyle.accent)
    } else {
        BorderStroke(1.dp, Color(0x33FFFFFF))
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        border = border,
        modifier = Modifier
            .width(86.dp)
            .height(62.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(chipStyle.chipGradientTop, chipStyle.chipGradientBottom)
                )
            )
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (selected) Color(0x20000000) else Color(0x55000000))
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(chipStyle.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = chipStyle.onAccent,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ThemeCharacterMiniCanvas(
                    theme = theme,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = theme.shortName,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CompactCustomPill(
    label: String,
    isLoading: Boolean,
    style: GameThemeUiStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0x28FFFFFF),
        border = BorderStroke(1.dp, style.glassBorder.copy(alpha = 0.45f)),
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = !isLoading, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            leading()
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CustomizeMediaDialog(
    selectedTheme: GameTheme,
    customBirdBitmap: ImageBitmap?,
    customBackgroundBitmap: ImageBitmap?,
    isLoadingBird: Boolean,
    isLoadingBackground: Boolean,
    onPickBirdPhoto: () -> Unit,
    onResetBirdPhoto: () -> Unit,
    onPickBackgroundPhoto: () -> Unit,
    onResetBackgroundPhoto: () -> Unit,
    onDismiss: () -> Unit
) {
    val style = selectedTheme.uiStyle()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xF512172A),
            border = BorderStroke(1.5.dp, style.glassBorder),
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kustomisasi",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Tutup",
                            tint = Color(0xFFC9D1E3)
                        )
                    }
                }

                // Custom Character row
                CustomizeOptionCard(
                    title = "Karakter",
                    subtitle = if (customBirdBitmap != null) {
                        "Foto custom aktif"
                    } else {
                        "${selectedTheme.characterName} (${selectedTheme.displayName})"
                    },
                    tip = "Foto 1:1 (min. 256×256 px), bisa digeser & zoom",
                    buttonLabel = "Pilih Foto Karakter",
                    accent = style.accent,
                    onAccent = style.onAccent,
                    hasCustom = customBirdBitmap != null,
                    isLoading = isLoadingBird,
                    onPick = onPickBirdPhoto,
                    onReset = onResetBirdPhoto,
                    preview = {
                        BirdPreviewBadge(
                            theme = selectedTheme,
                            bitmap = customBirdBitmap,
                            isLoading = isLoadingBird,
                            sizeDp = 46.dp
                        )
                    }
                )

                // Custom Background row
                CustomizeOptionCard(
                    title = "Background",
                    subtitle = if (customBackgroundBitmap != null) {
                        "Background custom aktif"
                    } else {
                        "Tema ${selectedTheme.displayName}"
                    },
                    tip = "Landscape 16:9 atau Portrait 9:16 untuk scroll mulus",
                    buttonLabel = "Pilih Foto Background",
                    accent = style.accentSecondary,
                    onAccent = Color(0xFF0A1018),
                    hasCustom = customBackgroundBitmap != null,
                    isLoading = isLoadingBackground,
                    onPick = onPickBackgroundPhoto,
                    onReset = onResetBackgroundPhoto,
                    preview = {
                        BackgroundPreviewBadge(
                            theme = selectedTheme,
                            bitmap = customBackgroundBitmap,
                            isLoading = isLoadingBackground,
                            sizeDp = 46.dp
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun CustomizeOptionCard(
    title: String,
    subtitle: String,
    tip: String,
    buttonLabel: String,
    accent: Color,
    onAccent: Color,
    hasCustom: Boolean,
    isLoading: Boolean,
    onPick: () -> Unit,
    onReset: () -> Unit,
    preview: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1B223A),
        border = BorderStroke(1.dp, Color(0x28FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                preview()
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFFB4C0D8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (hasCustom) {
                    IconButton(
                        onClick = onReset,
                        enabled = !isLoading,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Reset ke Tema",
                            tint = Color(0xFFFF8A80)
                        )
                    }
                }
            }

            Text(
                text = tip,
                fontSize = 11.sp,
                color = Color(0xFF8C9AB8)
            )

            Button(
                onClick = onPick,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = onAccent
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = buttonLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun BirdPreviewBadge(
    theme: GameTheme,
    bitmap: ImageBitmap?,
    isLoading: Boolean,
    sizeDp: Dp
) {
    val style = theme.uiStyle()
    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(Color(0xFF1D243C))
            .border(1.5.dp, style.accent, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> CircularProgressIndicator(
                modifier = Modifier.size(sizeDp * 0.5f),
                strokeWidth = 2.dp,
                color = style.accent
            )

            bitmap != null -> Image(
                bitmap = bitmap,
                contentDescription = "Preview Karakter",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            else -> ThemeCharacterMiniCanvas(
                theme = theme,
                modifier = Modifier.size(sizeDp * 0.85f)
            )
        }
    }
}

@Composable
private fun BackgroundPreviewBadge(
    theme: GameTheme,
    bitmap: ImageBitmap?,
    isLoading: Boolean,
    sizeDp: Dp
) {
    val style = theme.uiStyle()
    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(style.chipGradientTop, style.chipGradientBottom)
                )
            )
            .border(1.5.dp, style.accentSecondary, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> CircularProgressIndicator(
                modifier = Modifier.size(sizeDp * 0.5f),
                strokeWidth = 2.dp,
                color = Color.White
            )

            bitmap != null -> Image(
                bitmap = bitmap,
                contentDescription = "Preview Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            else -> Icon(
                imageVector = Icons.Rounded.Landscape,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(sizeDp * 0.55f)
            )
        }
    }
}

@Composable
private fun ThemeCharacterMiniCanvas(
    theme: GameTheme,
    modifier: Modifier = Modifier
) {
    val path = remember { Path() }
    val outlineStroke = remember { Stroke(width = 1.6f) }
    val thinStroke = remember { Stroke(width = 1.2f) }
    val beakStroke = remember { Stroke(width = 1.3f, cap = StrokeCap.Round, join = StrokeJoin.Round) }

    Canvas(modifier = modifier) {
        val r = size.minDimension * 0.30f
        drawThemeVectorCharacter(
            theme = theme,
            cx = size.width * 0.44f,
            cy = size.height * 0.52f,
            r = r,
            wingPhase = 0.4f,
            beakOpen = 0.15f,
            path = path,
            outlineStroke = outlineStroke,
            thinStroke = thinStroke,
            beakStroke = beakStroke
        )
    }
}

@Composable
fun PauseOverlay(
    selectedTheme: GameTheme,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = selectedTheme.uiStyle()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x99060914))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = style.glassSurface,
            border = BorderStroke(1.5.dp, style.glassBorder),
            shadowElevation = 14.dp,
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "DIJEDA",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    color = Color.White
                )

                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = style.accent,
                        contentColor = style.onAccent
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lanjutkan", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }

                OutlinedButton(
                    onClick = onRestart,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.2.dp, Color(0x55FFFFFF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Main Lagi", color = Color.White, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onBackToMenu,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.2.dp, Color(0x55FFFFFF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(Icons.Rounded.Home, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Menu Utama", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun GameOverOverlay(
    visible: Boolean,
    selectedTheme: GameTheme,
    score: Int,
    bestScore: Int,
    isNewBest: Boolean,
    medal: Medal,
    onRetry: () -> Unit,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = selectedTheme.uiStyle()
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.88f),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x88050812))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = style.glassSurface,
                border = BorderStroke(1.5.dp, style.glassBorder),
                shadowElevation = 18.dp,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "GAME OVER",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.6.sp
                    )

                    Text(
                        text = selectedTheme.displayName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = style.accent
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0x22FFFFFF),
                        border = BorderStroke(1.dp, Color(0x30FFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ScoreColumn(title = "SKOR", value = score.toString())
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(46.dp)
                                    .background(Color(0x33FFFFFF))
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "TERBAIK",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        color = Color(0xFFB4C0D8)
                                    )
                                    if (isNewBest) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = style.accent
                                        ) {
                                            Text(
                                                text = "BARU",
                                                color = style.onAccent,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = bestScore.toString(),
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    if (medal != Medal.NONE) {
                        Spacer(modifier = Modifier.height(12.dp))
                        MedalBadge(medal = medal)
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = style.accent,
                            contentColor = style.onAccent
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Main Lagi",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onBackToMenu,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.2.dp, Color(0x55FFFFFF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(Icons.Rounded.Home, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Menu Utama",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreColumn(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = Color(0xFFB4C0D8)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
    }
}

@Composable
private fun MedalBadge(medal: Medal) {
    val tint = when (medal) {
        Medal.BRONZE -> Color(0xFFFFB74D)
        Medal.SILVER -> Color(0xFFCFD8DC)
        Medal.GOLD -> Color(0xFFFFD54F)
        Medal.PLATINUM -> Color(0xFF4FC3F7)
        Medal.NONE -> Color.Transparent
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = tint.copy(alpha = 0.18f),
        border = BorderStroke(1.2.dp, tint.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Medali ${medal.label}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}
