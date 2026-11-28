package com.mybird.game.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * Pure Kotlin deterministic 2D Flappy Bird game engine.
 *
 * Uses pre-allocated object pools (pipes, particles, floating score popups, clouds)
 * so the 60fps/120fps simulation loop performs zero heap allocations per frame.
 */
class GameEngine(
    private val random: Random = Random.Default
) {
    var worldWidth: Float = GameConfig.DEFAULT_WORLD_WIDTH
        private set

    var phase: GamePhase = GamePhase.MENU
        private set

    var activeTheme: GameTheme = GameTheme.DEFAULT
        private set

    var score: Int = 0
        private set

    var elapsedTime: Float = 0f
        private set

    var groundScrollOffset: Float = 0f
        private set

    var farMountainScrollOffset: Float = 0f
        private set

    var midSceneryScrollOffset: Float = 0f
        private set

    var nearMountainScrollOffset: Float = 0f
        private set

    /**
     * Continuous horizontal scroll distance (in world units) used for seamless
     * scrolling of custom user background photos.
     */
    var customBgScrollDistance: Float = 0f
        private set

    var hitFlashAlpha: Float = 0f
        private set

    var shakeTimer: Float = 0f
        private set

    var shakeOffsetX: Float = 0f
        private set

    var shakeOffsetY: Float = 0f
        private set

    val bird = BirdState()

    val pipes: Array<PipePair> = Array(GameConfig.PIPE_POOL_SIZE) { PipePair() }
    val particles: Array<Particle> = Array(GameConfig.PARTICLE_POOL_SIZE) { Particle() }
    val popups: Array<FloatingPopup> = Array(GameConfig.POPUP_POOL_SIZE) { FloatingPopup() }
    val clouds: Array<CloudItem> = Array(GameConfig.CLOUD_COUNT) { idx ->
        CloudItem(
            x = idx * 165f + 40f,
            y = 95f + (idx % 3) * 85f,
            width = 110f + (idx % 3) * 32f,
            speedFactor = 0.08f + (idx % 3) * 0.04f
        )
    }
    val ambientParticles: Array<AmbientParticle> = Array(GameConfig.AMBIENT_PARTICLE_COUNT) { idx ->
        AmbientParticle(
            x = (idx * 73f) % GameConfig.DEFAULT_WORLD_WIDTH,
            y = (idx * 97f) % GameConfig.GROUND_Y,
            size = 2.5f + (idx % 4) * 1.4f,
            speedX = -26f - (idx % 5) * 14f,
            speedY = -28f - (idx % 4) * 16f,
            phase = idx * 0.85f,
            variant = idx % 3
        )
    }

    init {
        reseedAmbientParticlesForTheme(activeTheme)
        resetToMenu()
    }

    fun setTheme(theme: GameTheme) {
        if (activeTheme == theme) return
        activeTheme = theme
        reseedAmbientParticlesForTheme(theme)
    }

    private fun reseedAmbientParticlesForTheme(theme: GameTheme) {
        for (i in ambientParticles.indices) {
            val p = ambientParticles[i]
            p.x = (i * 89f) % worldWidth.coerceAtLeast(GameConfig.MIN_WORLD_WIDTH)
            p.y = (i * 113f) % GameConfig.GROUND_Y
            p.phase = i * 0.77f
            p.variant = i % 3
            when (theme) {
                GameTheme.KARHUTLA_BORNEO -> {
                    // Rising glowing wildfire embers
                    p.size = 2.6f + (i % 4) * 1.3f
                    p.speedX = -38f - (i % 5) * 15f
                    p.speedY = -52f - (i % 4) * 26f
                }

                GameTheme.CYBERPUNK -> {
                    // Fast slanted neon rain streaks
                    p.size = 1.8f + (i % 3) * 0.9f
                    p.speedX = -95f - (i % 4) * 24f
                    p.speedY = 420f + (i % 5) * 65f
                }

                GameTheme.BLOK_M -> {
                    // Warm floating Jaksel street bokeh & kopi sparks
                    p.size = 3.2f + (i % 4) * 1.8f
                    p.speedX = -24f - (i % 4) * 10f
                    p.speedY = -14f - (i % 3) * 8f
                }

                GameTheme.GTA6 -> {
                    // Warm Vice sunset glitter & palm breeze motes
                    p.size = 2.8f + (i % 4) * 1.2f
                    p.speedX = -42f - (i % 4) * 14f
                    p.speedY = -10f + (i % 3) * 8f
                }

                GameTheme.ORIGINAL -> {
                    // Soft drifting sunlit air motes
                    p.size = 2.2f + (i % 3) * 1.0f
                    p.speedX = -28f - (i % 3) * 10f
                    p.speedY = -6f + (i % 3) * 5f
                }
            }
        }
    }

    /**
     * Adapts horizontal world width to match the Canvas aspect ratio while
     * keeping vertical physics 100% consistent across all devices.
     */
    fun updateViewport(canvasWidthPx: Float, canvasHeightPx: Float) {
        if (canvasWidthPx <= 0f || canvasHeightPx <= 0f) return
        val newWidth = (GameConfig.WORLD_HEIGHT * (canvasWidthPx / canvasHeightPx))
            .coerceIn(GameConfig.MIN_WORLD_WIDTH, GameConfig.MAX_WORLD_WIDTH)
        worldWidth = newWidth
        bird.x = worldWidth * GameConfig.BIRD_X_RATIO
    }

    fun resetToMenu() {
        resetWorldEntities()
        phase = GamePhase.MENU
    }

    fun resetToReady() {
        resetWorldEntities()
        phase = GamePhase.READY
    }

    private fun resetWorldEntities() {
        score = 0
        hitFlashAlpha = 0f
        shakeTimer = 0f
        shakeOffsetX = 0f
        shakeOffsetY = 0f

        bird.x = worldWidth * GameConfig.BIRD_X_RATIO
        bird.y = GameConfig.GROUND_Y * GameConfig.BIRD_READY_Y_RATIO
        bird.velocityY = 0f
        bird.rotationDeg = 0f
        bird.wingPhase = 0f
        bird.beakOpen = 0f

        for (pipe in pipes) {
            pipe.active = false
            pipe.scored = false
        }
        for (particle in particles) {
            particle.active = false
        }
        for (popup in popups) {
            popup.active = false
        }
    }

    /**
     * Triggers an upward flap impulse. Starts the run automatically from [GamePhase.READY].
     * @return true if a flap occurred.
     */
    fun flap(): Boolean {
        return when (phase) {
            GamePhase.READY -> {
                phase = GamePhase.PLAYING
                bird.velocityY = GameConfig.FLAP_VELOCITY
                bird.rotationDeg = GameConfig.MIN_ROTATION_DEG
                bird.beakOpen = 1f
                spawnInitialPipes()
                spawnFlapParticles()
                true
            }

            GamePhase.PLAYING -> {
                bird.velocityY = GameConfig.FLAP_VELOCITY
                bird.wingPhase = 0f
                bird.beakOpen = 1f
                spawnFlapParticles()
                true
            }

            GamePhase.MENU, GamePhase.PAUSED, GamePhase.GAME_OVER -> false
        }
    }

    fun pause() {
        if (phase == GamePhase.PLAYING) {
            phase = GamePhase.PAUSED
        }
    }

    fun resume() {
        if (phase == GamePhase.PAUSED) {
            phase = GamePhase.PLAYING
        }
    }

    /**
     * Advances simulation by [rawDt] seconds.
     * @return bitmask of [GameConfig.EVENT_SCORED] / [GameConfig.EVENT_CRASHED].
     */
    fun update(rawDt: Float): Int {
        val dt = rawDt.coerceIn(0f, GameConfig.MAX_DELTA_TIME)
        if (dt <= 0f || phase == GamePhase.PAUSED) return GameConfig.EVENT_NONE

        elapsedTime += dt
        updateEffects(dt)

        return when (phase) {
            GamePhase.MENU, GamePhase.READY -> {
                updateIdleHover(dt)
                GameConfig.EVENT_NONE
            }

            GamePhase.PLAYING -> updatePlaying(dt)
            GamePhase.GAME_OVER -> {
                updateGameOver(dt)
                GameConfig.EVENT_NONE
            }

            GamePhase.PAUSED -> GameConfig.EVENT_NONE
        }
    }

    fun currentPipeSpeed(): Float {
        val progress = (score / GameConfig.DIFFICULTY_MAX_SCORE).coerceIn(0f, 1f)
        return GameConfig.PIPE_BASE_SPEED +
            (GameConfig.PIPE_MAX_SPEED - GameConfig.PIPE_BASE_SPEED) * progress
    }

    fun currentPipeGap(): Float {
        val progress = (score / GameConfig.DIFFICULTY_MAX_SCORE).coerceIn(0f, 1f)
        return GameConfig.PIPE_BASE_GAP -
            (GameConfig.PIPE_BASE_GAP - GameConfig.PIPE_MIN_GAP) * progress
    }

    fun currentSkyTheme(): SkyTheme = when ((score / 10) % 3) {
        0 -> SkyTheme.DAY
        1 -> SkyTheme.SUNSET
        else -> SkyTheme.NIGHT
    }

    fun currentMedal(): Medal = when {
        score >= 50 -> Medal.PLATINUM
        score >= 35 -> Medal.GOLD
        score >= 20 -> Medal.SILVER
        score >= 10 -> Medal.BRONZE
        else -> Medal.NONE
    }

    fun isSettled(): Boolean {
        if (phase != GamePhase.GAME_OVER) return false
        if (bird.y + bird.radius < GameConfig.GROUND_Y - 0.5f) return false
        if (hitFlashAlpha > 0.01f || shakeTimer > 0.01f) return false
        for (p in particles) if (p.active) return false
        for (pop in popups) if (pop.active) return false
        return true
    }

    private fun updateIdleHover(dt: Float) {
        val baseY = GameConfig.GROUND_Y * GameConfig.BIRD_READY_Y_RATIO
        bird.y = baseY + sin(elapsedTime * 5.2f) * 14f
        bird.rotationDeg = sin(elapsedTime * 5.2f) * 4f
        bird.wingPhase += dt * 9f
        bird.beakOpen = ((sin(elapsedTime * 4.5f) + 1f) * 0.22f)

        scrollEnvironment(GameConfig.PIPE_BASE_SPEED * 0.65f, dt)
    }

    private fun updatePlaying(dt: Float): Int {
        var events = GameConfig.EVENT_NONE
        val speed = currentPipeSpeed()

        scrollEnvironment(speed, dt)

        // Bird vertical integration
        bird.velocityY = (bird.velocityY + GameConfig.GRAVITY * dt)
            .coerceAtMost(GameConfig.MAX_FALL_VELOCITY)
        bird.y += bird.velocityY * dt

        // Smooth pitch angle based on velocity
        val targetRotation = if (bird.velocityY < 140f) {
            GameConfig.MIN_ROTATION_DEG
        } else {
            val fallFactor = ((bird.velocityY - 140f) / (GameConfig.MAX_FALL_VELOCITY - 140f))
                .coerceIn(0f, 1f)
            GameConfig.MIN_ROTATION_DEG +
                (GameConfig.MAX_ROTATION_DEG - GameConfig.MIN_ROTATION_DEG) * fallFactor
        }
        val lerpRate = if (bird.velocityY < 0f) 16f else 8.5f
        bird.rotationDeg += (targetRotation - bird.rotationDeg) * (lerpRate * dt).coerceAtMost(1f)
        bird.wingPhase += dt * if (bird.velocityY < 0f) 18f else 9f
        bird.beakOpen = (bird.beakOpen - dt * 4.8f).coerceAtLeast(0f)

        // Move pipes, score & check collisions
        var rightmostActiveX = Float.NEGATIVE_INFINITY
        var crashed = false

        for (pipe in pipes) {
            if (!pipe.active) continue

            pipe.x -= speed * dt

            if (pipe.x + GameConfig.PIPE_WIDTH + GameConfig.PIPE_LIP_OVERHANG < -20f) {
                pipe.active = false
                continue
            }

            if (pipe.x > rightmostActiveX) {
                rightmostActiveX = pipe.x
            }

            // Score when bird center passes pipe center
            if (!pipe.scored && pipe.x + GameConfig.PIPE_WIDTH * 0.5f < bird.x) {
                pipe.scored = true
                score += 1
                events = events or GameConfig.EVENT_SCORED
                spawnScorePopup()
                spawnScoreSparkles()
            }

            if (!crashed && collidesWithPipe(pipe)) {
                crashed = true
            }
        }

        // Spawn next pipe when spacing threshold is met
        if (rightmostActiveX <= worldWidth - GameConfig.PIPE_SPAWN_SPACING + GameConfig.PIPE_WIDTH) {
            val nextX = max(worldWidth + 32f, rightmostActiveX + GameConfig.PIPE_SPAWN_SPACING)
            spawnPipeAt(nextX)
        }

        // Top screen boundary collision
        if (bird.y - bird.hitboxRadius <= 0f) {
            bird.y = bird.hitboxRadius
            crashed = true
        }

        // Ground collision
        if (bird.y + bird.hitboxRadius >= GameConfig.GROUND_Y) {
            bird.y = GameConfig.GROUND_Y - bird.radius
            crashed = true
        }

        if (crashed) {
            triggerGameOver()
            events = events or GameConfig.EVENT_CRASHED
        }

        return events
    }

    private fun updateGameOver(dt: Float) {
        bird.beakOpen = (bird.beakOpen - dt * 4f).coerceAtLeast(0f)
        if (bird.y + bird.radius < GameConfig.GROUND_Y) {
            bird.velocityY = (bird.velocityY + GameConfig.GRAVITY * dt)
                .coerceAtMost(GameConfig.MAX_FALL_VELOCITY)
            bird.y += bird.velocityY * dt
            bird.rotationDeg = (bird.rotationDeg + 320f * dt).coerceAtMost(90f)
            if (bird.y + bird.radius >= GameConfig.GROUND_Y) {
                bird.y = GameConfig.GROUND_Y - bird.radius
                bird.velocityY = 0f
            }
        }
    }

    private fun updateEffects(dt: Float) {
        if (hitFlashAlpha > 0f) {
            hitFlashAlpha = (hitFlashAlpha - dt * 2.8f).coerceAtLeast(0f)
        }

        if (shakeTimer > 0f) {
            shakeTimer = (shakeTimer - dt).coerceAtLeast(0f)
            val magnitude = (shakeTimer / 0.28f) * 14f
            shakeOffsetX = (random.nextFloat() * 2f - 1f) * magnitude
            shakeOffsetY = (random.nextFloat() * 2f - 1f) * magnitude
        } else {
            shakeOffsetX = 0f
            shakeOffsetY = 0f
        }

        for (p in particles) {
            if (!p.active) continue
            p.life -= dt
            if (p.life <= 0f) {
                p.active = false
                continue
            }
            p.vy += 460f * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
        }

        for (pop in popups) {
            if (!pop.active) continue
            pop.life -= dt
            if (pop.life <= 0f) {
                pop.active = false
                continue
            }
            pop.y += pop.vy * dt
        }
    }

    private fun scrollEnvironment(speed: Float, dt: Float) {
        groundScrollOffset = (groundScrollOffset + speed * dt) % 64f
        farMountainScrollOffset = (farMountainScrollOffset + speed * 0.14f * dt) % 360f
        midSceneryScrollOffset = (midSceneryScrollOffset + speed * 0.22f * dt) % 320f
        nearMountainScrollOffset = (nearMountainScrollOffset + speed * 0.34f * dt) % 280f
        customBgScrollDistance += speed * 0.26f * dt

        for (cloud in clouds) {
            cloud.x -= speed * cloud.speedFactor * dt
            if (cloud.x + cloud.width < -20f) {
                cloud.x = worldWidth + 40f + random.nextFloat() * 80f
                cloud.y = 70f + random.nextFloat() * 240f
            }
        }

        val maxW = worldWidth + 40f
        for (ap in ambientParticles) {
            ap.x += ap.speedX * dt
            ap.y += ap.speedY * dt
            if (ap.x < -30f) {
                ap.x = maxW + random.nextFloat() * 35f
                ap.y = if (activeTheme == GameTheme.KARHUTLA_BORNEO) {
                    GameConfig.GROUND_Y - random.nextFloat() * 260f
                } else {
                    random.nextFloat() * GameConfig.GROUND_Y
                }
            }
            if (ap.y < -25f) {
                ap.y = GameConfig.GROUND_Y + random.nextFloat() * 15f
                ap.x = random.nextFloat() * maxW
            } else if (ap.y > GameConfig.GROUND_Y + 25f) {
                ap.y = -15f - random.nextFloat() * 30f
                ap.x = random.nextFloat() * maxW
            }
        }
    }

    private fun spawnInitialPipes() {
        val firstX = worldWidth + 140f
        spawnPipeAt(firstX)
        spawnPipeAt(firstX + GameConfig.PIPE_SPAWN_SPACING)
    }

    internal fun spawnPipeAt(xPos: Float, forcedGapCenterY: Float? = null) {
        val slot = pipes.firstOrNull { !it.active } ?: return
        val gap = currentPipeGap()
        val minCenter = GameConfig.PIPE_MIN_EDGE_CLEARANCE + gap * 0.5f
        val maxCenter = GameConfig.GROUND_Y - GameConfig.PIPE_MIN_EDGE_CLEARANCE - gap * 0.5f
        val gapCenter = forcedGapCenterY?.coerceIn(minCenter, maxCenter)
            ?: (minCenter + random.nextFloat() * (maxCenter - minCenter))

        slot.x = xPos
        slot.gapCenterY = gapCenter
        slot.gapHeight = gap
        slot.scored = false
        slot.active = true
    }

    private fun collidesWithPipe(pipe: PipePair): Boolean {
        val cx = bird.x
        val cy = bird.y
        val r = bird.hitboxRadius

        val bodyLeft = pipe.x
        val bodyRight = pipe.x + GameConfig.PIPE_WIDTH
        val lipLeft = pipe.x - GameConfig.PIPE_LIP_OVERHANG
        val lipRight = pipe.x + GameConfig.PIPE_WIDTH + GameConfig.PIPE_LIP_OVERHANG

        val topBottom = pipe.topPipeBottomY
        val bottomTop = pipe.bottomPipeTopY

        // Quick horizontal broad-phase rejection
        if (cx + r < lipLeft || cx - r > lipRight) return false

        // Top pipe shaft + lip
        if (circleIntersectsRect(cx, cy, r, bodyLeft, -1000f, bodyRight, topBottom)) return true
        if (circleIntersectsRect(
                cx, cy, r,
                lipLeft, topBottom - GameConfig.PIPE_LIP_HEIGHT,
                lipRight, topBottom
            )
        ) return true

        // Bottom pipe shaft + lip
        if (circleIntersectsRect(cx, cy, r, bodyLeft, bottomTop, bodyRight, GameConfig.GROUND_Y)) return true
        if (circleIntersectsRect(
                cx, cy, r,
                lipLeft, bottomTop,
                lipRight, bottomTop + GameConfig.PIPE_LIP_HEIGHT
            )
        ) return true

        return false
    }

    private fun triggerGameOver() {
        phase = GamePhase.GAME_OVER
        hitFlashAlpha = 0.78f
        shakeTimer = 0.28f
        if (bird.velocityY < 0f) {
            bird.velocityY = 120f
        }
        spawnCrashParticles()
    }

    private fun spawnFlapParticles() {
        val colors = activeTheme.flapParticleColors
        var spawned = 0
        for (p in particles) {
            if (p.active) continue
            p.active = true
            p.x = bird.x - bird.radius * 0.65f
            p.y = bird.y + (random.nextFloat() - 0.5f) * 14f
            p.vx = -90f - random.nextFloat() * 110f
            p.vy = 60f + random.nextFloat() * 130f
            p.size = 4.5f + random.nextFloat() * 4.5f
            p.maxLife = 0.34f + random.nextFloat() * 0.16f
            p.life = p.maxLife
            p.colorArgb = colors[spawned % colors.size]
            spawned++
            if (spawned >= 4) break
        }
    }

    private fun spawnScoreSparkles() {
        val colors = activeTheme.scoreParticleColors
        var spawned = 0
        for (p in particles) {
            if (p.active) continue
            val angle = (spawned / 6f) * (2f * PI.toFloat())
            val speed = 110f + random.nextFloat() * 90f
            p.active = true
            p.x = bird.x
            p.y = bird.y - bird.radius
            p.vx = cos(angle) * speed
            p.vy = sin(angle) * speed - 60f
            p.size = 5f + random.nextFloat() * 3f
            p.maxLife = 0.45f
            p.life = p.maxLife
            p.colorArgb = colors[spawned % colors.size]
            spawned++
            if (spawned >= 6) break
        }
    }

    private fun spawnCrashParticles() {
        val colors = activeTheme.crashParticleColors
        var spawned = 0
        for (p in particles) {
            if (p.active) continue
            val angle = random.nextFloat() * 2f * PI.toFloat()
            val speed = 130f + random.nextFloat() * 220f
            p.active = true
            p.x = bird.x
            p.y = bird.y
            p.vx = cos(angle) * speed
            p.vy = sin(angle) * speed - 90f
            p.size = 6f + random.nextFloat() * 5f
            p.maxLife = 0.55f + random.nextFloat() * 0.25f
            p.life = p.maxLife
            p.colorArgb = colors[spawned % colors.size]
            spawned++
            if (spawned >= 12) break
        }
    }

    private fun spawnScorePopup() {
        val slot = popups.firstOrNull { !it.active } ?: return
        slot.active = true
        slot.x = bird.x + 12f
        slot.y = bird.y - bird.radius - 12f
        slot.vy = -105f
        slot.maxLife = 0.72f
        slot.life = slot.maxLife
        slot.text = "+1"
    }

    companion object {
        internal fun circleIntersectsRect(
            cx: Float,
            cy: Float,
            radius: Float,
            left: Float,
            top: Float,
            right: Float,
            bottom: Float
        ): Boolean {
            val closestX = cx.coerceIn(left, right)
            val closestY = cy.coerceIn(top, bottom)
            val dx = cx - closestX
            val dy = cy - closestY
            return (dx * dx + dy * dy) < (radius * radius)
        }
    }
}
