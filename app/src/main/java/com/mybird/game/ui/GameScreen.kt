package com.mybird.game.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mybird.game.audio.RetroSoundEngine
import com.mybird.game.data.CustomMediaManager
import com.mybird.game.data.GamePreferences
import com.mybird.game.engine.GameConfig
import com.mybird.game.engine.GameEngine
import com.mybird.game.engine.GamePhase
import com.mybird.game.ui.canvas.GameCanvasRenderer
import com.mybird.game.ui.components.GameOverOverlay
import com.mybird.game.ui.components.LiveScoreCounter
import com.mybird.game.ui.components.MainMenuOverlay
import com.mybird.game.ui.components.PauseOverlay
import com.mybird.game.ui.components.ReadyTapHint
import com.mybird.game.ui.components.TopHudBar
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Main interactive screen for MyBird.
 *
 * - 60fps/120fps VSync-locked Coroutines loop (`withFrameNanos`) updating a draw-only `frameTick`
 * - Zero-latency touch handling (`awaitFirstDown`)
 * - Custom bird & background selection via Android Photo Picker (`PickVisualMedia`) + SAF
 * - Auto-loading saved media on `Dispatchers.IO` and auto-pausing on background lifecycle
 */
@Composable
fun GameScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val preferences = remember { GamePreferences(context) }
    val mediaManager = remember { CustomMediaManager(context, preferences) }
    val soundEngine = remember { RetroSoundEngine(context) }
    val engine = remember { GameEngine() }
    val renderer = remember { GameCanvasRenderer() }

    // Discrete UI states (recompose only when values change)
    var phase by remember { mutableStateOf(GamePhase.MENU) }
    var score by remember { mutableIntStateOf(0) }
    var bestScore by remember { mutableIntStateOf(preferences.getBestScore()) }
    var isNewBest by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(preferences.isSoundEnabled()) }

    var customBirdBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var customBackgroundBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLoadingBird by remember { mutableStateOf(false) }
    var isLoadingBackground by remember { mutableStateOf(false) }
    var mediaError by remember { mutableStateOf<String?>(null) }

    // Primitive frame invalidation ticker read ONLY inside Canvas DrawScope
    var frameTick by remember { mutableLongStateOf(0L) }

    // Photo Picker (PickVisualMedia) for Custom Bird Character
    val birdPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isLoadingBird = true
        mediaError = null
        coroutineScope.launch {
            mediaManager.importBirdPhoto(uri)
                .onSuccess { bitmap ->
                    customBirdBitmap = bitmap
                    frameTick++
                }
                .onFailure {
                    mediaError = "Gagal memuat foto burung. Coba pilih foto lain."
                }
            isLoadingBird = false
        }
    }

    // Photo Picker (PickVisualMedia) for Custom Scrolling Background
    val bgPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isLoadingBackground = true
        mediaError = null
        coroutineScope.launch {
            mediaManager.importBackgroundPhoto(uri)
                .onSuccess { bitmap ->
                    customBackgroundBitmap = bitmap
                    frameTick++
                }
                .onFailure {
                    mediaError = "Gagal memuat foto background. Coba pilih foto lain."
                }
            isLoadingBackground = false
        }
    }

    val startNewRun: () -> Unit = {
        engine.resetToReady()
        score = 0
        isNewBest = false
        phase = GamePhase.READY
        frameTick++
    }

    val returnToMainMenu: () -> Unit = {
        engine.resetToMenu()
        score = 0
        isNewBest = false
        phase = GamePhase.MENU
        frameTick++
    }

    // Async initialization of synthesized SFX and persisted custom media on Dispatchers.IO
    LaunchedEffect(Unit) {
        soundEngine.isEnabled = soundEnabled
        launch { soundEngine.initialize() }
        launch {
            val savedBird = mediaManager.loadSavedBirdAvatar()
            if (savedBird != null) {
                customBirdBitmap = savedBird
                frameTick++
            }
        }
        launch {
            val savedBg = mediaManager.loadSavedBackground()
            if (savedBg != null) {
                customBackgroundBitmap = savedBg
                frameTick++
            }
        }
    }

    // Auto-pause when moving to background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && engine.phase == GamePhase.PLAYING) {
                engine.pause()
                phase = engine.phase
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            soundEngine.release()
        }
    }

    // Choreographer / VSync-backed 60fps-120fps Coroutines game loop
    LaunchedEffect(phase) {
        if (phase == GamePhase.PAUSED) return@LaunchedEffect

        var lastFrameNanos = 0L
        while (isActive) {
            withFrameNanos { frameNanos ->
                if (lastFrameNanos == 0L) {
                    lastFrameNanos = frameNanos
                    return@withFrameNanos
                }

                val dt = (frameNanos - lastFrameNanos) / 1_000_000_000f
                lastFrameNanos = frameNanos

                val events = engine.update(dt)
                frameTick = frameNanos

                if ((events and GameConfig.EVENT_SCORED) != 0) {
                    score = engine.score
                    soundEngine.playScore()
                }
                if ((events and GameConfig.EVENT_CRASHED) != 0) {
                    soundEngine.playHit()
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    val finalScore = engine.score
                    if (finalScore > bestScore) {
                        bestScore = finalScore
                        isNewBest = true
                        preferences.saveBestScore(finalScore)
                    } else {
                        isNewBest = false
                    }
                    phase = engine.phase
                }
            }

            if (phase == GamePhase.GAME_OVER && engine.isSettled()) {
                break
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // High-FPS 2D Game Canvas with immediate ACTION_DOWN flap control
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        if (engine.phase == GamePhase.READY || engine.phase == GamePhase.PLAYING) {
                            if (engine.flap()) {
                                soundEngine.playFlap()
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (phase != engine.phase) {
                                    isNewBest = false
                                    score = engine.score
                                    phase = engine.phase
                                }
                            }
                        }
                    }
                }
        ) {
            // Read frameTick strictly inside DrawScope so only Canvas redraws at 60/120fps
            @Suppress("UNUSED_VARIABLE")
            val tick = frameTick

            engine.updateViewport(size.width, size.height)
            with(renderer) {
                render(
                    engine = engine,
                    customBirdBitmap = customBirdBitmap,
                    customBackgroundBitmap = customBackgroundBitmap
                )
            }
        }

        // Top HUD (Pause button in gameplay + Sound toggle)
        TopHudBar(
            phase = phase,
            soundEnabled = soundEnabled,
            onToggleSound = {
                soundEnabled = !soundEnabled
                soundEngine.isEnabled = soundEnabled
                preferences.setSoundEnabled(soundEnabled)
            },
            onTogglePause = {
                when (engine.phase) {
                    GamePhase.PLAYING -> {
                        engine.pause()
                        phase = engine.phase
                    }

                    GamePhase.PAUSED -> {
                        engine.resume()
                        phase = engine.phase
                    }

                    else -> Unit
                }
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Large top-center score during gameplay
        if (phase == GamePhase.PLAYING || phase == GamePhase.PAUSED) {
            LiveScoreCounter(
                score = score,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // Ready tap prompt immediately after pressing "Mulai Game" or "Main Lagi"
        if (phase == GamePhase.READY) {
            ReadyTapHint()
        }

        // Main Menu (Logo, High Score, Start, Custom Bird & Custom Background)
        if (phase == GamePhase.MENU) {
            MainMenuOverlay(
                bestScore = bestScore,
                customBirdBitmap = customBirdBitmap,
                customBackgroundBitmap = customBackgroundBitmap,
                isLoadingBird = isLoadingBird,
                isLoadingBackground = isLoadingBackground,
                mediaError = mediaError,
                onStartGame = startNewRun,
                onPickBirdPhoto = {
                    mediaError = null
                    birdPhotoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onResetBirdPhoto = {
                    mediaError = null
                    coroutineScope.launch {
                        mediaManager.clearBirdPhoto()
                        customBirdBitmap = null
                        frameTick++
                    }
                },
                onPickBackgroundPhoto = {
                    mediaError = null
                    bgPhotoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onResetBackgroundPhoto = {
                    mediaError = null
                    coroutineScope.launch {
                        mediaManager.clearBackgroundPhoto()
                        customBackgroundBitmap = null
                        frameTick++
                    }
                }
            )
        }

        // Pause dialog
        if (phase == GamePhase.PAUSED) {
            PauseOverlay(
                onResume = {
                    engine.resume()
                    phase = engine.phase
                },
                onRestart = startNewRun,
                onBackToMenu = returnToMainMenu
            )
        }

        // Cartoon Game Over Pop-up
        GameOverOverlay(
            visible = phase == GamePhase.GAME_OVER,
            score = score,
            bestScore = bestScore,
            isNewBest = isNewBest,
            medal = engine.currentMedal(),
            onRetry = startNewRun,
            onBackToMenu = returnToMainMenu
        )
    }
}
