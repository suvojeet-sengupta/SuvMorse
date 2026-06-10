package com.suvojeet.suvmorse.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.suvojeet.suvmorse.morse.MorseSignal
import com.suvojeet.suvmorse.morse.MorseTiming
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.sin

/**
 * Plays a Morse [MorseSignal] sequence as audible tones using a streaming [AudioTrack].
 *
 * A short raised-cosine envelope is applied to each tone so keying doesn't produce clicks.
 * Playback runs on [Dispatchers.Default] and is fully cancellable — cancelling the coroutine
 * stops the audio immediately.
 */
class MorsePlayer(
    private val sampleRate: Int = 44_100
) {
    /**
     * Plays [signals]. [unitMillis] is the duration of one Morse unit, [frequencyHz] the tone
     * pitch. [onSymbol] is invoked on a background thread with the index of the dot/dash that is
     * currently sounding (-1 when no tone is playing).
     */
    suspend fun play(
        signals: List<MorseSignal>,
        unitMillis: Int,
        frequencyHz: Double,
        onSymbol: (Int) -> Unit
    ) = withContext(Dispatchers.Default) {
        if (signals.isEmpty()) return@withContext

        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        try {
            track.play()
            for (signal in signals) {
                coroutineContext.ensureActive()
                val durationMs = signal.units * unitMillis
                when (signal) {
                    is MorseSignal.Tone -> {
                        onSymbol(signal.symbolIndex)
                        writeTone(track, durationMs, frequencyHz)
                    }
                    is MorseSignal.Gap -> {
                        onSymbol(-1)
                        writeSilence(track, durationMs)
                    }
                }
            }
            // Let the buffered tail drain before we tear the track down.
            track.stop()
        } finally {
            onSymbol(-1)
            runCatching { track.flush() }
            runCatching { track.release() }
        }
    }

    private suspend fun writeTone(track: AudioTrack, durationMs: Int, frequencyHz: Double) {
        val totalSamples = (sampleRate.toLong() * durationMs / 1000L).toInt().coerceAtLeast(1)
        // ~5 ms raised-cosine ramp on each edge to avoid clicks.
        val ramp = (sampleRate * 0.005).toInt().coerceIn(1, totalSamples / 2 + 1)
        val twoPiF = 2.0 * PI * frequencyHz / sampleRate
        val chunk = ShortArray(2048)
        var written = 0
        while (written < totalSamples) {
            coroutineContext.ensureActive()
            val n = minOf(chunk.size, totalSamples - written)
            for (i in 0 until n) {
                val idx = written + i
                var amp = 0.85
                if (idx < ramp) amp *= 0.5 * (1 - kotlin.math.cos(PI * idx / ramp))
                else if (idx >= totalSamples - ramp) {
                    val t = totalSamples - idx
                    amp *= 0.5 * (1 - kotlin.math.cos(PI * t / ramp))
                }
                chunk[i] = (sin(twoPiF * idx) * amp * Short.MAX_VALUE).toInt().toShort()
            }
            track.write(chunk, 0, n)
            written += n
        }
    }

    private suspend fun writeSilence(track: AudioTrack, durationMs: Int) {
        val totalSamples = (sampleRate.toLong() * durationMs / 1000L).toInt().coerceAtLeast(1)
        val chunk = ShortArray(2048) // zero-filled
        var written = 0
        while (written < totalSamples) {
            coroutineContext.ensureActive()
            val n = minOf(chunk.size, totalSamples - written)
            track.write(chunk, 0, n)
            written += n
        }
    }
}
