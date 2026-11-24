package com.mybird.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Ultra-lightweight procedural arcade SFX synthesizer backed by Android [SoundPool].
 * Generates tiny PCM16 WAV clips in cacheDir once on [Dispatchers.IO] so the APK
 * needs zero binary audio assets while delivering crisp low-latency game audio.
 */
class RetroSoundEngine(private val context: Context) {

    private var soundPool: SoundPool? = null
    private var flapSoundId: Int = 0
    private var scoreSoundId: Int = 0
    private var hitSoundId: Int = 0
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
                .setMaxStreams(4)
                .setAudioAttributes(attrs)
                .build()

            val flapFile = ensureWavFile("sfx_flap.wav") { synthesizeFlapPcm() }
            val scoreFile = ensureWavFile("sfx_score.wav") { synthesizeScorePcm() }
            val hitFile = ensureWavFile("sfx_hit.wav") { synthesizeHitPcm() }

            flapSoundId = pool.load(flapFile.absolutePath, 1)
            scoreSoundId = pool.load(scoreFile.absolutePath, 1)
            hitSoundId = pool.load(hitFile.absolutePath, 1)
            soundPool = pool
            ready = true
        }
    }

    fun playFlap() {
        if (!isEnabled || !ready || flapSoundId == 0) return
        soundPool?.play(flapSoundId, 0.55f, 0.55f, 1, 0, 1.0f)
    }

    fun playScore() {
        if (!isEnabled || !ready || scoreSoundId == 0) return
        soundPool?.play(scoreSoundId, 0.70f, 0.70f, 2, 0, 1.0f)
    }

    fun playHit() {
        if (!isEnabled || !ready || hitSoundId == 0) return
        soundPool?.play(hitSoundId, 0.85f, 0.85f, 3, 0, 1.0f)
    }

    fun release() {
        ready = false
        soundPool?.release()
        soundPool = null
        flapSoundId = 0
        scoreSoundId = 0
        hitSoundId = 0
    }

    private fun ensureWavFile(fileName: String, generator: () -> ShortArray): File {
        val file = File(context.cacheDir, fileName)
        if (!file.exists() || file.length() <= 44L) {
            writePcmWav(file, generator(), SAMPLE_RATE)
        }
        return file
    }

    private fun synthesizeFlapPcm(): ShortArray {
        val durationSec = 0.085
        val count = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val freq = 300.0 + 380.0 * t
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val env = sin(PI * t) * exp(-2.5 * t)
            samples[i] = (sin(phase) * env * Short.MAX_VALUE * 0.55).toInt().toShort()
        }
        return samples
    }

    private fun synthesizeScorePcm(): ShortArray {
        val durationSec = 0.19
        val count = (SAMPLE_RATE * durationSec).toInt()
        val split = (count * 0.42).toInt()
        val samples = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val freq = if (i < split) 987.77 else 1318.51 // B5 -> E6 classic arcade ding
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val localT = if (i < split) i.toDouble() / split else (i - split).toDouble() / (count - split)
            val env = exp(-4.0 * localT)
            val wave = sin(phase) * 0.8 + sin(phase * 2.0) * 0.2
            samples[i] = (wave * env * Short.MAX_VALUE * 0.55).toInt().toShort()
        }
        return samples
    }

    private fun synthesizeHitPcm(): ShortArray {
        val durationSec = 0.18
        val count = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(count)
        var phase = 0.0
        var seed = 1337
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val freq = 180.0 * (1.0 - 0.65 * t)
            phase += 2.0 * PI * freq / SAMPLE_RATE
            // Deterministic lightweight noise for crisp punch
            seed = seed * 1664525 + 1013904223
            val noise = ((seed shr 16) and 0xFFFF) / 32768.0 - 1.0
            val env = exp(-7.5 * t)
            val mixed = sin(phase) * 0.65 + noise * 0.35
            samples[i] = (mixed * env * Short.MAX_VALUE * 0.75).toInt().toShort()
        }
        return samples
    }

    private fun writePcmWav(file: File, pcm: ShortArray, sampleRate: Int) {
        val dataBytes = pcm.size * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(36 + dataBytes)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16) // PCM chunk size
            putShort(1) // PCM format
            putShort(1) // Mono
            putInt(sampleRate)
            putInt(sampleRate * 2) // Byte rate
            putShort(2) // Block align
            putShort(16) // Bits per sample
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
        private const val SAMPLE_RATE = 22050
    }
}
