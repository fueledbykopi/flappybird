package com.mybird.game.engine

import com.mybird.game.audio.RetroSoundEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class GameThemeAndAudioTest {

    @Test
    fun blokMIsDefaultThemeAndAllFiveThemesRoundTripById() {
        assertEquals(GameTheme.BLOK_M, GameTheme.DEFAULT)
        assertEquals(5, GameTheme.entries.size)
        assertEquals(GameTheme.BLOK_M, GameTheme.fromId(null))
        assertEquals(GameTheme.BLOK_M, GameTheme.fromId("unknown_theme"))

        for (theme in GameTheme.entries) {
            assertEquals(theme, GameTheme.fromId(theme.id))
            assertTrue(theme.displayName.isNotBlank())
            assertTrue(theme.characterName.isNotBlank())
        }
    }

    @Test
    fun engineAppliesThemeAndAnimatesAmbientParticles() {
        val engine = GameEngine(Random(17))
        assertEquals(GameTheme.BLOK_M, engine.activeTheme)

        engine.setTheme(GameTheme.KARHUTLA_BORNEO)
        assertEquals(GameTheme.KARHUTLA_BORNEO, engine.activeTheme)
        // Karhutla Borneo embers rise upward (negative Y speed)
        assertTrue(engine.ambientParticles.all { it.speedY < 0f })

        engine.setTheme(GameTheme.CYBERPUNK)
        assertEquals(GameTheme.CYBERPUNK, engine.activeTheme)
        // Cyberpunk rain streaks fall downward (positive Y speed)
        assertTrue(engine.ambientParticles.all { it.speedY > 0f })

        val initialY = engine.ambientParticles[0].y
        engine.update(1f / 60f)
        assertNotEquals(initialY, engine.ambientParticles[0].y)
    }

    @Test
    fun eachThemeSynthesizesDistinctNonSilentAudioWaveforms() {
        val flapFingerprints = mutableSetOf<Long>()
        val scoreFingerprints = mutableSetOf<Long>()
        val hitFingerprints = mutableSetOf<Long>()

        for (theme in GameTheme.entries) {
            val flap = RetroSoundEngine.synthesizeFlapPcm(theme)
            val score = RetroSoundEngine.synthesizeScorePcm(theme)
            val hit = RetroSoundEngine.synthesizeHitPcm(theme)

            assertTrue("${theme.name} flap must be non-empty", flap.size > 500)
            assertTrue("${theme.name} score must be non-empty", score.size > 1000)
            assertTrue("${theme.name} hit must be non-empty", hit.size > 1000)

            assertTrue("${theme.name} flap must be audible", peakAmplitude(flap) > 1000)
            assertTrue("${theme.name} score must be audible", peakAmplitude(score) > 1000)
            assertTrue("${theme.name} hit must be audible", peakAmplitude(hit) > 1000)

            flapFingerprints.add(waveformFingerprint(flap))
            scoreFingerprints.add(waveformFingerprint(score))
            hitFingerprints.add(waveformFingerprint(hit))
        }

        // Verify every single theme has a unique sound signature
        assertEquals(GameTheme.entries.size, flapFingerprints.size)
        assertEquals(GameTheme.entries.size, scoreFingerprints.size)
        assertEquals(GameTheme.entries.size, hitFingerprints.size)
    }

    private fun peakAmplitude(pcm: ShortArray): Int {
        var maxAmp = 0
        for (s in pcm) {
            val a = abs(s.toInt())
            if (a > maxAmp) maxAmp = a
        }
        return maxAmp
    }

    private fun waveformFingerprint(pcm: ShortArray): Long {
        var hash = pcm.size.toLong()
        for (i in pcm.indices step 13) {
            hash = hash * 31L + pcm[i].toLong()
        }
        return hash
    }
}
