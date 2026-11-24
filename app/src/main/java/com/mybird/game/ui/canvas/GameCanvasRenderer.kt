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
import com.mybird.game.engine.PipePair
import com.mybird.game.engine.SkyTheme
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Allocation-conscious 60fps/120fps Jetpack Compose Canvas renderer for MyBird.
 *
 * Features:
 * - Default 2D flat/pixel-art parallax sky, layered mountains, and drifting clouds
 * - Seamless horizontally scrolling custom user background photo
 * - Circular custom user bird photo decorated with animated outer wing & beak
 * - Default round yellow cartoon bird with animated wing & beak
 */
class GameCanvasRenderer {

    private val reusablePath = Path()

    private val popupTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val popupOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(220, 68, 42, 14)
        style = Paint.Style.STROKE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    fun DrawScope.render(
        engine: GameEngine,
        customBirdBitmap: ImageBitmap?,
        customBackgroundBitmap: ImageBitmap?
    ) {
        val scale = size.height / GameConfig.WORLD_HEIGHT
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
                val theme = engine.currentSkyTheme()
                drawSky(theme, engine.elapsedTime, scale)
                drawClouds(engine, theme, scale)
                drawParallaxMountains(engine, theme, scale)
            }

            drawPipes(engine, scale)
            drawGround(engine, scale)
            drawParticles(engine, scale)
            drawBird(engine, customBirdBitmap, scale)
            drawFloatingPopups(engine, scale)
        }

        // Brief white flash on collision impact
        if (engine.hitFlashAlpha > 0.01f) {
            drawRect(
                color = Color.White.copy(alpha = engine.hitFlashAlpha.coerceIn(0f, 1f)),
                size = size
            )
        }
    }

    private fun DrawScope.drawScrollingCustomBackground(
        bitmap: ImageBitmap,
        scrollDistanceWorld: Float,
        scale: Float
    ) {
        val skyHeightPx = (GameConfig.GROUND_Y * scale).roundToInt().coerceAtLeast(1)
        val bitmapAspect = bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)
        val tileWidthPx = max(size.width, skyHeightPx * bitmapAspect).roundToInt().coerceAtLeast(1)

        val offsetPx = ((scrollDistanceWorld * scale) % tileWidthPx).roundToInt()
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

        // Subtle atmospheric veil so pipes, bird, and white score text always stay readable
        drawRect(
            color = Color(0x1E000000),
            size = Size(size.width, GameConfig.GROUND_Y * scale)
        )
    }

    private fun DrawScope.drawSky(theme: SkyTheme, elapsedTime: Float, scale: Float) {
        val skyColors = when (theme) {
            SkyTheme.DAY -> DAY_SKY_COLORS
            SkyTheme.SUNSET -> SUNSET_SKY_COLORS
            SkyTheme.NIGHT -> NIGHT_SKY_COLORS
        }
        drawRect(
            brush = Brush.verticalGradient(
                colors = skyColors,
                startY = 0f,
                endY = GameConfig.GROUND_Y * scale
            ),
            size = Size(size.width, GameConfig.GROUND_Y * scale)
        )

        val orbCenter = Offset(size.width * 0.80f, 135f * scale)
        when (theme) {
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
                for (i in STAR_POSITIONS.indices step 2) {
                    val sx = STAR_POSITIONS[i] * size.width
                    val sy = STAR_POSITIONS[i + 1] * GameConfig.GROUND_Y * scale * 0.58f
                    val twinkle = 0.45f + 0.55f * ((sin(elapsedTime * 3.2f + i) + 1f) * 0.5f)
                    drawCircle(
                        color = Color.White.copy(alpha = twinkle),
                        radius = (1.8f + (i % 3) * 0.9f) * scale,
                        center = Offset(sx, sy)
                    )
                }
                drawCircle(color = Color(0x33E3F2FD), radius = 58f * scale, center = orbCenter)
                drawCircle(color = Color(0xFFFFFDE7), radius = 34f * scale, center = orbCenter)
                drawCircle(
                    color = Color(0xFF1A2954),
                    radius = 28f * scale,
                    center = Offset(orbCenter.x + 12f * scale, orbCenter.y - 6f * scale)
                )
            }
        }
    }

    private fun DrawScope.drawClouds(engine: GameEngine, theme: SkyTheme, scale: Float) {
        val cloudColor = when (theme) {
            SkyTheme.DAY -> Color(0xECFFFFFF)
            SkyTheme.SUNSET -> Color(0xCCFFE0B2)
            SkyTheme.NIGHT -> Color(0x3890CAF9)
        }
        val cloudShadow = when (theme) {
            SkyTheme.DAY -> Color(0x44B2DFDB)
            SkyTheme.SUNSET -> Color(0x44FF8A65)
            SkyTheme.NIGHT -> Color(0x221E88E5)
        }

        // Crisp flat/pixel-inspired layered clouds
        for (cloud in engine.clouds) {
            val cx = cloud.x * scale
            val cy = cloud.y * scale
            val w = cloud.width * scale
            val h = w * 0.34f
            val corner = CornerRadius(h * 0.35f, h * 0.35f)

            drawRoundRect(
                color = cloudShadow,
                topLeft = Offset(cx, cy + h * 0.14f),
                size = Size(w, h),
                cornerRadius = corner
            )
            drawRoundRect(
                color = cloudColor,
                topLeft = Offset(cx, cy),
                size = Size(w, h),
                cornerRadius = corner
            )
            drawRoundRect(
                color = cloudColor,
                topLeft = Offset(cx + w * 0.16f, cy - h * 0.42f),
                size = Size(w * 0.42f, h * 0.72f),
                cornerRadius = corner
            )
            drawRoundRect(
                color = cloudColor,
                topLeft = Offset(cx + w * 0.46f, cy - h * 0.62f),
                size = Size(w * 0.36f, h * 0.90f),
                cornerRadius = corner
            )
        }
    }

    private fun DrawScope.drawParallaxMountains(engine: GameEngine, theme: SkyTheme, scale: Float) {
        val groundY = GameConfig.GROUND_Y * scale

        val farLight = when (theme) {
            SkyTheme.DAY -> Color(0xFF76C2B5)
            SkyTheme.SUNSET -> Color(0xFFA25A7B)
            SkyTheme.NIGHT -> Color(0xFF213A66)
        }
        val farDark = when (theme) {
            SkyTheme.DAY -> Color(0xFF5FA89E)
            SkyTheme.SUNSET -> Color(0xFF834466)
            SkyTheme.NIGHT -> Color(0xFF172B4F)
        }
        val snowCap = when (theme) {
            SkyTheme.DAY -> Color(0xFFEAF9FA)
            SkyTheme.SUNSET -> Color(0xFFFFD8B0)
            SkyTheme.NIGHT -> Color(0xFF8FB8E8)
        }
        val nearLight = when (theme) {
            SkyTheme.DAY -> Color(0xFF5FB878)
            SkyTheme.SUNSET -> Color(0xFF6B3E5E)
            SkyTheme.NIGHT -> Color(0xFF142847)
        }
        val nearDark = when (theme) {
            SkyTheme.DAY -> Color(0xFF479A60)
            SkyTheme.SUNSET -> Color(0xFF512B4A)
            SkyTheme.NIGHT -> Color(0xFF0D1D36)
        }

        // Far mountain range (slower parallax)
        val farTileW = 360f * scale
        val farOffset = engine.farMountainScrollOffset * scale
        val farCount = ceil(size.width / farTileW).toInt() + 2
        for (i in 0 until farCount) {
            val bx = i * farTileW - farOffset
            drawMountainPeak(bx, groundY, 210f * scale, 225f * scale, farLight, farDark, snowCap)
            drawMountainPeak(bx + 150f * scale, groundY, 230f * scale, 265f * scale, farLight, farDark, snowCap)
        }

        // Near foothill mountain range (faster parallax)
        val nearTileW = 280f * scale
        val nearOffset = engine.nearMountainScrollOffset * scale
        val nearCount = ceil(size.width / nearTileW).toInt() + 2
        for (i in 0 until nearCount) {
            val bx = i * nearTileW - nearOffset
            drawMountainPeak(bx - 20f * scale, groundY, 175f * scale, 130f * scale, nearLight, nearDark, null)
            drawMountainPeak(bx + 120f * scale, groundY, 195f * scale, 155f * scale, nearLight, nearDark, null)
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

        // Sunlit left facet
        reusablePath.reset()
        reusablePath.moveTo(leftX, baseY)
        reusablePath.lineTo(peakX, peakY)
        reusablePath.lineTo(peakX + width * 0.08f, baseY)
        reusablePath.close()
        drawPath(path = reusablePath, color = lightColor)

        // Shaded right facet
        reusablePath.reset()
        reusablePath.moveTo(peakX, peakY)
        reusablePath.lineTo(leftX + width, baseY)
        reusablePath.lineTo(peakX + width * 0.04f, baseY)
        reusablePath.close()
        drawPath(path = reusablePath, color = darkColor)

        // Flat-art snow cap on tall distant peaks
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

    private fun DrawScope.drawPipes(engine: GameEngine, scale: Float) {
        for (pipe in engine.pipes) {
            if (!pipe.active) continue
            drawPipePair(pipe, scale)
        }
    }

    private fun DrawScope.drawPipePair(pipe: PipePair, scale: Float) {
        val pipeW = GameConfig.PIPE_WIDTH * scale
        val lipH = GameConfig.PIPE_LIP_HEIGHT * scale
        val overhang = GameConfig.PIPE_LIP_OVERHANG * scale
        val x = pipe.x * scale
        val groundY = GameConfig.GROUND_Y * scale
        val outlineWidth = (2.6f * scale).coerceAtLeast(2f)

        val topBottomY = pipe.topPipeBottomY * scale
        val bottomTopY = pipe.bottomPipeTopY * scale

        if (topBottomY > 0f) {
            drawPipeRect(
                left = x,
                top = -outlineWidth,
                width = pipeW,
                height = (topBottomY - lipH + outlineWidth).coerceAtLeast(0f),
                outlineWidth = outlineWidth
            )
            drawPipeRect(
                left = x - overhang,
                top = topBottomY - lipH,
                width = pipeW + overhang * 2f,
                height = lipH,
                outlineWidth = outlineWidth,
                cornerPx = 5f * scale
            )
        }

        val bottomBodyTop = bottomTopY + lipH
        if (bottomBodyTop < groundY) {
            drawPipeRect(
                left = x,
                top = bottomBodyTop,
                width = pipeW,
                height = (groundY - bottomBodyTop + outlineWidth).coerceAtLeast(0f),
                outlineWidth = outlineWidth
            )
        }
        drawPipeRect(
            left = x - overhang,
            top = bottomTopY,
            width = pipeW + overhang * 2f,
            height = lipH,
            outlineWidth = outlineWidth,
            cornerPx = 5f * scale
        )
    }

    private fun DrawScope.drawPipeRect(
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        outlineWidth: Float,
        cornerPx: Float = 0f
    ) {
        if (height <= 0f || width <= 0f) return
        val corner = CornerRadius(cornerPx, cornerPx)

        drawRoundRect(
            color = PIPE_BASE_GREEN,
            topLeft = Offset(left, top),
            size = Size(width, height),
            cornerRadius = corner
        )
        drawRect(
            color = PIPE_HIGHLIGHT_GREEN,
            topLeft = Offset(left + width * 0.12f, top + outlineWidth * 0.5f),
            size = Size(width * 0.16f, (height - outlineWidth).coerceAtLeast(0f))
        )
        drawRect(
            color = Color(0x66FFFFFF),
            topLeft = Offset(left + width * 0.15f, top + outlineWidth * 0.5f),
            size = Size(width * 0.05f, (height - outlineWidth).coerceAtLeast(0f))
        )
        drawRect(
            color = PIPE_SHADOW_GREEN,
            topLeft = Offset(left + width * 0.76f, top + outlineWidth * 0.5f),
            size = Size(width * 0.22f, (height - outlineWidth).coerceAtLeast(0f))
        )
        drawRoundRect(
            color = PIPE_OUTLINE,
            topLeft = Offset(left, top),
            size = Size(width, height),
            cornerRadius = corner,
            style = Stroke(width = outlineWidth)
        )
    }

    private fun DrawScope.drawGround(engine: GameEngine, scale: Float) {
        val groundY = GameConfig.GROUND_Y * scale
        val groundHeight = size.height - groundY
        val grassHeight = 26f * scale

        drawRect(
            color = GROUND_SOIL,
            topLeft = Offset(0f, groundY + grassHeight),
            size = Size(size.width, (groundHeight - grassHeight).coerceAtLeast(0f))
        )
        drawRect(
            color = GROUND_SOIL_DARK,
            topLeft = Offset(0f, groundY + grassHeight),
            size = Size(size.width, 10f * scale)
        )
        drawRect(
            color = GRASS_LIGHT,
            topLeft = Offset(0f, groundY),
            size = Size(size.width, grassHeight)
        )

        val tileW = 64f * scale
        val scrollPx = engine.groundScrollOffset * scale
        val tileCount = ceil(size.width / tileW).toInt() + 2
        for (i in 0 until tileCount) {
            val sx = i * tileW - scrollPx
            reusablePath.reset()
            reusablePath.moveTo(sx, groundY + grassHeight)
            reusablePath.lineTo(sx + tileW * 0.35f, groundY)
            reusablePath.lineTo(sx + tileW * 0.68f, groundY)
            reusablePath.lineTo(sx + tileW * 0.33f, groundY + grassHeight)
            reusablePath.close()
            drawPath(path = reusablePath, color = GRASS_DARK)
        }

        val stroke = (2.5f * scale).coerceAtLeast(2f)
        drawLine(
            color = PIPE_OUTLINE,
            start = Offset(0f, groundY),
            end = Offset(size.width, groundY),
            strokeWidth = stroke
        )
        drawLine(
            color = PIPE_OUTLINE,
            start = Offset(0f, groundY + grassHeight),
            end = Offset(size.width, groundY + grassHeight),
            strokeWidth = stroke
        )
    }

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

    private fun DrawScope.drawBird(
        engine: GameEngine,
        customBirdBitmap: ImageBitmap?,
        scale: Float
    ) {
        val bird = engine.bird
        val cx = bird.x * scale
        val cy = bird.y * scale
        val r = bird.radius * scale

        // Subtle shadow projected onto ground
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
                    cx = cx,
                    cy = cy,
                    radius = r,
                    wingPhase = bird.wingPhase,
                    beakOpen = bird.beakOpen,
                    scale = scale
                )
            } else {
                drawDefaultVectorBird(
                    cx = cx,
                    cy = cy,
                    r = r,
                    wingPhase = bird.wingPhase,
                    beakOpen = bird.beakOpen,
                    scale = scale
                )
            }
        }
    }

    private fun DrawScope.drawCustomPhotoBird(
        bitmap: ImageBitmap,
        cx: Float,
        cy: Float,
        radius: Float,
        wingPhase: Float,
        beakOpen: Float,
        scale: Float
    ) {
        val diameter = (radius * 2.12f).roundToInt().coerceAtLeast(1)
        val left = (cx - diameter * 0.5f).roundToInt()
        val top = (cy - diameter * 0.5f).roundToInt()
        val outline = (2.3f * scale).coerceAtLeast(1.8f)

        // 1. Animated outer wing flapping at the back/left side
        val wingAngle = sin(wingPhase) * 28f
        val wingPivot = Offset(cx - radius * 0.85f, cy + radius * 0.05f)
        withTransform({
            rotate(degrees = wingAngle, pivot = wingPivot)
        }) {
            drawOval(
                color = Color(0xFFFFF8DC),
                topLeft = Offset(cx - radius * 1.52f, cy - radius * 0.26f),
                size = Size(radius * 0.92f, radius * 0.56f)
            )
            drawOval(
                color = BIRD_OUTLINE,
                topLeft = Offset(cx - radius * 1.52f, cy - radius * 0.26f),
                size = Size(radius * 0.92f, radius * 0.56f),
                style = Stroke(width = outline)
            )
        }

        // 2. Animated outer beak (upper & lower mandible) at the right edge
        drawAnimatedBeak(
            baseX = cx + radius * 0.92f,
            centerY = cy + radius * 0.06f,
            beakLength = radius * 0.62f,
            beakHalfHeight = radius * 0.26f,
            beakOpen = beakOpen,
            outlineWidth = outline
        )

        // 3. Circular cropped custom user photo with crisp thin outline
        drawImage(
            image = bitmap,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(bitmap.width, bitmap.height),
            dstOffset = IntOffset(left, top),
            dstSize = IntSize(diameter, diameter)
        )
    }

    private fun DrawScope.drawDefaultVectorBird(
        cx: Float,
        cy: Float,
        r: Float,
        wingPhase: Float,
        beakOpen: Float,
        scale: Float
    ) {
        val outline = (2.6f * scale).coerceAtLeast(2f)

        // Animated beak on right edge
        drawAnimatedBeak(
            baseX = cx + r * 0.84f,
            centerY = cy + r * 0.12f,
            beakLength = r * 0.64f,
            beakHalfHeight = r * 0.26f,
            beakOpen = beakOpen,
            outlineWidth = outline * 0.9f
        )

        // Round yellow cartoon bird body
        drawOval(
            color = BIRD_YELLOW,
            topLeft = Offset(cx - r * 1.08f, cy - r * 0.96f),
            size = Size(r * 2.16f, r * 1.92f)
        )
        // Warm belly highlight
        drawOval(
            color = BIRD_BELLY,
            topLeft = Offset(cx - r * 0.62f, cy + r * 0.16f),
            size = Size(r * 1.28f, r * 0.68f)
        )
        // Crisp cartoon body outline
        drawOval(
            color = BIRD_OUTLINE,
            topLeft = Offset(cx - r * 1.08f, cy - r * 0.96f),
            size = Size(r * 2.16f, r * 1.92f),
            style = Stroke(width = outline)
        )

        // Animated flapping wing
        val wingAngle = sin(wingPhase) * 24f
        val wingPivot = Offset(cx - r * 0.35f, cy + r * 0.12f)
        withTransform({
            rotate(degrees = wingAngle, pivot = wingPivot)
        }) {
            drawOval(
                color = Color(0xFFFFF9DF),
                topLeft = Offset(cx - r * 0.92f, cy - r * 0.14f),
                size = Size(r * 1.00f, r * 0.62f)
            )
            drawOval(
                color = BIRD_OUTLINE,
                topLeft = Offset(cx - r * 0.92f, cy - r * 0.14f),
                size = Size(r * 1.00f, r * 0.62f),
                style = Stroke(width = outline * 0.85f)
            )
        }

        // Expressive cartoon eye
        val eyeCenter = Offset(cx + r * 0.40f, cy - r * 0.24f)
        val eyeRadius = r * 0.35f
        drawCircle(color = Color.White, radius = eyeRadius, center = eyeCenter)
        drawCircle(
            color = BIRD_OUTLINE,
            radius = eyeRadius,
            center = eyeCenter,
            style = Stroke(width = outline * 0.85f)
        )
        drawCircle(
            color = Color(0xFF1F1F1F),
            radius = eyeRadius * 0.44f,
            center = Offset(eyeCenter.x + eyeRadius * 0.22f, eyeCenter.y)
        )
        drawCircle(
            color = Color.White,
            radius = eyeRadius * 0.17f,
            center = Offset(eyeCenter.x + eyeRadius * 0.34f, eyeCenter.y - eyeRadius * 0.22f)
        )
    }

    private fun DrawScope.drawAnimatedBeak(
        baseX: Float,
        centerY: Float,
        beakLength: Float,
        beakHalfHeight: Float,
        beakOpen: Float,
        outlineWidth: Float
    ) {
        val openSpread = beakOpen.coerceIn(0f, 1f) * beakHalfHeight * 0.55f
        val stroke = Stroke(
            width = outlineWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        // Upper beak mandible
        reusablePath.reset()
        reusablePath.moveTo(baseX, centerY - beakHalfHeight)
        reusablePath.lineTo(baseX + beakLength, centerY - openSpread)
        reusablePath.lineTo(baseX, centerY + beakHalfHeight * 0.05f)
        reusablePath.close()
        drawPath(path = reusablePath, color = BIRD_BEAK_TOP)
        drawPath(path = reusablePath, color = BIRD_OUTLINE, style = stroke)

        // Lower beak mandible
        reusablePath.reset()
        reusablePath.moveTo(baseX, centerY - beakHalfHeight * 0.05f)
        reusablePath.lineTo(baseX + beakLength * 0.86f, centerY + beakHalfHeight * 0.28f + openSpread)
        reusablePath.lineTo(baseX, centerY + beakHalfHeight)
        reusablePath.close()
        drawPath(path = reusablePath, color = BIRD_BEAK_BOTTOM)
        drawPath(path = reusablePath, color = BIRD_OUTLINE, style = stroke)
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

    companion object {
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
    }
}
