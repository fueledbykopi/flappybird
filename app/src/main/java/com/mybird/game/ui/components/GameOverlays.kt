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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    var showImageGuide by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = 20.dp, vertical = 18.dp),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom cartoon menu card: Start + Bird & Background customization
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = CardCream.copy(alpha = 0.96f),
                border = BorderStroke(3.dp, CartoonOutline),
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            .height(54.dp)
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

                    // Header row for customization + Image Size Guide button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kustomisasi Gambar",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF6B563E)
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFECE0C0),
                            border = BorderStroke(1.dp, Color(0xFFCBB483)),
                            modifier = Modifier.clickable { showImageGuide = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = "Panduan Ukuran Gambar",
                                    tint = CartoonOutline,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Panduan Gambar",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CartoonOutline
                                )
                            }
                        }
                    }

                    // Ubah Karakter (Circular Preview + Pilih Foto Burung)
                    CustomizationRowCard(
                        title = "Ubah Karakter",
                        subtitle = if (customBirdBitmap != null) "Foto burung aktif" else "Burung kartun kuning",
                        fitTip = "Pas: 1:1 persegi (min. 256×256 px), objek di tengah & menghadap kanan (PNG/JPG)",
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
                        fitTip = "Pas: Landscape 16:9 / 2:1 (1920×1080 px) untuk scroll mulus, atau Portrait 9:16",
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
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFEBEE),
                            border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = mediaError,
                                color = Color(0xFFC62828),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showImageGuide) {
        ImageGuideDialog(onDismiss = { showImageGuide = false })
    }
}

@Composable
private fun CustomizationRowCard(
    title: String,
    subtitle: String,
    fitTip: String,
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

            Text(
                text = fitTip,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = Color(0xFF5D4B36)
            )

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
private fun ImageGuideDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardCream,
            border = BorderStroke(3.dp, CartoonOutline),
            shadowElevation = 14.dp,
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
                        text = "Panduan Gambar Pas",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = CartoonOutline
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Tutup",
                            tint = CartoonOutline
                        )
                    }
                }

                GuideSectionCard(
                    title = "Karakter Burung",
                    accentColor = PopOrange,
                    bullets = listOf(
                        "Rasio 1:1 (persegi) — otomatis dipotong menjadi lingkaran dengan sayap & paruh.",
                        "Ukuran ideal 512×512 px (minimal 256×256 px) agar tajam.",
                        "Taruh wajah/objek di tengah (beri ruang ~15% dari pinggir) & idealnya menghadap ke kanan.",
                        "Format: PNG (mendukung transparan), JPG, WebP, atau HEIC."
                    )
                )

                GuideSectionCard(
                    title = "Background Game",
                    accentColor = PopTeal,
                    bullets = listOf(
                        "Terbaik: Landscape 16:9 atau 2:1 (mis. 1920×1080 px) untuk efek scroll horizontal yang mulus.",
                        "Alternatif: Portrait 9:16 atau 2:3 (mis. 1080×1650 px) tetap proporsional tanpa gepeng.",
                        "Area aman: 14% bagian bawah tertutup tanah/rumput, taruh pemandangan utama di 86% bagian atas.",
                        "Format: JPG, PNG, WebP, atau HEIC. Pilih gambar yang tidak terlalu ramai agar pipa & skor tetap jelas."
                    )
                )

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PopGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mengerti", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun GuideSectionCard(
    title: String,
    accentColor: Color,
    bullets: List<String>
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF3E6C6),
        border = BorderStroke(1.5.dp, Color(0xFFD2BC8D)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentColor
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            for (bullet in bullets) {
                Text(
                    text = "• $bullet",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = CartoonOutline
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
