package com.mybird.game.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Face
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mybird.game.engine.GamePhase
import com.mybird.game.engine.Medal

private val CartoonOutline = Color(0xFF4A3214)
private val CardCream = Color(0xFFFFF8E7)
private val PopOrange = Color(0xFFFF9800)
private val PopGreen = Color(0xFF4CAF50)
private val PopTeal = Color(0xFF009688)

@Composable
fun TopHudBar(
    phase: GamePhase,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onTogglePause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (phase == GamePhase.PLAYING || phase == GamePhase.PAUSED) {
            HudIconButton(
                onClick = onTogglePause,
                imageVector = if (phase == GamePhase.PAUSED) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                contentDescription = if (phase == GamePhase.PAUSED) "Lanjutkan" else "Jeda"
            )
        } else {
            Spacer(modifier = Modifier.size(44.dp))
        }

        HudIconButton(
            onClick = onToggleSound,
            imageVector = if (soundEnabled) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
            contentDescription = if (soundEnabled) "Matikan Suara" else "Nyalakan Suara"
        )
    }
}

@Composable
private fun HudIconButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String
) {
    Surface(
        shape = CircleShape,
        color = Color(0x70000000),
        border = BorderStroke(1.5.dp, Color(0x77FFFFFF)),
        modifier = Modifier.size(44.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = Color.White
            )
        }
    }
}

@Composable
fun LiveScoreCounter(
    score: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = score.toString(),
        style = TextStyle(
            color = Color.White,
            fontSize = 62.sp,
            fontWeight = FontWeight.Black,
            shadow = Shadow(
                color = Color(0xDD2E1D09),
                offset = Offset(0f, 6f),
                blurRadius = 6f
            )
        ),
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 48.dp)
    )
}

@Composable
fun ReadyTapHint(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xAA2B1B07),
            border = BorderStroke(2.dp, Color(0xFFFFD54F)),
            modifier = Modifier.padding(top = 150.dp)
        ) {
            Text(
                text = "Ketuk layar di mana saja untuk terbang!",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
fun MainMenuOverlay(
    bestScore: Int,
    customBirdBitmap: ImageBitmap?,
    customBackgroundBitmap: ImageBitmap?,
    isLoadingBird: Boolean,
    isLoadingBackground: Boolean,
    mediaError: String?,
    onStartGame: () -> Unit,
    onPickBirdPhoto: () -> Unit,
    onResetBirdPhoto: () -> Unit,
    onPickBackgroundPhoto: () -> Unit,
    onResetBackgroundPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Pop-art logo + High Score header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 26.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFFFD13B),
                    border = BorderStroke(3.dp, CartoonOutline),
                    shadowElevation = 10.dp
                ) {
                    Text(
                        text = "MyBird",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            shadow = Shadow(
                                color = CartoonOutline,
                                offset = Offset(0f, 5f),
                                blurRadius = 2f
                            )
                        ),
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xAA2B1B07),
                    border = BorderStroke(1.5.dp, Color(0xFFFFD54F))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "High Score: $bestScore",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // Bottom cartoon menu card: Start + Bird & Background customization
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = CardCream.copy(alpha = 0.96f),
                border = BorderStroke(3.dp, CartoonOutline),
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Primary CTA: Mulai Game
                    Button(
                        onClick = onStartGame,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PopGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, CartoonOutline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mulai Game",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    // Ubah Karakter (Circular Preview + Pilih Foto Burung)
                    CustomizationRowCard(
                        title = "Ubah Karakter",
                        subtitle = if (customBirdBitmap != null) "Foto burung aktif" else "Burung kartun kuning",
                        buttonLabel = "Pilih Foto Burung",
                        buttonColor = PopOrange,
                        hasCustom = customBirdBitmap != null,
                        isLoading = isLoadingBird,
                        onPick = onPickBirdPhoto,
                        onReset = onResetBirdPhoto,
                        preview = {
                            BirdCirclePreview(
                                bitmap = customBirdBitmap,
                                isLoading = isLoadingBird
                            )
                        }
                    )

                    // Ubah Background (Thumbnail Preview + Pilih Background)
                    CustomizationRowCard(
                        title = "Ubah Background",
                        subtitle = if (customBackgroundBitmap != null) "Background custom aktif" else "Parallax gunung 2D",
                        buttonLabel = "Pilih Background",
                        buttonColor = PopTeal,
                        hasCustom = customBackgroundBitmap != null,
                        isLoading = isLoadingBackground,
                        onPick = onPickBackgroundPhoto,
                        onReset = onResetBackgroundPhoto,
                        preview = {
                            BackgroundThumbnailPreview(
                                bitmap = customBackgroundBitmap,
                                isLoading = isLoadingBackground
                            )
                        }
                    )

                    if (mediaError != null) {
                        Text(
                            text = mediaError,
                            color = Color(0xFFC62828),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomizationRowCard(
    title: String,
    subtitle: String,
    buttonLabel: String,
    buttonColor: Color,
    hasCustom: Boolean,
    isLoading: Boolean,
    onPick: () -> Unit,
    onReset: () -> Unit,
    preview: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF3E6C6),
        border = BorderStroke(1.5.dp, Color(0xFFD2BC8D)),
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
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = CartoonOutline
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF6B563E)
                    )
                }
                if (hasCustom) {
                    IconButton(
                        onClick = onReset,
                        enabled = !isLoading,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Reset ke Default",
                            tint = Color(0xFFC62828)
                        )
                    }
                }
            }

            Button(
                onClick = onPick,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
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
private fun BirdCirclePreview(
    bitmap: ImageBitmap?,
    isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(Color(0xFFFFD13B))
            .border(2.dp, CartoonOutline, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.5.dp,
                color = CartoonOutline
            )

            bitmap != null -> Image(
                bitmap = bitmap,
                contentDescription = "Preview Foto Burung",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            else -> Icon(
                imageVector = Icons.Rounded.Face,
                contentDescription = null,
                tint = CartoonOutline,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun BackgroundThumbnailPreview(
    bitmap: ImageBitmap?,
    isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF4EC0CA), Color(0xFF66BB6A))
                )
            )
            .border(2.dp, CartoonOutline, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.5.dp,
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
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
fun PauseOverlay(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardCream,
            border = BorderStroke(3.dp, CartoonOutline),
            shadowElevation = 12.dp,
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
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = CartoonOutline
                )

                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PopGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lanjutkan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                OutlinedButton(
                    onClick = onRestart,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(2.dp, CartoonOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = CartoonOutline)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Main Lagi", color = CartoonOutline, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onBackToMenu,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(2.dp, CartoonOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(Icons.Rounded.Home, contentDescription = null, tint = CartoonOutline)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kembali ke Menu Utama", color = CartoonOutline, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GameOverOverlay(
    visible: Boolean,
    score: Int,
    bestScore: Int,
    isNewBest: Boolean,
    medal: Medal,
    onRetry: () -> Unit,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.86f),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x72000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = CardCream,
                border = BorderStroke(3.5.dp, CartoonOutline),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFF5722),
                        border = BorderStroke(2.dp, CartoonOutline)
                    ) {
                        Text(
                            text = "GAME OVER",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFFF3E6C6),
                        border = BorderStroke(2.dp, Color(0xFFD6C296)),
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
                                    .width(1.5.dp)
                                    .height(48.dp)
                                    .background(Color(0xFFD6C296))
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "HIGH SCORE",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF7B6242)
                                    )
                                    if (isNewBest) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFE53935)
                                        ) {
                                            Text(
                                                text = "BARU!",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
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
                                    color = CartoonOutline
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
                            containerColor = PopGreen,
                            contentColor = Color.White
                        ),
                        border = BorderStroke(2.dp, CartoonOutline),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Main Lagi",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onBackToMenu,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, CartoonOutline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Rounded.Home, contentDescription = null, tint = CartoonOutline)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kembali ke Menu Utama",
                            color = CartoonOutline,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
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
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF7B6242)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            color = CartoonOutline
        )
    }
}

@Composable
private fun MedalBadge(medal: Medal) {
    val (bg, tint) = when (medal) {
        Medal.BRONZE -> Color(0xFFFFE0B2) to Color(0xFFBF6D2D)
        Medal.SILVER -> Color(0xFFECEFF1) to Color(0xFF78909C)
        Medal.GOLD -> Color(0xFFFFF8E1) to Color(0xFFFFB300)
        Medal.PLATINUM -> Color(0xFFE1F5FE) to Color(0xFF0288D1)
        Medal.NONE -> Color.Transparent to Color.Transparent
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = bg,
        border = BorderStroke(1.5.dp, tint)
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
                color = CartoonOutline,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}
