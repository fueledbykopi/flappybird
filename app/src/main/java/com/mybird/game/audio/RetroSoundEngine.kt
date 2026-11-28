package com.mybird.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.mybird.game.engine.GameTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.EnumMap
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Ultra-lightweight multi-theme procedural SFX synthesizer backed by Android [SoundPool].
 *
 * Each [GameTheme] synthesizes its own signature sound identity (flap, score, hit) into tiny
 * cached PCM16 WAV clips on [Dispatchers.IO] (zero binary APK overhead, <150 KB total RAM,
 * low-latency playback on Android 9 through Android 15+).
 */
class RetroSoundEngine(private val context: Context) {

    private data class ThemeSoundIds(
        val flapId: Int,
        val scoreId: Int,
        val hitId: Int
    )

    private var soundPool: SoundPool? = null
    private val themeSounds = EnumMap<GameTheme, ThemeSoundIds>(GameTheme::class.java)

    @Volatile
    private var ready: Boolean = false

    @Volatile
    var isEnabled: Boolean = true

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (ready) return@withContext
        runCatching {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val pool = SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(attrs)
                .build()

            for (theme in GameTheme.entries) {
                val flapFile = ensureWavFile("sfx_${CACHE_VERSION}_${theme.id}_flap.wav") {
                    synthesizeFlapPcm(theme)
                }
                val scoreFile = ensureWavFile("sfx_${CACHE_VERSION}_${theme.id}_score.wav") {
                    synthesizeScorePcm(theme)
                }
                val hitFile = ensureWavFile("sfx_${CACHE_VERSION}_${theme.id}_hit.wav") {
                    synthesizeHitPcm(theme)
                }
                themeSounds[theme] = ThemeSoundIds(
                    flapId = pool.load(flapFile.absolutePath, 1),
                    scoreId = pool.load(scoreFile.absolutePath, 1),
                    hitId = pool.load(hitFile.absolutePath, 1)
                )
            }

            soundPool = pool
            ready = true
        }
    }

    fun playFlap(theme: GameTheme = GameTheme.DEFAULT) {
        if (!isEnabled || !ready) return
        val id = themeSounds[theme]?.flapId ?: return
        if (id != 0) soundPool?.play(id, 0.58f, 0.58f, 1, 0, 1.0f)
    }

    fun playScore(theme: GameTheme = GameTheme.DEFAULT) {
        if (!isEnabled || !ready) return
        val id = themeSounds[theme]?.scoreId ?: return
        if (id != 0) soundPool?.play(id, 0.72f, 0.72f, 2, 0, 1.0f)
    }

    fun playHit(theme: GameTheme = GameTheme.DEFAULT) {
        if (!isEnabled || !ready) return
        val id = themeSounds[theme]?.hitId ?: return
        if (id != 0) soundPool?.play(id, 0.85f, 0.85f, 3, 0, 1.0f)
    }

    fun playThemePreview(theme: GameTheme) {
        if (!isEnabled || !ready) return
        val id = themeSounds[theme]?.scoreId ?: return
        if (id != 0) soundPool?.play(id, 0.45f, 0.45f, 1, 0, 1.0f)
    }

    fun release() {
        ready = false
        soundPool?.release()
        soundPool = null
        themeSounds.clear()
    }

    private fun ensureWavFile(fileName: String, generator: () -> ShortArray): File {
        val file = File(context.cacheDir, fileName)
        if (!file.exists() || file.length() <= 44L) {
            writePcmWav(file, generator(), SAMPLE_RATE)
        }
        return file
    }

    private fun writePcmWav(file: File, pcm: ShortArray, sampleRate: Int) {
        val dataBytes = pcm.size * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(36 + dataBytes)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1)
            putShort(1)
            putInt(sampleRate)
            putInt(sampleRate * 2)
            putShort(2)
            putShort(16)
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataBytes)
        }

        val body = ByteBuffer.allocate(dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in pcm) {
            body.putShort(sample)
        }

        FileOutputStream(file).use { out ->
            out.write(header.array())
            out.write(body.array())
        }
    }

    companion object {
        internal const val SAMPLE_RATE = 22050
        private const val CACHE_VERSION = "v2"

        internal fun synthesizeFlapPcm(theme: GameTheme): ShortArray = when (theme) {
            GameTheme.BLOK_M -> synthesizeBlokMFlap()
            GameTheme.GTA6 -> synthesizeGta6Flap()
            GameTheme.CYBERPUNK -> synthesizeCyberpunkFlap()
            GameTheme.KARHUTLA_BORNEO -> synthesizeBorneoFlap()
            GameTheme.ORIGINAL -> synthesizeOriginalFlap()
        }

        internal fun synthesizeScorePcm(theme: GameTheme): ShortArray = when (theme) {
            GameTheme.BLOK_M -> synthesizeBlokMScore()
            GameTheme.GTA6 -> synthesizeGta6Score()
            GameTheme.CYBERPUNK -> synthesizeCyberpunkScore()
            GameTheme.KARHUTLA_BORNEO -> synthesizeBorneoScore()
            GameTheme.ORIGINAL -> synthesizeOriginalScore()
        }

        internal fun synthesizeHitPcm(theme: GameTheme): ShortArray = when (theme) {
            GameTheme.BLOK_M -> synthesizeBlokMHit()
            GameTheme.GTA6 -> synthesizeGta6Hit()
            GameTheme.CYBERPUNK -> synthesizeCyberpunkHit()
            GameTheme.KARHUTLA_BORNEO -> synthesizeBorneoHit()
            GameTheme.ORIGINAL -> synthesizeOriginalHit()
        }

        // ---------------------------------------------------------------------
        // 1. BLOK M — Smooth Jaksel City-Pop / Lo-Fi Rhodes & MRT Chime Vibes
        // ---------------------------------------------------------------------

        private fun synthesizeBlokMFlap(): ShortArray {
            val count = (SAMPLE_RATE * 0.095).toInt()
            val samples = ShortArray(count)
            var phase = 0.0
            var subPhase = 0.0
            for (i in 0 until count) {
                val t = i.toDouble() / count
                val freq = 340.0 + 250.0 * sin(t * PI * 0.5)
                phase += 2.0 * PI * freq / SAMPLE_RATE
                subPhase += 2.0 * PI * (freq * 1.5) / SAMPLE_RATE
                val env = sin(PI * t) * exp(-2.2 * t)
                // Warm Rhodes-like tine + soft fundamental
                val wave = sin(phase) * 0.72 + sin(subPhase) * 0.28 * exp(-5.0 * t)
                samples[i] = clampToShort(wave * env * 0.58)
            }
            return samples
        }

        private fun synthesizeBlokMScore(): ShortArray {
            // Smooth MRT/City-pop Maj7 chime: F5 -> A5 -> C6
            val count = (SAMPLE_RATE * 0.25).toInt()
            val samples = ShortArray(count)
            val notes = doubleArrayOf(698.46, 880.0, 1046.50)
            val step = count / 3
            var p1 = 0.0
            var p2 = 0.0
            for (i in 0 until count) {
                val idx = (i / step).coerceAtMost(2)
                val localT = (i - idx * step).toDouble() / step
                val freq = notes[idx]
                p1 += 2.0 * PI * freq / SAMPLE_RATE
                p2 += 2.0 * PI * (freq * 2.0) / SAMPLE_RATE
                val env = (1.0 - exp(-28.0 * localT)) * exp(-3.4 * localT)
                // Subtle warm tremolo
                val trem = 0.92 + 0.08 * sin(2.0 * PI * 6.5 * i / SAMPLE_RATE)
                val wave = (sin(p1) * 0.78 + sin(p2) * 0.22 * exp(-6.0 * localT)) * trem
                samples[i] = clampToShort(wave * env * 0.60)
            }
            return samples
        }

        private fun synthesizeBlokMHit(): ShortArray {
            // Lo-fi sub kick thump + muted vinyl pop
            val count = (SAMPLE_RATE * 0.20).toInt()
            val samples = ShortArray(count)
            var phase = 0.0
            var seed = 4201
            for (i in 0 until count) {
                val t = i.toDouble() / count
                val freq = 145.0 * exp(-3.2 * t) + 48.0
                phase += 2.0 * PI * freq / SAMPLE_RATE
                seed = seed * 1664525 + 1013904223
                val noise = ((seed shr 16) and 0xFFFF) / 32768.0 - 1.0
                val env = exp(-6.5 * t)
                val thump = tanh(sin(phase) * 1.4) * 0.78 + noise * 0.22 * exp(-10.0 * t)
                samples[i] = clampToShort(thump * env * 0.78)
            }
            return samples
        }

        // ---------------------------------------------------------------------
        // 2. GTA 6 — 80s Vice City Synthwave / Retro Neon Pop
        // ---------------------------------------------------------------------

        private fun synthesizeGta6Flap(): ShortArray {
            // Glossy detuned 80s analog synth sweep
            val count = (SAMPLE_RATE * 0.10).toInt()
            val samples = ShortArray(count)
            var p1 = 0.0
            var p2 = 0.0
            for (i in 0 until count) {
                val t = i.toDouble() / count
                val freq = 260.0 + 490.0 * t * t
                p1 += 2.0 * PI * freq / SAMPLE_RATE
                p2 += 2.0 * PI * (freq * 1.012) / SAMPLE_RATE
                val saw1 = sin(p1) + 0.45 * sin(2.0 * p1) + 0.22 * sin(3.0 * p1)
                val saw2 = sin(p2) + 0.45 * sin(2.0 * p2)
                val env = sin(PI * t) * exp(-2.0 * t)
                samples[i] = clampToShort((saw1 + saw2) * 0.34 * env * 0.62)
            }
            return samples
        }

        private fun synthesizeGta6Score(): ShortArray {
            // Shimmering 80s synthwave arpeggio: E5 -> G#5 -> B5 -> E6
            val count = (SAMPLE_RATE * 0.26).toInt()
            val samples = ShortArray(count)
            val notes = doubleArrayOf(659.25, 830.61, 987.77, 1318.51)
            val step = count / 4
            var p1 = 0.0
            var p2 = 0.0
            for (i in 0 until count) {
                val idx = (i / step).coerceAtMost(3)
                val localT = (i - idx * step).toDouble() / step
                val freq = notes[idx]
                p1 += 2.0 * PI * freq / SAMPLE_RATE
                p2 += 2.0 * PI * (freq * 1.008) / SAMPLE_RATE
                val env = exp(-3.2 * localT)
                val wave = (sin(p1) + sin(p2)) * 0.42 + sin(p1 * 2.0) * 0.16
                samples[i] = clampToShort(wave * env * 0.62)
            }
            return samples
        }

        private fun synthesizeGta6Hit(): ShortArray {
            // Punchy 80s gated-reverb synth boom + crisp retro snare
            val count = (SAMPLE_RATE * 0.24).toInt()
            val samples = ShortArray(count)
            var phase = 0.0
            var seed = 1986
            for (i in 0 until count) {
                val t = i.toDouble() / count
                val freq = 175.0 * (1.0 - 0.72 * t)
                phase += 2.0 * PI * freq / SAMPLE_RATE
                seed = seed * 1664525 + 1013904223
                val noise = ((seed shr 16) and 0xFFFF) / 32768.0 - 1.0
                val gateEnv = if (t < 0.68) exp(-3.8 * t) else exp(-18.0 * (t - 0.68)) * exp(-3.8 * 0.68)
                val mixed = sin(phase) * 0.58 + noise * 0.42 * exp(-4.0 * t)
                samples[i] = clampToShort(mixed * gateEnv * 0.80)
            }
            return samples
        }

        // ---------------------------------------------------------------------
        // 3. CYBERPUNK — Night City Sci-Fi Thruster, Data Ping & EMP Glitch
        // ---------------------------------------------------------------------

        private fun synthesizeCyberpunkFlap(): ShortArray {
            // High-tech FM plasma thruster pulse
            val count = (SAMPLE_RATE * 0.09).toInt()
            val samples = ShortArray(count)
            var carrier = 0.0
            var mod = 0.0
            for (i in 0 until count) {
                val t = i.toDouble() / count
                val freq = 420.0 + 680.0 * t
                carrier += 2.0 * PI * freq / SAMPLE_RATE
                mod += 2.0 * PI * (freq * 0.5) / SAMPLE_RATE
                val fm = sin(carrier + 1.8 * sin(mod) * (1.0 - t))
                val env = sin(PI * t) * exp(-2.6 * t)
                samples[i] = clampToShort(fm * env * 0.56)
            }
            return samples
        }

        private fun synthesizeCyberpunkScore(): ShortArray {
            // Futuristic cybernetic dual-harmonic data chime
            val count = (SAMPLE_RATE * 0.21).toInt()
            val split = (count * 0.45).toInt()
            val samples = ShortArray(count)
            var p = 0.0
            var ring = 0.0
            for (i in 0 until count) {
                val freq = if (i < split) 1174.66 else 1760.0 // D6 -> A6
                p += 2.0 * PI * freq / SAMPLE_RATE
                ring += 2.0 * PI * (freq * 1.414) / SAMPLE_RATE
                val localT = if (i < split) i.toDouble() / split else (i - split).toDouble() / (count - split)
                val env = exp(-4.8 * localT)
                val wave = sin(p) * 0.72 + (sin(p) * sin(ring)) * 0.28
                samples[i] = clampToShort(wave * env * 0.58)
            }
            return samples
        }

        private fun synthesizeCyberpunkHit(): ShortArray {
            // Bit-crushed EMP glitch + descending cyber power-down
            val count = (SAMPLE_RATE * 0.24).toInt()
            val samples = ShortArray(count)
            var phase = 0.0
            var seed = 2077
            var held = 0.0
            for (i in 0 until count) {
                val t = i.toDouble() / count
                val freq = 340.0 * exp(-4.2 * t) + 42.0
                phase += 2.0 * PI * freq / SAMPLE_RATE
                seed = seed * 1664525 + 1013904223
                val noise = ((seed shr 16) and 0xFFFF) / 32768.0 - 1.0
                // Sample-and-hold bitcrush every 4 samples for crisp digital glitch
                if (i % 4 == 0) {
                    val raw = tanh((sin(phase) * 0.62 + noise * 0.38) * 2.2)
                    held = (raw * 8.0).toInt() / 8.0
                }
                val env = exp(-5.8 * t)
                samples[i] = clampToShort(held * env * 0.78)
            }
            return samples
        }

        // ---------------------------------------------------------------------
        // 4. KARHUTLA BORNEO — Hornbill Wing Whoosh, Sape' Chime & Ember Impact
        // ---------------------------------------------------------------------

        private fun synthesizeBorneoFlap(): ShortArray {
            // Deep organic hornbill wing air-whoosh + warm bamboo resonance
            val count = (SAMPLE_RATE * 0.115).toInt()
            val samples = ShortArray(count)
            var phase = 0.0
            var seed = 8081
            var filteredNoise = 0.0
            for (i in 0 until count) {
                val t = i.toDouble() / count
                val freq = 195.0 + 135.0 * sin(PI * t)
                phase += 2.0 * PI * freq / SAMPLE_RATE
                seed = seed * 1664525 + 1013904223
                val white = ((seed shr 16) and 0xFFFF) / 32768.0 - 1.0
                filteredNoise = filteredNoise * 0.82 + white * 0.18 // Low-pass breathy wind
                val env = sin(PI * t) * exp(-1.8 * t)
                val wave = sin(phase) * 0.55 + filteredNoise * 1.15
                samples[i] = clampToShort(wave * env * 0.62)
            }
            return samples
        }

        private fun synthesizeBorneoScore(): ShortArray {
            // Warm pentatonic Borneo Sape' plucked string interval (D5 -> F#5 -> A5)
            val count = (SAMPLE_RATE * 0.28).toInt()
            val samples = ShortArray(count)
            val notes = doubleArrayOf(587.33, 739.99, 880.0)
            val step = count / 3
            var p1 = 0.0
            var p2 = 0.0
            var p3 = 0.0
            for (i in 0 until count) {
                val idx = (i / step).coerceAtMost(2)
                val localT = (i - idx * step).toDouble() / step
                val freq = notes[idx]
                p1 += 2.0 * PI * freq / SAMPLE_RATE
                p2 += 2.0 * PI * (freq * 2.0) / SAMPLE_RATE
                p3 += 2.0 * PI * (freq * 3.0) / SAMPLE_RATE
                // Plucked string attack + resonant woody body decay
                val env = (1.0 - exp(-45.0 * localT)) * exp(-3.6 * localT)
                val pluck = sin(p1) * 0.68 + sin(p2) * 0.24 * exp(-5.0 * localT) + sin(p3) * 0.08 * exp(-9.0 * localT)
                samples[i] = clampToShort(pluck * env * 0.64)
            }
            return samples
        }

        private fun synthesizeBorneoHit(): ShortArray {
            // Deep hollow ironwood thud + crackling wildfire ember burst
            val count = (SAMPLE_RATE * 0.23).toInt()
            val samples = ShortArray(count)
            var phase = 0.0
            var seed = 9191
            for (i in 0 until count) {
                val t = i.toDouble() / count
                val freq = 128.0 * (1.0 - 0.55 * t)
                phase += 2.0 * PI * freq / SAMPLE_RATE
                seed = seed * 1664525 + 1013904223
                val r = ((seed shr 16) and 0xFFFF) / 32768.0 - 1.0
                // Intermittent fire crackle pops
                val crackle = if (kotlin.math.abs(r) > 0.86) r * 1.4 else r * 0.25
                val env = exp(-5.6 * t)
                val mixed = sin(phase) * 0.68 + crackle * 0.32
                samples[i] = clampToShort(mixed * env * 0.80)
            }
            return samples
        }

        // ---------------------------------------------------------------------
        // 5. ORIGINAL — Classic Crisp 8-Bit Arcade Flappy Bird Audio
        // ---------------------------------------------------------------------

        private fun synthesizeOriginalFlap(): ShortArray {
            val count = (SAMPLE_RATE * 0.085).toInt()
            val samples = ShortArray(count)
            var phase = 0.0
            for (i in 0 until count) {
                val t = i.toDouble() / count
                val freq = 300.0 + 380.0 * t
                phase += 2.0 * PI * freq / SAMPLE_RATE
                val env = sin(PI * t) * exp(-2.5 * t)
                samples[i] = clampToShort(sin(phase) * env * 0.55)
            }
            return samples
        }

        private fun synthesizeOriginalScore(): ShortArray {
            val count = (SAMPLE_RATE * 0.19).toInt()
            val split = (count * 0.42).toInt()
            val samples = ShortArray(count)
            var phase = 0.0
            for (i in 0 until count) {
                val freq = if (i < split) 987.77 else 1318.51 // Classic B5 -> E6 arcade ding
                phase += 2.0 * PI * freq / SAMPLE_RATE
                val localT = if (i < split) i.toDouble() / split else (i - split).toDouble() / (count - split)
                val env = exp(-4.0 * localT)
                val wave = sin(phase) * 0.8 + sin(phase * 2.0) * 0.2
                samples[i] = clampToShort(wave * env * 0.55)
            }
            return samples
        }

        private fun synthesizeOriginalHit(): ShortArray {
            val count = (SAMPLE_RATE * 0.18).toInt()
            val samples = ShortArray(count)
            var phase = 0.0
            var seed = 1337
            for (i in 0 until count) {
                val t = i.toDouble() / count
                val freq = 180.0 * (1.0 - 0.65 * t)
                phase += 2.0 * PI * freq / SAMPLE_RATE
                seed = seed * 1664525 + 1013904223
                val noise = ((seed shr 16) and 0xFFFF) / 32768.0 - 1.0
                val env = exp(-7.5 * t)
                val mixed = sin(phase) * 0.65 + noise * 0.35
                samples[i] = clampToShort(mixed * env * 0.75)
            }
            return samples
        }

        private fun clampToShort(value: Double): Short {
            val scaled = (value * Short.MAX_VALUE).toInt()
            return scaled.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }
}
