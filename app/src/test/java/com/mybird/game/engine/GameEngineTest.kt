package com.mybird.game.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    @Test
    fun startsInMenuAndFlapsOnlyAfterEnteringReady() {
        val engine = GameEngine(Random(42))
        assertEquals(GamePhase.MENU, engine.phase)
        assertFalse(engine.flap())

        engine.resetToReady()
        assertEquals(GamePhase.READY, engine.phase)

        val flapped = engine.flap()
        assertTrue(flapped)
        assertEquals(GamePhase.PLAYING, engine.phase)
        assertEquals(GameConfig.FLAP_VELOCITY, engine.bird.velocityY, 0.01f)
        assertTrue(engine.pipes.any { it.active })
    }

    @Test
    fun gravityPullsBirdDownAndTriggersGameOverOnGroundCollision() {
        val engine = GameEngine(Random(42))
        engine.resetToReady()
        engine.flap()

        for (pipe in engine.pipes) pipe.active = false

        var crashed = false
        for (frame in 0 until 240) {
            val events = engine.update(1f / 60f)
            if ((events and GameConfig.EVENT_CRASHED) != 0) {
                crashed = true
                break
            }
        }

        assertTrue("Bird should crash into ground under gravity", crashed)
        assertEquals(GamePhase.GAME_OVER, engine.phase)
        assertEquals(GameConfig.GROUND_Y - engine.bird.radius, engine.bird.y, 0.5f)
    }

    @Test
    fun hittingTopScreenBoundaryTriggersGameOver() {
        val engine = GameEngine(Random(42))
        engine.resetToReady()
        engine.flap()
        for (pipe in engine.pipes) pipe.active = false

        engine.bird.y = engine.bird.hitboxRadius - 2f
        engine.bird.velocityY = -300f

        val events = engine.update(1f / 60f)

        assertTrue("Bird hitting top screen edge should crash", (events and GameConfig.EVENT_CRASHED) != 0)
        assertEquals(GamePhase.GAME_OVER, engine.phase)
    }

    @Test
    fun passingPipeCenterIncrementsScoreExactlyOnce() {
        val engine = GameEngine(Random(7))
        engine.resetToReady()
        engine.flap()
        for (pipe in engine.pipes) pipe.active = false

        val safeGapY = GameConfig.GROUND_Y * 0.5f
        engine.bird.y = safeGapY
        engine.bird.velocityY = 0f
        engine.spawnPipeAt(
            xPos = engine.bird.x - GameConfig.PIPE_WIDTH * 0.5f + 2f,
            forcedGapCenterY = safeGapY
        )

        val events1 = engine.update(1f / 60f)
        assertTrue((events1 and GameConfig.EVENT_SCORED) != 0)
        assertEquals(1, engine.score)

        val events2 = engine.update(1f / 60f)
        assertFalse((events2 and GameConfig.EVENT_SCORED) != 0)
        assertEquals(1, engine.score)
    }

    @Test
    fun hittingPipeBodyTriggersCollisionAndGameOver() {
        val engine = GameEngine(Random(99))
        engine.resetToReady()
        engine.flap()
        for (pipe in engine.pipes) pipe.active = false

        engine.bird.y = 140f
        engine.bird.velocityY = 0f
        engine.spawnPipeAt(xPos = engine.bird.x, forcedGapCenterY = 620f)

        val events = engine.update(1f / 60f)

        assertTrue((events and GameConfig.EVENT_CRASHED) != 0)
        assertEquals(GamePhase.GAME_OVER, engine.phase)
    }

    @Test
    fun pauseAndResumeFreezeAndRestoreSimulation() {
        val engine = GameEngine(Random(12))
        engine.resetToReady()
        engine.flap()
        engine.pause()
        assertEquals(GamePhase.PAUSED, engine.phase)

        val frozenY = engine.bird.y
        val events = engine.update(0.05f)
        assertEquals(GameConfig.EVENT_NONE, events)
        assertEquals(frozenY, engine.bird.y, 0.001f)
        assertFalse(engine.flap())

        engine.resume()
        assertEquals(GamePhase.PLAYING, engine.phase)
        engine.update(1f / 60f)
        assertTrue(engine.bird.y != frozenY)
    }

    @Test
    fun customBackgroundScrollsContinuously() {
        val engine = GameEngine(Random(5))
        val initialScroll = engine.customBgScrollDistance
        engine.update(0.03f)
        assertTrue(engine.customBgScrollDistance > initialScroll)
    }

    @Test
    fun circleIntersectsRectMatchesGeometry() {
        assertTrue(GameEngine.circleIntersectsRect(10f, 10f, 5f, 12f, 8f, 30f, 20f))
        assertFalse(GameEngine.circleIntersectsRect(10f, 10f, 5f, 20f, 20f, 30f, 30f))
    }
}
