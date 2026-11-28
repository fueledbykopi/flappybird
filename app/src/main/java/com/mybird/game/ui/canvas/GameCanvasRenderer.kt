package com.mybird.game.ui.canvas

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.mybird.game.engine.GameConfig
import com.mybird.game.engine.GameEngine
import com.mybird.game.engine.GameTheme
import com.mybird.game.engine.PipePair
import com.mybird.game.engine.SkyTheme
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * High-performance, allocation-free-per-frame 60fps/120fps Jetpack Compose Canvas renderer.
 *
 * Supports 5 rich signature themes:
 * - [GameTheme.BLOK_M] (Default): Jaksel twilight, Sudirman skyline, elevated MRT Jakarta with
 *   gliding Ratangga train, Melawai/Little Tokyo glowing lanterns & neon signs, Merpati Jaksel bird.
 * - [GameTheme.GTA6]: Vice City neon sunset, striped tropical sun, Art Deco hotel skyline,
 *   ocean reflections, swaying palms, Flamingo Vice character.
 * - [GameTheme.CYBERPUNK]: Night City 2077 megabuildings, holographic billboards, flying traffic
 *   light trails, neon rain, cyber plasma columns, Cyber Falcon mech-bird.
 * - [GameTheme.KARHUTLA_BORNEO]: Kalimantan peatland wildfire haze, veiled crimson sun, drifting
 *   smoke plumes, burning treeline & charred ironwood trunks, Enggang Borneo (hornbill) character.
 * - [GameTheme.ORIGINAL]: Classic bright Flappy Bird world with Day/Sunset/Night progression,
 *   glossy green warp pipes, striped grass, and Classic Yellow Bird.
 *
 * Caching strategy: sky [Brush] instances and [Stroke] styles are cached and rebuilt only when
 * theme, sky cycle, or viewport scale changes, eliminating per-frame GC churn on Android 9+.
 */
class GameCanvasRenderer {

    private val reusablePath = Path()

    private val popupTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val popupOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(230, 18, 18, 28)
        style = Paint.Style.STROKE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    // Cached Brushes & Strokes (zero heap allocation inside steady-state frame loop)
    private var cachedSkyTheme: GameTheme? = null
    private var cachedOriginalSky: SkyTheme? = null
    private var cachedGroundYPx: Float = -1f
    private var cachedSkyBrush: Brush = Brush.verticalGradient(BLOK_M_SKY)
    private var cachedHorizonGlowBrush: Brush? = null

    private var cachedScale: Float = -1f
    private var pipeOutlineStroke = Stroke(width = 2.5f)
    private var birdOutlineStroke = Stroke(width = 2.5f)
    private var birdThinStroke = Stroke(width = 2f)
    private var beakStroke = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    private var neonTrimStroke = Stroke(width = 2f, cap = StrokeCap.Round)

    private fun ensureCachedResources(theme: GameTheme, originalSky: SkyTheme, groundYPx: Float, scale: Float) {
        if (cachedSkyTheme != theme ||
            (theme == GameTheme.ORIGINAL && cachedOriginalSky != originalSky) ||
            abs(cachedGroundYPx - groundYPx) > 0.5f
        ) {
            cachedSkyTheme = theme
            cachedOriginalSky = originalSky
            cachedGroundYPx = groundYPx
            val skyColors = when (theme) {
                GameTheme.BLOK_M -> BLOK_M_SKY
                GameTheme.GTA6 -> GTA6_SKY
                GameTheme.CYBERPUNK -> CYBERPUNK_SKY
                GameTheme.KARHUTLA_BORNEO -> BORNEO_SKY
                GameTheme.ORIGINAL -> when (originalSky) {
                    SkyTheme.DAY -> DAY_SKY_COLORS
                    SkyTheme.SUNSET -> SUNSET_SKY_COLORS
                    SkyTheme.NIGHT -> NIGHT_SKY_COLORS
                }
            }
            cachedSkyBrush = Brush.verticalGradient(
                colors = skyColors,
                startY = 0f,
                endY = groundYPx
            )
            cachedHorizonGlowBrush = when (theme) {
                GameTheme.KARHUTLA_BORNEO -> Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0x55FF5722), Color(0xAAFF7043)),
                    startY = groundYPx * 0.62f,
                    endY = groundYPx
                )

                GameTheme.BLOK_M -> Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0x33FF9E40)),
                    startY = groundYPx * 0.68f,
                    endY = groundYPx
                )

                GameTheme.CYBERPUNK -> Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0x38FF2A6D), Color(0x4005D9E8)),
                    startY = groundYPx * 0.66f,
                    endY = groundYPx
                )

                else -> null
            }
        }

        if (abs(cachedScale - scale) > 0.001f) {
            cachedScale = scale
            val pipeW = (2.6f * scale).coerceAtLeast(2f)
            val birdW = (2.5f * scale).coerceAtLeast(1.9f)
            pipeOutlineStroke = Stroke(width = pipeW)
            birdOutlineStroke = Stroke(width = birdW)
            birdThinStroke = Stroke(width = (birdW * 0.78f).coerceAtLeast(1.4f))
            beakStroke = Stroke(width = (birdW * 0.88f).coerceAtLeast(1.6f), cap = StrokeCap.Round, join = StrokeJoin.Round)
            neonTrimStroke = Stroke(width = (2.2f * scale).coerceAtLeast(1.6f), cap = StrokeCap.Round)
        }
    }

    fun DrawScope.render(
        engine: GameEngine,
        theme: GameTheme,
        customBirdBitmap: ImageBitmap?,
        customBackgroundBitmap: ImageBitmap?
    ) {
        val scale = size.height / GameConfig.WORLD_HEIGHT
        val groundYPx = GameConfig.GROUND_Y * scale
        val originalSky = engine.currentSkyTheme()
        ensureCachedResources(theme, originalSky, groundYPx, scale)

        val shakeX = engine.shakeOffsetX * scale
        val shakeY = engine.shakeOffsetY * scale

        withTransform({
            translate(left = shakeX, top = shakeY)
        }) {
            if (customBackgroundBitmap != null) {
                drawScrollingCustomBackground(
                    bitmap = customBackgroundBitmap,
                    scrollDistanceWorld = engine.customBgScrollDistance,
                    scale = scale
                )
            } else {
                drawThemedBackground(
                    engine = engine,
                    theme = theme,
                    originalSky = originalSky,
                    groundYPx = groundYPx,
                    scale = scale
                )
            }

            drawAmbientParticles(engine, theme, scale)
            drawPipes(engine, theme, scale)
            drawGround(engine, theme, groundYPx, scale)
            drawParticles(engine, scale)
            drawBird(engine, theme, customBirdBitmap, scale)
            drawFloatingPopups(engine, scale)
        }

        if (engine.hitFlashAlpha > 0.01f) {
            val flashColor = when (theme) {
                GameTheme.KARHUTLA_BORNEO -> Color(0xFFFFAB40)
                GameTheme.CYBERPUNK -> Color(0xFF05D9E8)
                GameTheme.GTA6 -> Color(0xFFFF80AB)
                else -> Color.White
            }
            drawRect(
                color = flashColor.copy(alpha = engine.hitFlashAlpha.coerceIn(0f, 1f)),
                size = size
            )
        }
    }

    // =========================================================================
    // CUSTOM USER BACKGROUND
    // =========================================================================

    private fun DrawScope.drawScrollingCustomBackground(
        bitmap: ImageBitmap,
        scrollDistanceWorld: Float,
        scale: Float
    ) {
        val skyHeightPx = (GameConfig.GROUND_Y * scale).roundToInt().coerceAtLeast(1)
        val bitmapAspect = bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)
        val tileWidthPx = (skyHeightPx * bitmapAspect).roundToInt().coerceAtLeast(1)

        val rawOffset = (scrollDistanceWorld * scale) % tileWidthPx.toFloat()
        val normalizedOffset = if (rawOffset < 0f) rawOffset + tileWidthPx else rawOffset
        val offsetPx = normalizedOffset.roundToInt()
        var drawX = -offsetPx
        val srcSize = IntSize(bitmap.width, bitmap.height)
        val dstSize = IntSize(tileWidthPx, skyHeightPx)

        while (drawX < size.width) {
            drawImage(
                image = bitmap,
                srcOffset = IntOffset.Zero,
                srcSize = srcSize,
                dstOffset = IntOffset(drawX, 0),
                dstSize = dstSize
            )
            drawX += tileWidthPx
        }

        drawRect(
            color = Color(0x22000000),
            size = Size(size.width, GameConfig.GROUND_Y * scale)
        )
    }

    // =========================================================================
    // THEMED BACKGROUNDS (5 SIGNATURE WORLDS)
    // =========================================================================

    private fun DrawScope.drawThemedBackground(
        engine: GameEngine,
        theme: GameTheme,
        originalSky: SkyTheme,
        groundYPx: Float,
        scale: Float
    ) {
        drawRect(
            brush = cachedSkyBrush,
            size = Size(size.width, groundYPx)
        )
        when (theme) {
            GameTheme.BLOK_M -> drawBlokMScenery(engine, groundYPx, scale)
            GameTheme.GTA6 -> drawGta6Scenery(engine, groundYPx, scale)
            GameTheme.CYBERPUNK -> drawCyberpunkScenery(engine, groundYPx, scale)
            GameTheme.KARHUTLA_BORNEO -> drawBorneoScenery(engine, groundYPx, scale)
            GameTheme.ORIGINAL -> drawOriginalScenery(engine, originalSky, groundYPx, scale)
        }
    }

    // -------------------------------------------------------------------------
    // 1. BLOK M — Sudirman Skyline, MRT Jakarta Viaduct + Train, Little Tokyo
    // -------------------------------------------------------------------------

    private fun DrawScope.drawBlokMScenery(engine: GameEngine, groundY: Float, scale: Float) {
        val elapsed = engine.elapsedTime

        // Soft stars & warm crescent moon over Jakarta Selatan
        drawStarfield(elapsed, groundY * 0.42f, scale, Color(0xFFFFF3E0))
        val moonCenter = Offset(size.width * 0.82f, 120f * scale)
        drawCircle(color = Color(0x28FFE0B2), radius = 56f * scale, center = moonCenter)
        drawCircle(color = Color(0xFFFFF3D1), radius = 26f * scale, center = moonCenter)
        drawCircle(
            color = Color(0xFF1A1A3D),
            radius = 22f * scale,
            center = Offset(moonCenter.x + 10f * scale, moonCenter.y - 5f * scale)
        )

        // Subtle warm city haze clouds
        drawSoftCloudLayer(engine, scale, Color(0x24FFCCBC), Color(0x165E35B1))

        // Horizon Jakarta city glow
        cachedHorizonGlowBrush?.let { glow ->
            drawRect(brush = glow, size = Size(size.width, groundY))
        }

        // Far Layer: Sudirman / Senopati high-rises with lit windows & blinking red beacons
        val farTileW = 360f * scale
        val farOffset = engine.farMountainScrollOffset * scale
        val farCount = ceil(size.width / farTileW).toInt() + 2
        val beaconAlpha = 0.35f + 0.65f * ((sin(elapsed * 4.5f) + 1f) * 0.5f)

        for (i in 0 until farCount) {
            val baseX = i * farTileW - farOffset
            for (b in BLOK_M_FAR_BUILDINGS.indices step 3) {
                val bx = baseX + BLOK_M_FAR_BUILDINGS[b] * scale
                val bw = BLOK_M_FAR_BUILDINGS[b + 1] * scale
                val bh = BLOK_M_FAR_BUILDINGS[b + 2] * scale
                val topY = groundY - bh

                drawRect(
                    color = Color(0xFF191D3A),
                    topLeft = Offset(bx, topY),
                    size = Size(bw, bh)
                )
                // Top rooftop neon trim
                if ((b / 3) % 2 == 0) {
                    drawRect(
                        color = Color(0x8826C6DA),
                        topLeft = Offset(bx, topY),
                        size = Size(bw, 3f * scale)
                    )
                }
                // Lit office window rows
                val winW = 5f * scale
                val winH = 5f * scale
                val stepX = 11f * scale
                val stepY = 14f * scale
                var wy = topY + 14f * scale
                var row = 0
                while (wy < groundY - 170f * scale) {
                    var wx = bx + 7f * scale
                    var col = 0
                    while (wx + winW < bx + bw - 5f * scale) {
                        if ((row * 7 + col * 3 + b) % 4 != 0) {
                            val winColor = if ((row + col) % 5 == 0) Color(0x884DD0E1) else Color(0x99FFD180)
                            drawRect(color = winColor, topLeft = Offset(wx, wy), size = Size(winW, winH))
                        }
                        wx += stepX
                        col++
                    }
                    wy += stepY
                    row++
                }
                // Spire + blinking aviation beacon on tall towers
                if (bh > 255f * scale) {
                    val spireX = bx + bw * 0.5f
                    drawLine(
                        color = Color(0xFF39416B),
                        start = Offset(spireX, topY),
                        end = Offset(spireX, topY - 26f * scale),
                        strokeWidth = 2f * scale
                    )
                    drawCircle(
                        color = Color(0xFFFF3D00).copy(alpha = beaconAlpha),
                        radius = 3.2f * scale,
                        center = Offset(spireX, topY - 26f * scale)
                    )
                }
            }
        }

        // Mid Layer: MRT Jakarta Elevated Viaduct + Gliding Ratangga Train
        val viaductY = groundY - 176f * scale
        val pillarTileW = 160f * scale
        val pillarOffset = (engine.midSceneryScrollOffset * scale) % pillarTileW
        val pillarCount = ceil(size.width / pillarTileW).toInt() + 2
        for (i in 0 until pillarCount) {
            val px = i * pillarTileW - pillarOffset
            // T-pillar supporting MRT elevated track
            drawRect(
                color = Color(0xFF28314A),
                topLeft = Offset(px + 22f * scale, viaductY + 14f * scale),
                size = Size(20f * scale, groundY - viaductY)
            )
            drawRect(
                color = Color(0xFF3A4766),
                topLeft = Offset(px + 12f * scale, viaductY + 12f * scale),
                size = Size(40f * scale, 10f * scale)
            )
        }

        // Gliding MRT Jakarta Ratangga Train (white + blue livery)
        val trainCycleW = size.width + 420f * scale
        val trainX = ((elapsed * 105f * scale) % trainCycleW) - 280f * scale
        val trainH = 26f * scale
        val trainTop = viaductY - trainH + 2f * scale
        val carW = 74f * scale
        val carGap = 4f * scale
        for (car in 0 until 3) {
            val cx = trainX + car * (carW + carGap)
            // White sleek MRT car body
            drawRoundRect(
                color = Color(0xFFE8EEF5),
                topLeft = Offset(cx, trainTop),
                size = Size(carW, trainH),
                cornerRadius = CornerRadius(6f * scale, 6f * scale)
            )
            // Signature MRT Jakarta blue stripe
            drawRect(
                color = Color(0xFF0077C8),
                topLeft = Offset(cx + 2f * scale, trainTop + trainH * 0.62f),
                size = Size(carW - 4f * scale, 5.5f * scale)
            )
            // Warm lit passenger windows
            for (w in 0 until 4) {
                drawRoundRect(
                    color = Color(0xFFFFE082),
                    topLeft = Offset(cx + (8f + w * 16f) * scale, trainTop + 5f * scale),
                    size = Size(12f * scale, 8.5f * scale),
                    cornerRadius = CornerRadius(2f * scale, 2f * scale)
                )
            }
        }
        // Train headlight beam
        val headX = trainX + 3 * (carW + carGap) - carGap
        drawCircle(color = Color(0x66FFF59D), radius = 14f * scale, center = Offset(headX + 2f * scale, trainTop + trainH * 0.65f))
        drawCircle(color = Color.White, radius = 3.5f * scale, center = Offset(headX - 1f * scale, trainTop + trainH * 0.65f))

        // MRT Concrete Viaduct Beam + Guardrail
        drawRect(
            color = Color(0xFF394663),
            topLeft = Offset(0f, viaductY),
            size = Size(size.width, 16f * scale)
        )
        drawRect(
            color = Color(0xFF54668C),
            topLeft = Offset(0f, viaductY),
            size = Size(size.width, 3.5f * scale)
        )
        // Subtle electric-blue MRT accent line along viaduct
        drawRect(
            color = Color(0x9926C6DA),
            topLeft = Offset(0f, viaductY + 11f * scale),
            size = Size(size.width, 2.5f * scale)
        )

        // Near Layer: Blok M / Melawai / Little Tokyo shophouses, cafés & glowing lanterns
        val nearTileW = 280f * scale
        val nearOffset = engine.nearMountainScrollOffset * scale
        val nearCount = ceil(size.width / nearTileW).toInt() + 2
        for (i in 0 until nearCount) {
            val bx = i * nearTileW - nearOffset
            // Ruko 1: Little Tokyo Izakaya / Coffee Shop
            val r1H = 114f * scale
            drawRect(color = Color(0xFF1A182D), topLeft = Offset(bx, groundY - r1H), size = Size(88f * scale, r1H))
            // Warm glowing café awning
            drawRect(color = Color(0xFFFF7043), topLeft = Offset(bx + 4f * scale, groundY - 52f * scale), size = Size(80f * scale, 10f * scale))
            // Warm café storefront window
            drawRect(color = Color(0xCCFFE082), topLeft = Offset(bx + 10f * scale, groundY - 38f * scale), size = Size(68f * scale, 26f * scale))
            // Vertical neon sign ("Little Tokyo" vibe)
            drawRoundRect(
                color = Color(0xFFEC407A),
                topLeft = Offset(bx + 68f * scale, groundY - r1H + 8f * scale),
                size = Size(14f * scale, 46f * scale),
                cornerRadius = CornerRadius(3f * scale, 3f * scale)
            )
            // Hanging red-cream Japanese paper lanterns (chochin)
            for (l in 0 until 3) {
                val lx = bx + (16f + l * 20f) * scale
                val ly = groundY - 62f * scale
                drawCircle(color = Color(0x44FFAB40), radius = 10f * scale, center = Offset(lx, ly))
                drawOval(
                    color = if (l % 2 == 0) Color(0xFFFF5252) else Color(0xFFFFF3E0),
                    topLeft = Offset(lx - 4.5f * scale, ly - 6f * scale),
                    size = Size(9f * scale, 12f * scale)
                )
            }

            // Ruko 2: Blok M Plaza / Indie Vinyl & Matcha Bar
            val r2X = bx + 94f * scale
            val r2H = 136f * scale
            drawRect(color = Color(0xFF14182B), topLeft = Offset(r2X, groundY - r2H), size = Size(96f * scale, r2H))
            // Teal neon horizontal signage bar
            drawRoundRect(
                color = Color(0xFF26C6DA),
                topLeft = Offset(r2X + 10f * scale, groundY - r2H + 14f * scale),
                size = Size(76f * scale, 14f * scale),
                cornerRadius = CornerRadius(4f * scale, 4f * scale)
            )
            drawRect(
                color = Color(0x99FFF59D),
                topLeft = Offset(r2X + 12f * scale, groundY - 48f * scale),
                size = Size(30f * scale, 34f * scale)
            )
            drawRect(
                color = Color(0x88FFAB40),
                topLeft = Offset(r2X + 50f * scale, groundY - 48f * scale),
                size = Size(34f * scale, 34f * scale)
            )

            // Ruko 3: Blok M Kopi Kiosk + Street Lamp
            val r3X = bx + 196f * scale
            val r3H = 92f * scale
            drawRect(color = Color(0xFF1D1E34), topLeft = Offset(r3X, groundY - r3H), size = Size(76f * scale, r3H))
            drawRect(color = Color(0xFFFFB74D), topLeft = Offset(r3X + 8f * scale, groundY - r3H + 12f * scale), size = Size(60f * scale, 9f * scale))
            drawRect(color = Color(0xAAFFE0B2), topLeft = Offset(r3X + 12f * scale, groundY - 38f * scale), size = Size(52f * scale, 28f * scale))

            // Warm Jaksel Streetlamp glow
            val lampX = bx + 274f * scale
            drawLine(
                color = Color(0xFF3E4863),
                start = Offset(lampX, groundY),
                end = Offset(lampX, groundY - 122f * scale),
                strokeWidth = 3f * scale
            )
            drawCircle(color = Color(0x38FFCA28), radius = 26f * scale, center = Offset(lampX, groundY - 122f * scale))
            drawCircle(color = Color(0xFFFFF59D), radius = 5f * scale, center = Offset(lampX, groundY - 122f * scale))
        }
    }

    // -------------------------------------------------------------------------
    // 2. GTA 6 — Vice City Neon Sunset, Striped Retro Sun, Art Deco & Palms
    // -------------------------------------------------------------------------

    private fun DrawScope.drawGta6Scenery(engine: GameEngine, groundY: Float, scale: Float) {
        val elapsed = engine.elapsedTime
        val sunCenter = Offset(size.width * 0.56f, groundY - 250f * scale)
        val sunR = 112f * scale

        // Subtle high-sky sunset stars
        drawStarfield(elapsed, groundY * 0.25f, scale, Color(0xFFFFD6F0))

        // Huge Vice City retro sun halo & disc
        drawCircle(color = Color(0x28FF4D94), radius = sunR * 1.65f, center = sunCenter)
        drawCircle(color = Color(0x40FF9E40), radius = sunR * 1.28f, center = sunCenter)
        drawCircle(color = Color(0xFFFFD166), radius = sunR, center = sunCenter)
        drawCircle(color = Color(0xFFFF758F), radius = sunR * 0.92f, center = Offset(sunCenter.x, sunCenter.y + sunR * 0.14f))

        // Classic synthwave horizontal scan-stripes across lower half of the sun
        val stripeColor = Color(0xFFE0487B)
        for (s in 0 until 5) {
            val sy = sunCenter.y + (16f + s * 19f) * scale
            val sh = (3.5f + s * 2.4f) * scale
            drawRect(
                color = stripeColor,
                topLeft = Offset(sunCenter.x - sunR, sy),
                size = Size(sunR * 2f, sh)
            )
        }

        // Warm magenta-gold sunset clouds
        drawSoftCloudLayer(engine, scale, Color(0x55FFB3C6), Color(0x337B1FA2))

        // Far Layer: Miami / Vice City Art Deco Hotel Skyline
        val farTileW = 360f * scale
        val farOffset = engine.farMountainScrollOffset * scale
        val farCount = ceil(size.width / farTileW).toInt() + 2
        val oceanTopY = groundY - 92f * scale

        for (i in 0 until farCount) {
            val bx = i * farTileW - farOffset
            for (b in GTA6_BUILDINGS.indices step 3) {
                val x = bx + GTA6_BUILDINGS[b] * scale
                val w = GTA6_BUILDINGS[b + 1] * scale
                val h = GTA6_BUILDINGS[b + 2] * scale
                val topY = oceanTopY - h

                drawRect(color = Color(0xFF2B1045), topLeft = Offset(x, topY), size = Size(w, h))
                // Art Deco stepped crown
                drawRect(
                    color = Color(0xFF3A155C),
                    topLeft = Offset(x + w * 0.2f, topY - 12f * scale),
                    size = Size(w * 0.6f, 12f * scale)
                )
                // Glowing neon pink or cyan hotel roofline
                val neonColor = if ((b / 3) % 2 == 0) Color(0xFFFF4D94) else Color(0xFF00F5D4)
                drawRect(color = neonColor, topLeft = Offset(x, topY), size = Size(w, 3.5f * scale))
                // Vertical neon Art Deco sign strip
                drawRect(
                    color = neonColor.copy(alpha = 0.85f),
                    topLeft = Offset(x + w * 0.45f, topY + 12f * scale),
                    size = Size(4f * scale, (h * 0.45f).coerceAtLeast(16f * scale))
                )
            }
        }

        // Mid Layer: Shimmering Vice Bay Ocean Water
        drawRect(
            color = Color(0xFF2A0F4E),
            topLeft = Offset(0f, oceanTopY),
            size = Size(size.width, groundY - oceanTopY)
        )
        // Animated sunset reflection shimmer bands on water
        for (r in 0 until 7) {
            val ry = oceanTopY + (10f + r * 11f) * scale
            val rw = (150f - r * 14f + sin(elapsed * 3.5f + r) * 18f) * scale
            val rx = sunCenter.x - rw * 0.5f + sin(elapsed * 2.2f + r * 1.3f) * 10f * scale
            drawRoundRect(
                color = if (r % 2 == 0) Color(0x88FFD166) else Color(0x66FF4D94),
                topLeft = Offset(rx, ry),
                size = Size(rw.coerceAtLeast(24f * scale), 4f * scale),
                cornerRadius = CornerRadius(2f * scale, 2f * scale)
            )
        }

        // Near Layer: Iconic Silhouetted Tropical Palm Trees swaying in Ocean Breeze
        val palmTileW = 280f * scale
        val palmOffset = engine.nearMountainScrollOffset * scale
        val palmCount = ceil(size.width / palmTileW).toInt() + 2
        val sway = sin(elapsed * 2.3f) * 6f * scale
        for (i in 0 until palmCount) {
            val px = i * palmTileW - palmOffset
            drawPalmTree(px + 44f * scale, groundY, 178f * scale, 18f * scale + sway, scale)
            drawPalmTree(px + 188f * scale, groundY, 142f * scale, -14f * scale + sway * 0.8f, scale)
        }
    }

    private fun DrawScope.drawPalmTree(baseX: Float, groundY: Float, height: Float, curveX: Float, scale: Float) {
        val topX = baseX + curveX
        val topY = groundY - height
        val trunkColor = Color(0xFF170829)

        // Curved palm trunk
        reusablePath.reset()
        reusablePath.moveTo(baseX - 6f * scale, groundY)
        reusablePath.quadraticTo(baseX + curveX * 0.35f, groundY - height * 0.55f, topX - 2.5f * scale, topY)
        reusablePath.lineTo(topX + 2.5f * scale, topY)
        reusablePath.quadraticTo(baseX + curveX * 0.35f + 6f * scale, groundY - height * 0.55f, baseX + 7f * scale, groundY)
        reusablePath.close()
        drawPath(path = reusablePath, color = trunkColor)

        // 6 graceful drooping palm fronds
        for (f in PALM_FROND_ANGLES.indices) {
            val angRad = PALM_FROND_ANGLES[f]
            val len = (48f + (f % 2) * 14f) * scale
            val endX = topX + cos(angRad) * len
            val endY = topY + sin(angRad) * len + 16f * scale
            val ctrlX = topX + cos(angRad) * len * 0.55f
            val ctrlY = topY + sin(angRad) * len * 0.35f - 14f * scale

            reusablePath.reset()
            reusablePath.moveTo(topX, topY)
            reusablePath.quadraticTo(ctrlX, ctrlY, endX, endY)
            reusablePath.quadraticTo(ctrlX, ctrlY + 11f * scale, topX, topY + 3f * scale)
            reusablePath.close()
            drawPath(path = reusablePath, color = trunkColor)
        }
    }

    // -------------------------------------------------------------------------
    // 3. CYBERPUNK — Night City Megabuildings, Holograms, Flying Traffic & Rain
    // -------------------------------------------------------------------------

    private fun DrawScope.drawCyberpunkScenery(engine: GameEngine, groundY: Float, scale: Float) {
        val elapsed = engine.elapsedTime

        // Distant orbital cyber-moon + subtle sweeping searchlights
        val moonCenter = Offset(size.width * 0.78f, 130f * scale)
        drawCircle(color = Color(0x2205D9E8), radius = 70f * scale, center = moonCenter)
        drawCircle(color = Color(0x55B8F8FF), radius = 38f * scale, center = moonCenter)
        drawCircle(color = Color(0xFF0A1026), radius = 34f * scale, center = Offset(moonCenter.x - 6f * scale, moonCenter.y - 4f * scale))

        // Sweeping Night City megacorp laser searchlights
        val beamSweep1 = sin(elapsed * 0.7f) * 120f * scale
        val beamSweep2 = cos(elapsed * 0.55f) * 110f * scale
        drawSearchlightBeam(size.width * 0.22f, groundY, size.width * 0.34f + beamSweep1, 0f, 52f * scale, Color(0x1805D9E8))
        drawSearchlightBeam(size.width * 0.76f, groundY, size.width * 0.62f + beamSweep2, 0f, 56f * scale, Color(0x18FF2A6D))

        cachedHorizonGlowBrush?.let { glow ->
            drawRect(brush = glow, size = Size(size.width, groundY))
        }

        // Far Layer: Towering Arcology Megabuildings
        val farTileW = 360f * scale
        val farOffset = engine.farMountainScrollOffset * scale
        val farCount = ceil(size.width / farTileW).toInt() + 2
        for (i in 0 until farCount) {
            val bx = i * farTileW - farOffset
            for (b in CYBER_BUILDINGS.indices step 3) {
                val x = bx + CYBER_BUILDINGS[b] * scale
                val w = CYBER_BUILDINGS[b + 1] * scale
                val h = CYBER_BUILDINGS[b + 2] * scale
                val topY = groundY - h

                // Sloped arcology silhouette
                reusablePath.reset()
                reusablePath.moveTo(x - 6f * scale, groundY)
                reusablePath.lineTo(x + 4f * scale, topY)
                reusablePath.lineTo(x + w - 4f * scale, topY)
                reusablePath.lineTo(x + w + 6f * scale, groundY)
                reusablePath.close()
                drawPath(path = reusablePath, color = Color(0xFF0C1024))

                // Glowing vertical edge trim
                val trimColor = when ((b / 3) % 3) {
                    0 -> Color(0x9905D9E8)
                    1 -> Color(0x99FF2A6D)
                    else -> Color(0x99FCEE0A)
                }
                drawLine(
                    color = trimColor,
                    start = Offset(x + 6f * scale, topY + 6f * scale),
                    end = Offset(x + 6f * scale, groundY - 40f * scale),
                    strokeWidth = 2f * scale
                )

                // Cyber window grid strips
                var wy = topY + 18f * scale
                var row = 0
                while (wy < groundY - 90f * scale) {
                    if ((row + b) % 2 == 0) {
                        drawRect(
                            color = if ((row + b) % 4 == 0) Color(0x6605D9E8) else Color(0x55FCEE0A),
                            topLeft = Offset(x + 12f * scale, wy),
                            size = Size((w - 24f * scale).coerceAtLeast(6f * scale), 3f * scale)
                        )
                    }
                    wy += 14f * scale
                    row++
                }
            }
        }

        // Mid Layer: Animated Flying Spinner Car Traffic Trails
        for (lane in 0 until 4) {
            val ly = groundY - (145f + lane * 68f) * scale
            val dir = if (lane % 2 == 0) 1f else -1f
            val laneSpan = size.width + 260f * scale
            val rawX = (elapsed * (165f + lane * 45f) * scale * dir + lane * 190f * scale) % laneSpan
            val cx = if (rawX < 0f) rawX + laneSpan - 130f * scale else rawX - 130f * scale
            val trailColor = if (dir > 0f) Color(0xCC05D9E8) else Color(0xCCFF2A6D)
            drawRoundRect(
                color = trailColor.copy(alpha = 0.35f),
                topLeft = Offset(cx - 24f * scale, ly - 1.5f * scale),
                size = Size(52f * scale, 4f * scale),
                cornerRadius = CornerRadius(2f * scale, 2f * scale)
            )
            drawCircle(color = Color.White, radius = 2.4f * scale, center = Offset(if (dir > 0f) cx + 26f * scale else cx - 22f * scale, ly + 0.5f * scale))
        }

        // Near Layer: Glowing Holographic Billboards & Neon Structures
        val nearTileW = 280f * scale
        val nearOffset = engine.nearMountainScrollOffset * scale
        val nearCount = ceil(size.width / nearTileW).toInt() + 2
        val pulse = 0.72f + 0.28f * sin(elapsed * 5.2f)
        for (i in 0 until nearCount) {
            val bx = i * nearTileW - nearOffset
            // Low cyber structure
            drawRect(color = Color(0xFF10152B), topLeft = Offset(bx, groundY - 94f * scale), size = Size(108f * scale, 94f * scale))
            drawRect(color = Color(0xFF0A0F22), topLeft = Offset(bx + 124f * scale, groundY - 122f * scale), size = Size(134f * scale, 122f * scale))

            // Hologram 1: Cyber Yellow floating screen
            val h1X = bx + 12f * scale
            val h1Y = groundY - 156f * scale
            drawRoundRect(
                color = Color(0x38FCEE0A),
                topLeft = Offset(h1X, h1Y),
                size = Size(84f * scale, 44f * scale),
                cornerRadius = CornerRadius(5f * scale, 5f * scale)
            )
            drawRoundRect(
                color = Color(0xFFFCEE0A).copy(alpha = pulse),
                topLeft = Offset(h1X, h1Y),
                size = Size(84f * scale, 44f * scale),
                cornerRadius = CornerRadius(5f * scale, 5f * scale),
                style = neonTrimStroke
            )
            drawRect(color = Color(0xCCFCEE0A), topLeft = Offset(h1X + 10f * scale, h1Y + 12f * scale), size = Size(48f * scale, 5f * scale))
            drawRect(color = Color(0x9905D9E8), topLeft = Offset(h1X + 10f * scale, h1Y + 24f * scale), size = Size(62f * scale, 4f * scale))

            // Hologram 2: Hot Pink vertical cyber-sign
            val h2X = bx + 150f * scale
            val h2Y = groundY - 196f * scale
            drawRoundRect(
                color = Color(0x38FF2A6D),
                topLeft = Offset(h2X, h2Y),
                size = Size(32f * scale, 66f * scale),
                cornerRadius = CornerRadius(4f * scale, 4f * scale)
            )
            drawRoundRect(
                color = Color(0xFFFF2A6D).copy(alpha = pulse),
                topLeft = Offset(h2X, h2Y),
                size = Size(32f * scale, 66f * scale),
                cornerRadius = CornerRadius(4f * scale, 4f * scale),
                style = neonTrimStroke
            )
            for (bar in 0 until 4) {
                drawRect(
                    color = Color(0xDDFFFFFF),
                    topLeft = Offset(h2X + 7f * scale, h2Y + (11f + bar * 13f) * scale),
                    size = Size(18f * scale, 4f * scale)
                )
            }
        }
    }

    private fun DrawScope.drawSearchlightBeam(
        baseX: Float,
        baseY: Float,
        topX: Float,
        topY: Float,
        topHalfWidth: Float,
        color: Color
    ) {
        reusablePath.reset()
        reusablePath.moveTo(baseX - 6f, baseY)
        reusablePath.lineTo(topX - topHalfWidth, topY)
        reusablePath.lineTo(topX + topHalfWidth, topY)
        reusablePath.lineTo(baseX + 6f, baseY)
        reusablePath.close()
        drawPath(path = reusablePath, color = color)
    }

    // -------------------------------------------------------------------------
    // 4. KARHUTLA BORNEO — Peatland Smoke Haze, Veiled Crimson Sun & Rainforest
    // -------------------------------------------------------------------------

    private fun DrawScope.drawBorneoScenery(engine: GameEngine, groundY: Float, scale: Float) {
        val elapsed = engine.elapsedTime

        // Veiled blood-orange sun dimmed by thick Kalimantan smoke haze
        val sunPos = Offset(size.width * 0.74f, 185f * scale)
        drawCircle(color = Color(0x28FF6D00), radius = 104f * scale, center = sunPos)
        drawCircle(color = Color(0x48FF8F00), radius = 68f * scale, center = sunPos)
        drawCircle(color = Color(0xFFFF7043), radius = 40f * scale, center = sunPos)
        drawCircle(color = Color(0xFFFFAB40), radius = 26f * scale, center = sunPos)

        // Billowing layered smoke plumes (instead of bright clouds)
        drawSoftCloudLayer(engine, scale, Color(0x523E2723), Color(0x38BF360C))

        // Pulsing wildfire glow along the horizon
        val fireFlicker = 0.80f + 0.20f * sin(elapsed * 7.5f)
        cachedHorizonGlowBrush?.let { glow ->
            drawRect(brush = glow, alpha = fireFlicker, size = Size(size.width, groundY))
        }

        // Far Layer: Hazy Kalimantan Hills & Distant Burning Ridge
        val farTileW = 360f * scale
        val farOffset = engine.farMountainScrollOffset * scale
        val farCount = ceil(size.width / farTileW).toInt() + 2
        for (i in 0 until farCount) {
            val bx = i * farTileW - farOffset
            drawMountainPeak(bx, groundY, 240f * scale, 185f * scale, Color(0xFF4A2013), Color(0xFF35160D), null)
            drawMountainPeak(bx + 145f * scale, groundY, 250f * scale, 220f * scale, Color(0xFF522314), Color(0xFF3A180E), null)
        }

        // Subtle glowing wildfire flame tongues along distant treeline
        val flameTileW = 120f * scale
        val flameOffset = (engine.midSceneryScrollOffset * scale) % flameTileW
        val flameCount = ceil(size.width / flameTileW).toInt() + 2
        for (i in 0 until flameCount) {
            val fx = i * flameTileW - flameOffset
            val flickerH = (26f + sin(elapsed * 8f + i * 1.9f) * 9f) * scale
            drawCircle(
                color = Color(0x55FF5722),
                radius = flickerH,
                center = Offset(fx + 35f * scale, groundY - 22f * scale)
            )
            drawCircle(
                color = Color(0x44FFAB00),
                radius = flickerH * 0.6f,
                center = Offset(fx + 82f * scale, groundY - 18f * scale)
            )
        }

        // Near Layer: Borneo Rainforest Dipterocarp Silhouettes & Charred Trunks
        val nearTileW = 280f * scale
        val nearOffset = engine.nearMountainScrollOffset * scale
        val nearCount = ceil(size.width / nearTileW).toInt() + 2
        for (i in 0 until nearCount) {
            val bx = i * nearTileW - nearOffset
            // Tall Borneo rainforest tree silhouette
            drawBorneoTree(bx + 36f * scale, groundY, 155f * scale, isCharred = false, scale = scale)
            // Charred skeletal burnt tree trunk with glowing ember base
            drawBorneoTree(bx + 128f * scale, groundY, 128f * scale, isCharred = true, scale = scale)
            // Dense rainforest canopy tree
            drawBorneoTree(bx + 218f * scale, groundY, 170f * scale, isCharred = false, scale = scale)
        }

        // Low atmospheric smoke veil right above the peatland floor
        drawRect(
            color = Color(0x28FF8F00),
            topLeft = Offset(0f, groundY - 46f * scale),
            size = Size(size.width, 46f * scale)
        )
    }

    private fun DrawScope.drawBorneoTree(x: Float, groundY: Float, height: Float, isCharred: Boolean, scale: Float) {
        val darkColor = Color(0xFF1D0F0A)
        val trunkW = 12f * scale
        // Main tree trunk
        drawRect(
            color = darkColor,
            topLeft = Offset(x - trunkW * 0.5f, groundY - height),
            size = Size(trunkW, height)
        )
        if (isCharred) {
            // Jagged burnt branches
            drawLine(darkColor, Offset(x, groundY - height * 0.72f), Offset(x - 24f * scale, groundY - height * 0.92f), 4f * scale)
            drawLine(darkColor, Offset(x, groundY - height * 0.58f), Offset(x + 26f * scale, groundY - height * 0.78f), 4f * scale)
            drawLine(darkColor, Offset(x, groundY - height * 0.40f), Offset(x - 18f * scale, groundY - height * 0.54f), 3.5f * scale)
            // Glowing smoldering ember at the burnt fork
            drawCircle(color = Color(0xCCFF6D00), radius = 4.5f * scale, center = Offset(x, groundY - height * 0.72f))
            drawCircle(color = Color(0xAAFF3D00), radius = 3.5f * scale, center = Offset(x + 4f * scale, groundY - 14f * scale))
        } else {
            // Layered tropical rainforest umbrella canopy
            val canopyY = groundY - height
            drawOval(color = darkColor, topLeft = Offset(x - 42f * scale, canopyY - 18f * scale), size = Size(84f * scale, 38f * scale))
            drawOval(color = darkColor, topLeft = Offset(x - 26f * scale, canopyY - 32f * scale), size = Size(56f * scale, 30f * scale))
            drawOval(color = Color(0xFF28140D), topLeft = Offset(x - 36f * scale, canopyY + 4f * scale), size = Size(72f * scale, 26f * scale))
        }
    }

    // -------------------------------------------------------------------------
    // 5. ORIGINAL — Classic Flappy Bird Day/Sunset/Night Mountains & Clouds
    // -------------------------------------------------------------------------

    private fun DrawScope.drawOriginalScenery(
        engine: GameEngine,
        skyTheme: SkyTheme,
        groundY: Float,
        scale: Float
    ) {
        val elapsed = engine.elapsedTime
        val orbCenter = Offset(size.width * 0.80f, 135f * scale)
        when (skyTheme) {
            SkyTheme.DAY -> {
                drawCircle(color = Color(0x55FFF59D), radius = 66f * scale, center = orbCenter)
                drawCircle(color = Color(0xFFFFF176), radius = 40f * scale, center = orbCenter)
            }

            SkyTheme.SUNSET -> {
                val sunPos = Offset(size.width * 0.76f, 225f * scale)
                drawCircle(color = Color(0x44FFAB40), radius = 84f * scale, center = sunPos)
                drawCircle(color = Color(0xFFFFCA28), radius = 48f * scale, center = sunPos)
            }

            SkyTheme.NIGHT -> {
                drawStarfield(elapsed, groundY * 0.58f, scale, Color.White)
                drawCircle(color = Color(0x33E3F2FD), radius = 58f * scale, center = orbCenter)
                drawCircle(color = Color(0xFFFFFDE7), radius = 34f * scale, center = orbCenter)
                drawCircle(
                    color = Color(0xFF1A2954),
                    radius = 28f * scale,
                    center = Offset(orbCenter.x + 12f * scale, orbCenter.y - 6f * scale)
                )
            }
        }

        val cloudColor = when (skyTheme) {
            SkyTheme.DAY -> Color(0xECFFFFFF)
            SkyTheme.SUNSET -> Color(0xCCFFE0B2)
            SkyTheme.NIGHT -> Color(0x3890CAF9)
        }
        val cloudShadow = when (skyTheme) {
            SkyTheme.DAY -> Color(0x44B2DFDB)
            SkyTheme.SUNSET -> Color(0x44FF8A65)
            SkyTheme.NIGHT -> Color(0x221E88E5)
        }
        drawSoftCloudLayer(engine, scale, cloudColor, cloudShadow)

        val farLight = when (skyTheme) {
            SkyTheme.DAY -> Color(0xFF76C2B5)
            SkyTheme.SUNSET -> Color(0xFFA25A7B)
            SkyTheme.NIGHT -> Color(0xFF213A66)
        }
        val farDark = when (skyTheme) {
            SkyTheme.DAY -> Color(0xFF5FA89E)
            SkyTheme.SUNSET -> Color(0xFF834466)
            SkyTheme.NIGHT -> Color(0xFF172B4F)
        }
        val snowCap = when (skyTheme) {
            SkyTheme.DAY -> Color(0xFFEAF9FA)
            SkyTheme.SUNSET -> Color(0xFFFFD8B0)
            SkyTheme.NIGHT -> Color(0xFF8FB8E8)
        }
        val nearLight = when (skyTheme) {
            SkyTheme.DAY -> Color(0xFF5FB878)
            SkyTheme.SUNSET -> Color(0xFF6B3E5E)
            SkyTheme.NIGHT -> Color(0xFF142847)
        }
        val nearDark = when (skyTheme) {
            SkyTheme.DAY -> Color(0xFF479A60)
            SkyTheme.SUNSET -> Color(0xFF512B4A)
            SkyTheme.NIGHT -> Color(0xFF0D1D36)
        }

        val farTileW = 360f * scale
        val farOffset = engine.farMountainScrollOffset * scale
        val farCount = ceil(size.width / farTileW).toInt() + 2
        for (i in 0 until farCount) {
            val bx = i * farTileW - farOffset
            drawMountainPeak(bx, groundY, 210f * scale, 225f * scale, farLight, farDark, snowCap)
            drawMountainPeak(bx + 150f * scale, groundY, 230f * scale, 265f * scale, farLight, farDark, snowCap)
        }

        val nearTileW = 280f * scale
        val nearOffset = engine.nearMountainScrollOffset * scale
        val nearCount = ceil(size.width / nearTileW).toInt() + 2
        for (i in 0 until nearCount) {
            val bx = i * nearTileW - nearOffset
            drawMountainPeak(bx - 20f * scale, groundY, 175f * scale, 130f * scale, nearLight, nearDark, null)
            drawMountainPeak(bx + 120f * scale, groundY, 195f * scale, 155f * scale, nearLight, nearDark, null)
        }
    }

    private fun DrawScope.drawStarfield(elapsed: Float, maxY: Float, scale: Float, tint: Color) {
        for (i in STAR_POSITIONS.indices step 2) {
            val sx = STAR_POSITIONS[i] * size.width
            val sy = STAR_POSITIONS[i + 1] * maxY
            val twinkle = 0.40f + 0.60f * ((sin(elapsed * 3.2f + i) + 1f) * 0.5f)
            drawCircle(
                color = tint.copy(alpha = twinkle),
                radius = (1.6f + (i % 3) * 0.8f) * scale,
                center = Offset(sx, sy)
            )
        }
    }

    private fun DrawScope.drawSoftCloudLayer(
        engine: GameEngine,
        scale: Float,
        cloudColor: Color,
        cloudShadow: Color
    ) {
        for (cloud in engine.clouds) {
            val cx = cloud.x * scale
            val cy = cloud.y * scale
            val w = cloud.width * scale
            val h = w * 0.34f
            val corner = CornerRadius(h * 0.35f, h * 0.35f)

            drawRoundRect(color = cloudShadow, topLeft = Offset(cx, cy + h * 0.14f), size = Size(w, h), cornerRadius = corner)
            drawRoundRect(color = cloudColor, topLeft = Offset(cx, cy), size = Size(w, h), cornerRadius = corner)
            drawRoundRect(color = cloudColor, topLeft = Offset(cx + w * 0.16f, cy - h * 0.42f), size = Size(w * 0.42f, h * 0.72f), cornerRadius = corner)
            drawRoundRect(color = cloudColor, topLeft = Offset(cx + w * 0.46f, cy - h * 0.62f), size = Size(w * 0.36f, h * 0.90f), cornerRadius = corner)
        }
    }

    private fun DrawScope.drawMountainPeak(
        leftX: Float,
        baseY: Float,
        width: Float,
        height: Float,
        lightColor: Color,
        darkColor: Color,
        snowColor: Color?
    ) {
        val peakX = leftX + width * 0.5f
        val peakY = baseY - height

        reusablePath.reset()
        reusablePath.moveTo(leftX, baseY)
        reusablePath.lineTo(peakX, peakY)
        reusablePath.lineTo(peakX + width * 0.08f, baseY)
        reusablePath.close()
        drawPath(path = reusablePath, color = lightColor)

        reusablePath.reset()
        reusablePath.moveTo(peakX, peakY)
        reusablePath.lineTo(leftX + width, baseY)
        reusablePath.lineTo(peakX + width * 0.04f, baseY)
        reusablePath.close()
        drawPath(path = reusablePath, color = darkColor)

        if (snowColor != null) {
            val capY = peakY + height * 0.24f
            val halfSpan = width * 0.12f
            reusablePath.reset()
            reusablePath.moveTo(peakX, peakY)
            reusablePath.lineTo(peakX - halfSpan, capY)
            reusablePath.lineTo(peakX - halfSpan * 0.25f, capY - height * 0.04f)
            reusablePath.lineTo(peakX + halfSpan * 0.25f, capY + height * 0.02f)
            reusablePath.lineTo(peakX + halfSpan, capY)
            reusablePath.close()
            drawPath(path = reusablePath, color = snowColor)
        }
    }

    // =========================================================================
    // AMBIENT PARTICLES (Embers, Neon Rain, Bokeh, Sunset Glints)
    // =========================================================================

    private fun DrawScope.drawAmbientParticles(engine: GameEngine, theme: GameTheme, scale: Float) {
        val elapsed = engine.elapsedTime
        for (ap in engine.ambientParticles) {
            val px = ap.x * scale
            val py = ap.y * scale
            val pulse = 0.45f + 0.55f * ((sin(elapsed * 4f + ap.phase) + 1f) * 0.5f)
            when (theme) {
                GameTheme.KARHUTLA_BORNEO -> {
                    val emberColor = when (ap.variant) {
                        0 -> Color(0xFFFF6D00)
                        1 -> Color(0xFFFFAB00)
                        else -> Color(0xFFFF3D00)
                    }
                    drawCircle(color = emberColor.copy(alpha = 0.25f * pulse), radius = ap.size * 2.2f * scale, center = Offset(px, py))
                    drawCircle(color = emberColor.copy(alpha = 0.85f * pulse), radius = ap.size * scale, center = Offset(px, py))
                }

                GameTheme.CYBERPUNK -> {
                    // Crisp slanted neon raindrop streak
                    val rainColor = if (ap.variant == 0) Color(0x6605D9E8) else Color(0x44FF2A6D)
                    drawLine(
                        color = rainColor,
                        start = Offset(px, py),
                        end = Offset(px - 4.5f * scale, py + 18f * scale),
                        strokeWidth = (ap.size * 0.7f * scale).coerceAtLeast(1.1f)
                    )
                }

                GameTheme.BLOK_M -> {
                    val bokehColor = when (ap.variant) {
                        0 -> Color(0xFFFFCA28)
                        1 -> Color(0xFF4DD0E1)
                        else -> Color(0xFFFF6EC7)
                    }
                    drawCircle(color = bokehColor.copy(alpha = 0.22f * pulse), radius = ap.size * 1.9f * scale, center = Offset(px, py))
                    drawCircle(color = bokehColor.copy(alpha = 0.55f * pulse), radius = ap.size * 0.7f * scale, center = Offset(px, py))
                }

                GameTheme.GTA6 -> {
                    val glintColor = if (ap.variant == 0) Color(0xFFFFD166) else Color(0xFFFF6EC7)
                    drawCircle(color = glintColor.copy(alpha = 0.35f * pulse), radius = ap.size * scale, center = Offset(px, py))
                }

                GameTheme.ORIGINAL -> {
                    drawCircle(color = Color.White.copy(alpha = 0.20f * pulse), radius = ap.size * scale, center = Offset(px, py))
                }
            }
        }
    }

    // =========================================================================
    // THEMED OBSTACLES / PIPES
    // =========================================================================

    private fun DrawScope.drawPipes(engine: GameEngine, theme: GameTheme, scale: Float) {
        for (pipe in engine.pipes) {
            if (!pipe.active) continue
            drawPipePair(pipe, theme, scale)
        }
    }

    private fun DrawScope.drawPipePair(pipe: PipePair, theme: GameTheme, scale: Float) {
        val pipeW = GameConfig.PIPE_WIDTH * scale
        val lipH = GameConfig.PIPE_LIP_HEIGHT * scale
        val overhang = GameConfig.PIPE_LIP_OVERHANG * scale
        val x = pipe.x * scale
        val groundY = GameConfig.GROUND_Y * scale
        val outlineWidth = (2.6f * scale).coerceAtLeast(2f)

        val topBottomY = pipe.topPipeBottomY * scale
        val bottomTopY = pipe.bottomPipeTopY * scale

        if (topBottomY > 0f) {
            drawThemedPipeSegment(
                theme = theme,
                left = x,
                top = -outlineWidth,
                width = pipeW,
                height = (topBottomY - lipH + outlineWidth).coerceAtLeast(0f),
                outlineWidth = outlineWidth,
                isCap = false,
                cornerPx = 0f,
                scale = scale
            )
            drawThemedPipeSegment(
                theme = theme,
                left = x - overhang,
                top = topBottomY - lipH,
                width = pipeW + overhang * 2f,
                height = lipH,
                outlineWidth = outlineWidth,
                isCap = true,
                cornerPx = 5f * scale,
                scale = scale
            )
        }

        val bottomBodyTop = bottomTopY + lipH
        if (bottomBodyTop < groundY) {
            drawThemedPipeSegment(
                theme = theme,
                left = x,
                top = bottomBodyTop,
                width = pipeW,
                height = (groundY - bottomBodyTop + outlineWidth).coerceAtLeast(0f),
                outlineWidth = outlineWidth,
                isCap = false,
                cornerPx = 0f,
                scale = scale
            )
        }
        drawThemedPipeSegment(
            theme = theme,
            left = x - overhang,
            top = bottomTopY,
            width = pipeW + overhang * 2f,
            height = lipH,
            outlineWidth = outlineWidth,
            isCap = true,
            cornerPx = 5f * scale,
            scale = scale
        )
    }

    private fun DrawScope.drawThemedPipeSegment(
        theme: GameTheme,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        outlineWidth: Float,
        isCap: Boolean,
        cornerPx: Float,
        scale: Float
    ) {
        if (height <= 0f || width <= 0f) return
        val corner = CornerRadius(cornerPx, cornerPx)
        val innerH = (height - outlineWidth).coerceAtLeast(0f)
        val innerTop = top + outlineWidth * 0.5f

        val baseColor: Color
        val highlightColor: Color
        val accentStripeColor: Color
        val shadowColor: Color
        val outlineColor: Color

        when (theme) {
            GameTheme.BLOK_M -> {
                baseColor = Color(0xFF3E4C66)
                highlightColor = Color(0xFF607394)
                accentStripeColor = if (isCap) Color(0xFFFFCA28) else Color(0xFF26C6DA)
                shadowColor = Color(0xFF232C3F)
                outlineColor = Color(0xFF121724)
            }

            GameTheme.GTA6 -> {
                baseColor = Color(0xFF19B5BA)
                highlightColor = Color(0xFF6AF2EE)
                accentStripeColor = if (isCap) Color(0xFFFF4D94) else Color(0xFFFF80AB)
                shadowColor = Color(0xFF116D7D)
                outlineColor = Color(0xFF230A38)
            }

            GameTheme.CYBERPUNK -> {
                baseColor = Color(0xFF1C2338)
                highlightColor = Color(0xFF323E5E)
                accentStripeColor = if (isCap) Color(0xFFFCEE0A) else Color(0xFF05D9E8)
                shadowColor = Color(0xFF0E1322)
                outlineColor = Color(0xFF05D9E8)
            }

            GameTheme.KARHUTLA_BORNEO -> {
                baseColor = Color(0xFF3A2218)
                highlightColor = Color(0xFF573526)
                accentStripeColor = if (isCap) Color(0xFFFF7043) else Color(0xFFFF8F00)
                shadowColor = Color(0xFF21120C)
                outlineColor = Color(0xFF140905)
            }

            GameTheme.ORIGINAL -> {
                baseColor = PIPE_BASE_GREEN
                highlightColor = PIPE_HIGHLIGHT_GREEN
                accentStripeColor = Color(0x66FFFFFF)
                shadowColor = PIPE_SHADOW_GREEN
                outlineColor = PIPE_OUTLINE
            }
        }

        // Main pipe fill
        drawRoundRect(color = baseColor, topLeft = Offset(left, top), size = Size(width, height), cornerRadius = corner)

        // Left specular / structural highlight
        drawRect(
            color = highlightColor,
            topLeft = Offset(left + width * 0.12f, innerTop),
            size = Size(width * 0.16f, innerH)
        )

        // Right depth shadow
        drawRect(
            color = shadowColor,
            topLeft = Offset(left + width * 0.76f, innerTop),
            size = Size(width * 0.22f, innerH)
        )

        // Theme-specific signature details on pipes
        when (theme) {
            GameTheme.BLOK_M -> {
                if (isCap) {
                    // Iconic Jakarta MRT yellow transit stripe on cap
                    drawRect(
                        color = Color(0xFFFFC107),
                        topLeft = Offset(left + 4f * scale, top + height * 0.58f),
                        size = Size(width - 8f * scale, height * 0.24f)
                    )
                } else {
                    // Glowing vertical Blok M teal neon strip
                    drawRect(
                        color = accentStripeColor,
                        topLeft = Offset(left + width * 0.45f, innerTop),
                        size = Size(width * 0.09f, innerH)
                    )
                }
            }

            GameTheme.GTA6 -> {
                if (isCap) {
                    drawRect(
                        color = Color(0xFFFF4D94),
                        topLeft = Offset(left + 3f * scale, top + height * 0.36f),
                        size = Size(width - 6f * scale, height * 0.26f)
                    )
                } else {
                    drawRect(
                        color = Color(0xBBFFFFFF),
                        topLeft = Offset(left + width * 0.16f, innerTop),
                        size = Size(width * 0.05f, innerH)
                    )
                    drawRect(
                        color = Color(0xCCFF4D94),
                        topLeft = Offset(left + width * 0.64f, innerTop),
                        size = Size(width * 0.07f, innerH)
                    )
                }
            }

            GameTheme.CYBERPUNK -> {
                if (isCap) {
                    drawRect(
                        color = Color(0xFFFCEE0A),
                        topLeft = Offset(left + 5f * scale, top + height * 0.34f),
                        size = Size(width - 10f * scale, height * 0.30f)
                    )
                } else {
                    // Dual plasma energy core strips
                    drawRect(
                        color = Color(0xCC05D9E8),
                        topLeft = Offset(left + width * 0.44f, innerTop),
                        size = Size(width * 0.11f, innerH)
                    )
                    drawRect(
                        color = Color(0xBBFFFFFF),
                        topLeft = Offset(left + width * 0.48f, innerTop),
                        size = Size(width * 0.03f, innerH)
                    )
                }
            }

            GameTheme.KARHUTLA_BORNEO -> {
                if (isCap) {
                    // Smoldering ember band on charred ironwood collar
                    drawRect(
                        color = Color(0xDDFF6D00),
                        topLeft = Offset(left + 5f * scale, top + height * 0.44f),
                        size = Size(width - 10f * scale, height * 0.22f)
                    )
                } else {
                    // Charred bark grooves + glowing ember fissure vein
                    drawRect(
                        color = Color(0xFF1E100A),
                        topLeft = Offset(left + width * 0.36f, innerTop),
                        size = Size(width * 0.06f, innerH)
                    )
                    drawRect(
                        color = Color(0xBBFF6D00),
                        topLeft = Offset(left + width * 0.56f, innerTop),
                        size = Size(width * 0.05f, innerH)
                    )
                }
            }

            GameTheme.ORIGINAL -> {
                drawRect(
                    color = accentStripeColor,
                    topLeft = Offset(left + width * 0.15f, innerTop),
                    size = Size(width * 0.05f, innerH)
                )
            }
        }

        // Crisp outline
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(left, top),
            size = Size(width, height),
            cornerRadius = corner,
            style = pipeOutlineStroke
        )
    }

    // =========================================================================
    // THEMED GROUND
    // =========================================================================

    private fun DrawScope.drawGround(engine: GameEngine, theme: GameTheme, groundY: Float, scale: Float) {
        val groundHeight = size.height - groundY
        val topBandH = 26f * scale

        val bodyColor: Color
        val bodyShadow: Color
        val stripBase: Color
        val stripChevron: Color
        val borderColor: Color

        when (theme) {
            GameTheme.BLOK_M -> {
                // Jakarta asphalt & Blok M yellow-black kerb
                bodyColor = Color(0xFF1B2030)
                bodyShadow = Color(0xFF121622)
                stripBase = Color(0xFFFFC107)
                stripChevron = Color(0xFF212633)
                borderColor = Color(0xFF0E121D)
            }

            GameTheme.GTA6 -> {
                // Vice City Ocean Drive neon boardwalk
                bodyColor = Color(0xFF1C0C33)
                bodyShadow = Color(0xFF120724)
                stripBase = Color(0xFFFF4D94)
                stripChevron = Color(0xFF00F5D4)
                borderColor = Color(0xFFFFD166)
            }

            GameTheme.CYBERPUNK -> {
                // Night City cyber deck & yellow-black hazard edge
                bodyColor = Color(0xFF0B0F1E)
                bodyShadow = Color(0xFF070914)
                stripBase = Color(0xFFFCEE0A)
                stripChevron = Color(0xFF151B2E)
                borderColor = Color(0xFF05D9E8)
            }

            GameTheme.KARHUTLA_BORNEO -> {
                // Scorched Kalimantan peatland & smoldering ember ash
                bodyColor = Color(0xFF1C100B)
                bodyShadow = Color(0xFF120906)
                stripBase = Color(0xFF3E2216)
                stripChevron = Color(0xFFFF6D00)
                borderColor = Color(0xFFFF8F00)
            }

            GameTheme.ORIGINAL -> {
                bodyColor = GROUND_SOIL
                bodyShadow = GROUND_SOIL_DARK
                stripBase = GRASS_LIGHT
                stripChevron = GRASS_DARK
                borderColor = PIPE_OUTLINE
            }
        }

        drawRect(
            color = bodyColor,
            topLeft = Offset(0f, groundY + topBandH),
            size = Size(size.width, (groundHeight - topBandH).coerceAtLeast(0f))
        )
        drawRect(
            color = bodyShadow,
            topLeft = Offset(0f, groundY + topBandH),
            size = Size(size.width, 10f * scale)
        )
        drawRect(
            color = stripBase,
            topLeft = Offset(0f, groundY),
            size = Size(size.width, topBandH)
        )

        val tileW = 64f * scale
        val scrollPx = engine.groundScrollOffset * scale
        val tileCount = ceil(size.width / tileW).toInt() + 2
        for (i in 0 until tileCount) {
            val sx = i * tileW - scrollPx
            reusablePath.reset()
            reusablePath.moveTo(sx, groundY + topBandH)
            reusablePath.lineTo(sx + tileW * 0.35f, groundY)
            reusablePath.lineTo(sx + tileW * 0.68f, groundY)
            reusablePath.lineTo(sx + tileW * 0.33f, groundY + topBandH)
            reusablePath.close()
            drawPath(path = reusablePath, color = stripChevron)

            // Extra subtle road dashes / circuit lines on lower ground
            when (theme) {
                GameTheme.BLOK_M -> {
                    drawRect(
                        color = Color(0x44FFFFFF),
                        topLeft = Offset(sx + 10f * scale, groundY + topBandH + 26f * scale),
                        size = Size(32f * scale, 5f * scale)
                    )
                }

                GameTheme.CYBERPUNK -> {
                    drawLine(
                        color = Color(0x4405D9E8),
                        start = Offset(sx + tileW * 0.5f, groundY + topBandH),
                        end = Offset(sx + tileW * 0.1f, size.height),
                        strokeWidth = 1.6f * scale
                    )
                }

                GameTheme.KARHUTLA_BORNEO -> {
                    drawRect(
                        color = Color(0x55FF5722),
                        topLeft = Offset(sx + 14f * scale, groundY + topBandH + 22f * scale),
                        size = Size(22f * scale, 3f * scale)
                    )
                }

                GameTheme.GTA6 -> {
                    drawRect(
                        color = Color(0x55FF4D94),
                        topLeft = Offset(sx + 8f * scale, groundY + topBandH + 24f * scale),
                        size = Size(34f * scale, 4f * scale)
                    )
                }

                GameTheme.ORIGINAL -> Unit
            }
        }

        val stroke = (2.5f * scale).coerceAtLeast(2f)
        drawLine(color = borderColor, start = Offset(0f, groundY), end = Offset(size.width, groundY), strokeWidth = stroke)
        drawLine(color = borderColor, start = Offset(0f, groundY + topBandH), end = Offset(size.width, groundY + topBandH), strokeWidth = stroke)
    }

    // =========================================================================
    // PARTICLES & FLOATING SCORE POPUPS
    // =========================================================================

    private fun DrawScope.drawParticles(engine: GameEngine, scale: Float) {
        for (p in engine.particles) {
            if (!p.active) continue
            drawCircle(
                color = Color(p.colorArgb).copy(alpha = p.alpha),
                radius = p.size * scale * (0.45f + 0.55f * p.alpha),
                center = Offset(p.x * scale, p.y * scale)
            )
        }
    }

    private fun DrawScope.drawFloatingPopups(engine: GameEngine, scale: Float) {
        val textSizePx = 28f * scale
        popupTextPaint.textSize = textSizePx
        popupOutlinePaint.textSize = textSizePx
        popupOutlinePaint.strokeWidth = 5f * scale

        val native = drawContext.canvas.nativeCanvas
        for (pop in engine.popups) {
            if (!pop.active) continue
            val alphaInt = (pop.alpha * 255f).roundToInt().coerceIn(0, 255)
            popupTextPaint.alpha = alphaInt
            popupOutlinePaint.alpha = alphaInt
            val px = pop.x * scale
            val py = pop.y * scale
            native.drawText(pop.text, px, py, popupOutlinePaint)
            native.drawText(pop.text, px, py, popupTextPaint)
        }
    }

    // =========================================================================
    // THEMED CHARACTERS (5 UNIQUE ANIMATED CHARACTERS + CUSTOM PHOTO)
    // =========================================================================

    private fun DrawScope.drawBird(
        engine: GameEngine,
        theme: GameTheme,
        customBirdBitmap: ImageBitmap?,
        scale: Float
    ) {
        val bird = engine.bird
        val cx = bird.x * scale
        val cy = bird.y * scale
        val r = bird.radius * scale

        val heightAboveGround = (GameConfig.GROUND_Y - bird.y).coerceAtLeast(0f)
        val shadowFactor = (1f - heightAboveGround / 520f).coerceIn(0.15f, 0.75f)
        drawOval(
            color = Color(0x33000000).copy(alpha = 0.25f * shadowFactor),
            topLeft = Offset(cx - r * shadowFactor, (GameConfig.GROUND_Y + 4f) * scale),
            size = Size(r * 2f * shadowFactor, 10f * scale * shadowFactor)
        )

        withTransform({
            rotate(degrees = bird.rotationDeg, pivot = Offset(cx, cy))
        }) {
            if (customBirdBitmap != null) {
                drawCustomPhotoBird(
                    bitmap = customBirdBitmap,
                    theme = theme,
                    cx = cx,
                    cy = cy,
                    radius = r,
                    wingPhase = bird.wingPhase,
                    beakOpen = bird.beakOpen
                )
            } else {
                drawThemeVectorCharacter(
                    theme = theme,
                    cx = cx,
                    cy = cy,
                    r = r,
                    wingPhase = bird.wingPhase,
                    beakOpen = bird.beakOpen,
                    path = reusablePath,
                    outlineStroke = birdOutlineStroke,
                    thinStroke = birdThinStroke,
                    beakStroke = beakStroke
                )
            }
        }
    }

    private fun DrawScope.drawCustomPhotoBird(
        bitmap: ImageBitmap,
        theme: GameTheme,
        cx: Float,
        cy: Float,
        radius: Float,
        wingPhase: Float,
        beakOpen: Float
    ) {
        val diameter = (radius * 2.12f).roundToInt().coerceAtLeast(1)
        val left = (cx - diameter * 0.5f).roundToInt()
        val top = (cy - diameter * 0.5f).roundToInt()

        val wingTint = when (theme) {
            GameTheme.BLOK_M -> Color(0xFFFFE0B2)
            GameTheme.GTA6 -> Color(0xFFFF80AB)
            GameTheme.CYBERPUNK -> Color(0xFF05D9E8)
            GameTheme.KARHUTLA_BORNEO -> Color(0xFFFFAB40)
            GameTheme.ORIGINAL -> Color(0xFFFFF8DC)
        }

        // 1. Animated outer wing
        val wingAngle = sin(wingPhase) * 28f
        val wingPivot = Offset(cx - radius * 0.85f, cy + radius * 0.05f)
        withTransform({
            rotate(degrees = wingAngle, pivot = wingPivot)
        }) {
            drawOval(
                color = wingTint,
                topLeft = Offset(cx - radius * 1.52f, cy - radius * 0.26f),
                size = Size(radius * 0.92f, radius * 0.56f)
            )
            drawOval(
                color = BIRD_OUTLINE,
                topLeft = Offset(cx - radius * 1.52f, cy - radius * 0.26f),
                size = Size(radius * 0.92f, radius * 0.56f),
                style = birdThinStroke
            )
        }

        // 2. Animated outer beak
        drawAnimatedBeak(
            baseX = cx + radius * 0.92f,
            centerY = cy + radius * 0.06f,
            beakLength = radius * 0.62f,
            beakHalfHeight = radius * 0.26f,
            beakOpen = beakOpen,
            topColor = BIRD_BEAK_TOP,
            bottomColor = BIRD_BEAK_BOTTOM,
            outlineColor = BIRD_OUTLINE,
            path = reusablePath,
            stroke = beakStroke
        )

        // 3. Circular cropped custom photo
        drawImage(
            image = bitmap,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(bitmap.width, bitmap.height),
            dstOffset = IntOffset(left, top),
            dstSize = IntSize(diameter, diameter)
        )
    }

    companion object {
        private val BLOK_M_SKY = listOf(Color(0xFF0E1228), Color(0xFF1D1B40), Color(0xFF3F2656), Color(0xFF7B3E52))
        private val GTA6_SKY = listOf(Color(0xFF1A0938), Color(0xFF7B1B72), Color(0xFFE6486E), Color(0xFFFFB85C))
        private val CYBERPUNK_SKY = listOf(Color(0xFF040612), Color(0xFF0C1230), Color(0xFF1E0B3B), Color(0xFF3E0A42))
        private val BORNEO_SKY = listOf(Color(0xFF28150E), Color(0xFF5D2612), Color(0xFFB34011), Color(0xFFF27D16))

        private val DAY_SKY_COLORS = listOf(Color(0xFF34B3C8), Color(0xFF7BE0EA), Color(0xFFC8F7F2))
        private val SUNSET_SKY_COLORS = listOf(Color(0xFF3C2B69), Color(0xFFD15F6B), Color(0xFFFFB56B))
        private val NIGHT_SKY_COLORS = listOf(Color(0xFF0E1733), Color(0xFF1A2954), Color(0xFF2B4578))

        private val PIPE_BASE_GREEN = Color(0xFF5FC743)
        private val PIPE_HIGHLIGHT_GREEN = Color(0xFF9CE86F)
        private val PIPE_SHADOW_GREEN = Color(0xFF3D9628)
        private val PIPE_OUTLINE = Color(0xFF2B3D1A)

        private val GRASS_LIGHT = Color(0xFF74CF4F)
        private val GRASS_DARK = Color(0xFF55AD36)
        private val GROUND_SOIL = Color(0xFFDFD595)
        private val GROUND_SOIL_DARK = Color(0xFFC9BC77)

        private val BIRD_YELLOW = Color(0xFFFFD13B)
        private val BIRD_BELLY = Color(0xFFFFF2B2)
        private val BIRD_BEAK_TOP = Color(0xFFFF7A29)
        private val BIRD_BEAK_BOTTOM = Color(0xFFE65100)
        private val BIRD_OUTLINE = Color(0xFF3E2912)

        private val STAR_POSITIONS = floatArrayOf(
            0.09f, 0.14f, 0.21f, 0.26f, 0.34f, 0.11f,
            0.47f, 0.22f, 0.58f, 0.15f, 0.67f, 0.30f,
            0.88f, 0.18f, 0.16f, 0.42f, 0.74f, 0.45f,
            0.41f, 0.38f, 0.91f, 0.36f, 0.05f, 0.31f
        )

        // (offsetX, width, height) triples in world units
        private val BLOK_M_FAR_BUILDINGS = floatArrayOf(
            8f, 54f, 240f,
            68f, 66f, 305f,
            142f, 48f, 225f,
            198f, 72f, 330f,
            278f, 62f, 260f
        )

        private val GTA6_BUILDINGS = floatArrayOf(
            12f, 58f, 155f,
            78f, 64f, 205f,
            152f, 54f, 140f,
            214f, 74f, 225f,
            296f, 52f, 165f
        )

        private val CYBER_BUILDINGS = floatArrayOf(
            6f, 64f, 310f,
            78f, 76f, 370f,
            162f, 54f, 265f,
            224f, 82f, 390f,
            312f, 44f, 285f
        )

        private val PALM_FROND_ANGLES = floatArrayOf(
            -2.85f, -2.35f, -1.75f, -1.05f, -0.45f, 0.10f
        )
    }
}

/**
 * Draws the signature animated vector character for any [GameTheme].
 * Shared between the live 60/120fps [GameCanvasRenderer] and lightweight menu preview badges.
 */
fun DrawScope.drawThemeVectorCharacter(
    theme: GameTheme,
    cx: Float,
    cy: Float,
    r: Float,
    wingPhase: Float,
    beakOpen: Float,
    path: Path,
    outlineStroke: Stroke,
    thinStroke: Stroke,
    beakStroke: Stroke
) {
    when (theme) {
        GameTheme.BLOK_M -> drawBlokMPigeon(cx, cy, r, wingPhase, beakOpen, path, outlineStroke, thinStroke, beakStroke)
        GameTheme.GTA6 -> drawViceFlamingo(cx, cy, r, wingPhase, beakOpen, path, outlineStroke, thinStroke, beakStroke)
        GameTheme.CYBERPUNK -> drawCyberFalcon(cx, cy, r, wingPhase, beakOpen, path, outlineStroke, thinStroke, beakStroke)
        GameTheme.KARHUTLA_BORNEO -> drawBorneoHornbill(cx, cy, r, wingPhase, beakOpen, path, outlineStroke, thinStroke, beakStroke)
        GameTheme.ORIGINAL -> drawOriginalYellowBird(cx, cy, r, wingPhase, beakOpen, path, outlineStroke, thinStroke, beakStroke)
    }
}

/**
 * BLOK M — Merpati Jaksel: Cool urban slate-white pigeon with iridescent teal collar,
 * trendy mustard/terracotta Jaksel scarf, and sleek dark sunglasses!
 */
private fun DrawScope.drawBlokMPigeon(
    cx: Float,
    cy: Float,
    r: Float,
    wingPhase: Float,
    beakOpen: Float,
    path: Path,
    outlineStroke: Stroke,
    thinStroke: Stroke,
    beakStroke: Stroke
) {
    val outline = Color(0xFF1A2233)

    // Animated amber-orange beak
    drawAnimatedBeak(
        baseX = cx + r * 0.82f,
        centerY = cy + r * 0.08f,
        beakLength = r * 0.58f,
        beakHalfHeight = r * 0.22f,
        beakOpen = beakOpen,
        topColor = Color(0xFFFFB74D),
        bottomColor = Color(0xFFE68A00),
        outlineColor = outline,
        path = path,
        stroke = beakStroke
    )

    // Sleek urban pigeon body (pearl-slate)
    drawOval(
        color = Color(0xFFDCE4EC),
        topLeft = Offset(cx - r * 1.06f, cy - r * 0.94f),
        size = Size(r * 2.12f, r * 1.88f)
    )
    // Iridescent emerald-teal pigeon neck/chest sheen
    drawOval(
        color = Color(0xFF26A69A),
        topLeft = Offset(cx - r * 0.15f, cy - r * 0.25f),
        size = Size(r * 1.12f, r * 0.92f)
    )
    // Trendy Blok M terracotta-amber beanie/cap crest on top of head
    drawArc(
        color = Color(0xFFE07A5F),
        startAngle = 195f,
        sweepAngle = 145f,
        useCenter = true,
        topLeft = Offset(cx - r * 0.72f, cy - r * 1.06f),
        size = Size(r * 1.48f, r * 1.05f)
    )
    drawOval(
        color = outline,
        topLeft = Offset(cx - r * 1.06f, cy - r * 0.94f),
        size = Size(r * 2.12f, r * 1.88f),
        style = outlineStroke
    )

    // Animated flapping slate wing with classic pigeon wing-bars
    val wingAngle = sin(wingPhase) * 24f
    withTransform({
        rotate(degrees = wingAngle, pivot = Offset(cx - r * 0.35f, cy + r * 0.14f))
    }) {
        drawOval(
            color = Color(0xFF90A4AE),
            topLeft = Offset(cx - r * 0.94f, cy - r * 0.12f),
            size = Size(r * 1.02f, r * 0.62f)
        )
        drawLine(
            color = Color(0xFF263238),
            start = Offset(cx - r * 0.72f, cy + r * 0.08f),
            end = Offset(cx - r * 0.16f, cy + r * 0.08f),
            strokeWidth = (r * 0.10f).coerceAtLeast(2f)
        )
        drawOval(
            color = outline,
            topLeft = Offset(cx - r * 0.94f, cy - r * 0.12f),
            size = Size(r * 1.02f, r * 0.62f),
            style = thinStroke
        )
    }

    // Cool Jaksel retro sunglasses with warm sunset-amber lens glare
    val glassLeft = cx + r * 0.04f
    val glassTop = cy - r * 0.46f
    drawRoundRect(
        color = Color(0xFF111622),
        topLeft = Offset(glassLeft, glassTop),
        size = Size(r * 0.78f, r * 0.44f),
        cornerRadius = CornerRadius(r * 0.14f, r * 0.14f)
    )
    drawLine(
        color = Color(0xFFFFCA28),
        start = Offset(glassLeft + r * 0.16f, glassTop + r * 0.34f),
        end = Offset(glassLeft + r * 0.42f, glassTop + r * 0.10f),
        strokeWidth = (r * 0.10f).coerceAtLeast(1.8f)
    )
    drawRoundRect(
        color = Color(0xFFFFD180),
        topLeft = Offset(glassLeft, glassTop),
        size = Size(r * 0.78f, r * 0.44f),
        cornerRadius = CornerRadius(r * 0.14f, r * 0.14f),
        style = thinStroke
    )
    // Sunglasses temple arm
    drawLine(
        color = Color(0xFF111622),
        start = Offset(glassLeft, glassTop + r * 0.16f),
        end = Offset(cx - r * 0.48f, glassTop + r * 0.12f),
        strokeWidth = (r * 0.10f).coerceAtLeast(1.8f)
    )
}

/**
 * GTA 6 — Flamingo Vice: Vibrant neon-pink Vice City flamingo-bird with swept head crest,
 * gold-rimmed aviator sunglasses, and curved two-tone tropical beak!
 */
private fun DrawScope.drawViceFlamingo(
    cx: Float,
    cy: Float,
    r: Float,
    wingPhase: Float,
    beakOpen: Float,
    path: Path,
    outlineStroke: Stroke,
    thinStroke: Stroke,
    beakStroke: Stroke
) {
    val outline = Color(0xFF3A0B3D)

    // Swept-back neon-pink flamingo crest feathers
    path.reset()
    path.moveTo(cx - r * 0.12f, cy - r * 0.85f)
    path.lineTo(cx - r * 0.78f, cy - r * 1.25f)
    path.lineTo(cx - r * 0.32f, cy - r * 0.62f)
    path.close()
    drawPath(path = path, color = Color(0xFFFF4D94))
    drawPath(path = path, color = outline, style = thinStroke)

    // Two-tone flamingo beak (creamy pink base + signature black tip)
    drawAnimatedBeak(
        baseX = cx + r * 0.82f,
        centerY = cy + r * 0.12f,
        beakLength = r * 0.68f,
        beakHalfHeight = r * 0.25f,
        beakOpen = beakOpen,
        topColor = Color(0xFFFFCDD2),
        bottomColor = Color(0xFF1E1028),
        outlineColor = outline,
        path = path,
        stroke = beakStroke
    )

    // Hot-pink flamingo body
    drawOval(
        color = Color(0xFFFF5C9E),
        topLeft = Offset(cx - r * 1.06f, cy - r * 0.94f),
        size = Size(r * 2.12f, r * 1.88f)
    )
    // Soft coral-pink belly highlight
    drawOval(
        color = Color(0xFFFFA6C9),
        topLeft = Offset(cx - r * 0.56f, cy + r * 0.14f),
        size = Size(r * 1.24f, r * 0.68f)
    )
    drawOval(
        color = outline,
        topLeft = Offset(cx - r * 1.06f, cy - r * 0.94f),
        size = Size(r * 2.12f, r * 1.88f),
        style = outlineStroke
    )

    // Animated tropical magenta-coral wing
    val wingAngle = sin(wingPhase) * 26f
    withTransform({
        rotate(degrees = wingAngle, pivot = Offset(cx - r * 0.36f, cy + r * 0.12f))
    }) {
        drawOval(
            color = Color(0xFFFF2E7E),
            topLeft = Offset(cx - r * 0.96f, cy - r * 0.14f),
            size = Size(r * 1.04f, r * 0.62f)
        )
        drawOval(
            color = outline,
            topLeft = Offset(cx - r * 0.96f, cy - r * 0.14f),
            size = Size(r * 1.04f, r * 0.62f),
            style = thinStroke
        )
    }

    // Gold-rimmed Vice City Aviator sunglasses reflecting sunset cyan & gold
    val avLeft = cx + r * 0.08f
    val avTop = cy - r * 0.46f
    drawRoundRect(
        color = Color(0xFF280B42),
        topLeft = Offset(avLeft, avTop),
        size = Size(r * 0.72f, r * 0.50f),
        cornerRadius = CornerRadius(r * 0.22f, r * 0.22f)
    )
    // Cyan/Gold sunset reflection band inside aviator lens
    drawLine(
        color = Color(0xFF00F5D4),
        start = Offset(avLeft + r * 0.14f, avTop + r * 0.24f),
        end = Offset(avLeft + r * 0.56f, avTop + r * 0.14f),
        strokeWidth = (r * 0.11f).coerceAtLeast(1.8f)
    )
    drawLine(
        color = Color(0xFFFFD166),
        start = Offset(avLeft + r * 0.22f, avTop + r * 0.36f),
        end = Offset(avLeft + r * 0.58f, avTop + r * 0.28f),
        strokeWidth = (r * 0.09f).coerceAtLeast(1.5f)
    )
    drawRoundRect(
        color = Color(0xFFFFD166),
        topLeft = Offset(avLeft, avTop),
        size = Size(r * 0.72f, r * 0.50f),
        cornerRadius = CornerRadius(r * 0.22f, r * 0.22f),
        style = thinStroke
    )
}

/**
 * CYBERPUNK — Cyber Falcon: Matte-obsidian & cyber-yellow armored mech-bird with
 * glowing electric-cyan visor eye and holographic plasma thruster wing!
 */
private fun DrawScope.drawCyberFalcon(
    cx: Float,
    cy: Float,
    r: Float,
    wingPhase: Float,
    beakOpen: Float,
    path: Path,
    outlineStroke: Stroke,
    thinStroke: Stroke,
    beakStroke: Stroke
) {
    val neonCyan = Color(0xFF05D9E8)
    val cyberYellow = Color(0xFFFCEE0A)

    // Sharp metallic cyber beak
    drawAnimatedBeak(
        baseX = cx + r * 0.82f,
        centerY = cy + r * 0.08f,
        beakLength = r * 0.66f,
        beakHalfHeight = r * 0.24f,
        beakOpen = beakOpen,
        topColor = Color(0xFFB0BEC5),
        bottomColor = Color(0xFF546E7A),
        outlineColor = Color(0xFF0A1020),
        path = path,
        stroke = beakStroke
    )

    // Armored dark-titanium body
    drawOval(
        color = Color(0xFF1A2238),
        topLeft = Offset(cx - r * 1.06f, cy - r * 0.94f),
        size = Size(r * 2.12f, r * 1.88f)
    )
    // Cyber-yellow armored chest/collar plate (zero-alloc)
    drawOval(
        color = cyberYellow,
        topLeft = Offset(cx - r * 0.62f, cy + r * 0.20f),
        size = Size(r * 1.28f, r * 0.56f)
    )
    drawOval(
        color = Color(0xFF1A2238),
        topLeft = Offset(cx - r * 0.52f, cy + r * 0.14f),
        size = Size(r * 1.08f, r * 0.36f)
    )
    // Outer electric cyan rim outline
    drawOval(
        color = neonCyan,
        topLeft = Offset(cx - r * 1.06f, cy - r * 0.94f),
        size = Size(r * 2.12f, r * 1.88f),
        style = outlineStroke
    )

    // Holographic cyan plasma thruster wing
    val wingAngle = sin(wingPhase) * 28f
    withTransform({
        rotate(degrees = wingAngle, pivot = Offset(cx - r * 0.35f, cy + r * 0.10f))
    }) {
        drawOval(
            color = Color(0x5505D9E8),
            topLeft = Offset(cx - r * 1.12f, cy - r * 0.20f),
            size = Size(r * 1.16f, r * 0.72f)
        )
        drawOval(
            color = Color(0xFF26334D),
            topLeft = Offset(cx - r * 0.94f, cy - r * 0.12f),
            size = Size(r * 0.96f, r * 0.56f)
        )
        drawOval(
            color = neonCyan,
            topLeft = Offset(cx - r * 0.94f, cy - r * 0.12f),
            size = Size(r * 0.96f, r * 0.56f),
            style = thinStroke
        )
    }

    // Glowing Netrunner cyber-visor eye
    val visorLeft = cx + r * 0.04f
    val visorTop = cy - r * 0.44f
    drawRoundRect(
        color = Color(0x4405D9E8),
        topLeft = Offset(visorLeft - r * 0.08f, visorTop - r * 0.08f),
        size = Size(r * 0.94f, r * 0.50f),
        cornerRadius = CornerRadius(r * 0.18f, r * 0.18f)
    )
    drawRoundRect(
        color = neonCyan,
        topLeft = Offset(visorLeft, visorTop),
        size = Size(r * 0.78f, r * 0.34f),
        cornerRadius = CornerRadius(r * 0.12f, r * 0.12f)
    )
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(visorLeft + r * 0.36f, visorTop + r * 0.09f),
        size = Size(r * 0.30f, r * 0.12f),
        cornerRadius = CornerRadius(r * 0.06f, r * 0.06f)
    )
}

/**
 * KARHUTLA BORNEO — Enggang Borneo (Burung Rangkong / Rhinoceros Hornbill):
 * Iconic sacred Kalimantan bird with jet-black plumage, crisp white accents, golden eye,
 * and unmistakable curved golden beak topped with upturned crimson-red & gold casque!
 */
private fun DrawScope.drawBorneoHornbill(
    cx: Float,
    cy: Float,
    r: Float,
    wingPhase: Float,
    beakOpen: Float,
    path: Path,
    outlineStroke: Stroke,
    thinStroke: Stroke,
    beakStroke: Stroke
) {
    val outline = Color(0xFF140A05)

    // Iconic large golden hornbill beak
    drawAnimatedBeak(
        baseX = cx + r * 0.76f,
        centerY = cy + r * 0.08f,
        beakLength = r * 0.86f,
        beakHalfHeight = r * 0.30f,
        beakOpen = beakOpen,
        topColor = Color(0xFFFFCA28),
        bottomColor = Color(0xFFFF8F00),
        outlineColor = outline,
        path = path,
        stroke = beakStroke
    )

    // Signature upturned crimson-red & gold Hornbill Casque (tanduk enggang) on top of beak!
    path.reset()
    path.moveTo(cx + r * 0.54f, cy - r * 0.24f)
    path.lineTo(cx + r * 1.18f, cy - r * 0.58f)
    path.lineTo(cx + r * 1.32f, cy - r * 0.16f)
    path.lineTo(cx + r * 0.76f, cy - r * 0.06f)
    path.close()
    drawPath(path = path, color = Color(0xFFD84315))
    drawPath(path = path, color = outline, style = thinStroke)

    // Jet-black Enggang plumage body
    drawOval(
        color = Color(0xFF1C1C22),
        topLeft = Offset(cx - r * 1.08f, cy - r * 0.94f),
        size = Size(r * 2.14f, r * 1.88f)
    )
    // White throat/belly sacred plumage accent
    drawOval(
        color = Color(0xFFF5EFE6),
        topLeft = Offset(cx - r * 0.38f, cy + r * 0.16f),
        size = Size(r * 1.16f, r * 0.66f)
    )
    drawOval(
        color = outline,
        topLeft = Offset(cx - r * 1.08f, cy - r * 0.94f),
        size = Size(r * 2.14f, r * 1.88f),
        style = outlineStroke
    )

    // Bold black-and-white banded flapping wing
    val wingAngle = sin(wingPhase) * 26f
    withTransform({
        rotate(degrees = wingAngle, pivot = Offset(cx - r * 0.35f, cy + r * 0.12f))
    }) {
        drawOval(
            color = Color(0xFF26262E),
            topLeft = Offset(cx - r * 0.98f, cy - r * 0.16f),
            size = Size(r * 1.08f, r * 0.64f)
        )
        // Crisp white hornbill feather stripe
        drawOval(
            color = Color(0xFFFAF6F0),
            topLeft = Offset(cx - r * 0.84f, cy + r * 0.08f),
            size = Size(r * 0.66f, r * 0.24f)
        )
        drawOval(
            color = outline,
            topLeft = Offset(cx - r * 0.98f, cy - r * 0.16f),
            size = Size(r * 1.08f, r * 0.64f),
            style = thinStroke
        )
    }

    // Fierce golden-amber eye
    val eyeCenter = Offset(cx + r * 0.38f, cy - r * 0.24f)
    val eyeR = r * 0.30f
    drawCircle(color = Color(0xFFFFCA28), radius = eyeR, center = eyeCenter)
    drawCircle(color = outline, radius = eyeR, center = eyeCenter, style = thinStroke)
    drawCircle(color = Color(0xFF140A05), radius = eyeR * 0.48f, center = Offset(eyeCenter.x + eyeR * 0.18f, eyeCenter.y))
    drawCircle(color = Color.White, radius = eyeR * 0.18f, center = Offset(eyeCenter.x + eyeR * 0.32f, eyeCenter.y - eyeR * 0.22f))
}

/**
 * ORIGINAL — Classic round golden-yellow Flappy Bird with cream belly, flapping white wing,
 * expressive cartoon eye, and bright animated orange beak!
 */
private fun DrawScope.drawOriginalYellowBird(
    cx: Float,
    cy: Float,
    r: Float,
    wingPhase: Float,
    beakOpen: Float,
    path: Path,
    outlineStroke: Stroke,
    thinStroke: Stroke,
    beakStroke: Stroke
) {
    val outline = Color(0xFF3E2912)

    drawAnimatedBeak(
        baseX = cx + r * 0.84f,
        centerY = cy + r * 0.12f,
        beakLength = r * 0.64f,
        beakHalfHeight = r * 0.26f,
        beakOpen = beakOpen,
        topColor = Color(0xFFFF7A29),
        bottomColor = Color(0xFFE65100),
        outlineColor = outline,
        path = path,
        stroke = beakStroke
    )

    drawOval(
        color = Color(0xFFFFD13B),
        topLeft = Offset(cx - r * 1.08f, cy - r * 0.96f),
        size = Size(r * 2.16f, r * 1.92f)
    )
    drawOval(
        color = Color(0xFFFFF2B2),
        topLeft = Offset(cx - r * 0.62f, cy + r * 0.16f),
        size = Size(r * 1.28f, r * 0.68f)
    )
    drawOval(
        color = outline,
        topLeft = Offset(cx - r * 1.08f, cy - r * 0.96f),
        size = Size(r * 2.16f, r * 1.92f),
        style = outlineStroke
    )

    val wingAngle = sin(wingPhase) * 24f
    withTransform({
        rotate(degrees = wingAngle, pivot = Offset(cx - r * 0.35f, cy + r * 0.12f))
    }) {
        drawOval(
            color = Color(0xFFFFF9DF),
            topLeft = Offset(cx - r * 0.92f, cy - r * 0.14f),
            size = Size(r * 1.00f, r * 0.62f)
        )
        drawOval(
            color = outline,
            topLeft = Offset(cx - r * 0.92f, cy - r * 0.14f),
            size = Size(r * 1.00f, r * 0.62f),
            style = thinStroke
        )
    }

    val eyeCenter = Offset(cx + r * 0.40f, cy - r * 0.24f)
    val eyeRadius = r * 0.35f
    drawCircle(color = Color.White, radius = eyeRadius, center = eyeCenter)
    drawCircle(color = outline, radius = eyeRadius, center = eyeCenter, style = thinStroke)
    drawCircle(color = Color(0xFF1F1F1F), radius = eyeRadius * 0.44f, center = Offset(eyeCenter.x + eyeRadius * 0.22f, eyeCenter.y))
    drawCircle(color = Color.White, radius = eyeRadius * 0.17f, center = Offset(eyeCenter.x + eyeRadius * 0.34f, eyeCenter.y - eyeRadius * 0.22f))
}

private fun DrawScope.drawAnimatedBeak(
    baseX: Float,
    centerY: Float,
    beakLength: Float,
    beakHalfHeight: Float,
    beakOpen: Float,
    topColor: Color,
    bottomColor: Color,
    outlineColor: Color,
    path: Path,
    stroke: Stroke
) {
    val openSpread = beakOpen.coerceIn(0f, 1f) * beakHalfHeight * 0.55f

    path.reset()
    path.moveTo(baseX, centerY - beakHalfHeight)
    path.lineTo(baseX + beakLength, centerY - openSpread)
    path.lineTo(baseX, centerY + beakHalfHeight * 0.05f)
    path.close()
    drawPath(path = path, color = topColor)
    drawPath(path = path, color = outlineColor, style = stroke)

    path.reset()
    path.moveTo(baseX, centerY - beakHalfHeight * 0.05f)
    path.lineTo(baseX + beakLength * 0.86f, centerY + beakHalfHeight * 0.28f + openSpread)
    path.lineTo(baseX, centerY + beakHalfHeight)
    path.close()
    drawPath(path = path, color = bottomColor)
    drawPath(path = path, color = outlineColor, style = stroke)
}
