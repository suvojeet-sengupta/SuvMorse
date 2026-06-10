package com.suvojeet.suvmorse.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.suvojeet.suvmorse.morse.MorseCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

/** Snapshot of the detector emitted to the UI on every analysis block. */
data class DetectorState(
    val level: Float = 0f,            // normalised tone energy, 0..1
    val toneOn: Boolean = false,      // is a tone currently keyed down?
    val pending: String = "",         // dots/dashes of the letter being received
    val decoded: String = "",         // text decoded so far
    val unitMillis: Int = 0,          // detector's current estimate of one Morse unit
    val detectedFrequency: Int = 0,   // dominant tone frequency in Hz (0 when none)
    val calibrating: Boolean = false, // measuring the ambient noise floor
    val waveform: FloatArray = FloatArray(0) // recent level history for the visualiser
) {
    // Identity-based equals/hashCode are fine: a fresh snapshot is emitted every block.
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

/**
 * Listens to the microphone and decodes Morse code tones in real time.
 *
 * Pipeline per ~12 ms block:
 *  1. A Goertzel **filterbank** spanning 400–1000 Hz finds the dominant tone and its energy,
 *     so the user doesn't have to dial in the exact pitch.
 *  2. The first ~0.7 s **calibrates** the ambient noise floor.
 *  3. An adaptive threshold with hysteresis plus a 3-block **median debounce** decides tone
 *     on/off, rejecting jitter.
 *  4. On/off run lengths are classified into dot/dash and intra/letter/word gaps against an
 *     adaptively-estimated unit length, then decoded incrementally to text.
 */
class MorseDetector(
    private val sampleRate: Int = 44_100,
    private val blockSize: Int = 512
) {
    companion object {
        private const val BAND_MIN_HZ = 400
        private const val BAND_MAX_HZ = 1000
        private const val BAND_STEP_HZ = 50
        private const val INITIAL_UNIT_MS = 120.0
        private const val MIN_UNIT_MS = 40.0
        private const val MAX_UNIT_MS = 500.0
        private const val WAVEFORM_POINTS = 96
        private const val CALIBRATION_MS = 700.0
    }

    @SuppressLint("MissingPermission") // caller is responsible for the RECORD_AUDIO grant
    suspend fun listen(
        sensitivity: Float = 0.5f,
        onUpdate: (DetectorState) -> Unit
    ) = withContext(Dispatchers.Default) {
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(blockSize * 4)

        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuf
        )
        check(record.state == AudioRecord.STATE_INITIALIZED) {
            "Could not initialise the microphone."
        }

        // Pre-computed Goertzel coefficients for each frequency bin in the band.
        val freqs = (BAND_MIN_HZ..BAND_MAX_HZ step BAND_STEP_HZ).toList()
        val coeffs = DoubleArray(freqs.size) { 2.0 * cos(2.0 * PI * freqs[it] / sampleRate) }

        val blockMs = blockSize * 1000.0 / sampleRate
        val frac = (0.55 - sensitivity * 0.30).coerceIn(0.20, 0.60)
        val buffer = ShortArray(blockSize)
        val wave = ArrayDeque<Float>()

        // Calibration state.
        val calibrationBlocks = (CALIBRATION_MS / blockMs).toInt().coerceAtLeast(10)
        var blockCount = 0
        var calibrating = true
        var noiseAccum = 0.0
        var calibSamples = 0

        // Adaptive detection state.
        var noiseFloor = 0.01
        var peak = 0.02
        var toneOn = false
        var d0 = false; var d1 = false; var d2 = false // 3-block debounce window
        var detectedFreq = 0

        // Timing / decode state.
        var unitMs = INITIAL_UNIT_MS
        var runMs = 0.0
        val pending = StringBuilder()
        val decoded = StringBuilder()
        var letterFlushed = false
        var wordFlushed = false

        fun pushWave(v: Float) {
            wave.addLast(v)
            while (wave.size > WAVEFORM_POINTS) wave.removeFirst()
        }

        try {
            record.startRecording()
            onUpdate(DetectorState(calibrating = true, unitMillis = unitMs.toInt()))

            while (true) {
                coroutineContext.ensureActive()
                val n = record.read(buffer, 0, blockSize)
                if (n <= 0) continue
                val dt = n * 1000.0 / sampleRate

                // ── Goertzel filterbank: dominant tone + its energy ──
                var bestLevel = 0.0
                var bestFreq = 0
                for (b in coeffs.indices) {
                    var s1 = 0.0
                    var s2 = 0.0
                    val c = coeffs[b]
                    for (i in 0 until n) {
                        val s0 = c * s1 - s2 + buffer[i] / 32768.0
                        s2 = s1
                        s1 = s0
                    }
                    val powr = s1 * s1 + s2 * s2 - c * s1 * s2
                    val lvl = sqrt(powr.coerceAtLeast(0.0)) * 2.0 / n
                    if (lvl > bestLevel) {
                        bestLevel = lvl
                        bestFreq = freqs[b]
                    }
                }
                val level = bestLevel.coerceIn(0.0, 1.0)
                pushWave(level.toFloat())

                // ── Calibration: just learn the noise floor, don't decode yet ──
                if (calibrating) {
                    noiseAccum += level
                    calibSamples++
                    if (++blockCount >= calibrationBlocks) {
                        noiseFloor = (noiseAccum / calibSamples).coerceAtLeast(0.005)
                        peak = noiseFloor * 3
                        calibrating = false
                    }
                    onUpdate(
                        DetectorState(
                            level = level.toFloat(),
                            calibrating = true,
                            unitMillis = unitMs.toInt(),
                            waveform = wave.toFloatArray()
                        )
                    )
                    continue
                }

                // ── Adaptive envelope + hysteresis ──
                peak = maxOf(peak * 0.997, level)
                if (!toneOn) noiseFloor = noiseFloor * 0.995 + level * 0.005
                val span = (peak - noiseFloor).coerceAtLeast(0.0)
                val gated = peak > noiseFloor * 2.2 + 0.004
                val onThresh = noiseFloor + span * frac
                val offThresh = noiseFloor + span * frac * 0.55
                val raw = gated && if (toneOn) level > offThresh else level > onThresh

                // 3-block median debounce.
                d0 = d1; d1 = d2; d2 = raw
                val present = (if (d0) 1 else 0) + (if (d1) 1 else 0) + (if (d2) 1 else 0) >= 2
                if (present) detectedFreq = bestFreq

                if (present == toneOn) {
                    runMs += dt
                    if (!toneOn) {
                        if (!letterFlushed && pending.isNotEmpty() && runMs >= unitMs * 2.0) {
                            decoded.append(MorseCode.decodeToken(pending.toString()) ?: '¿')
                            pending.setLength(0)
                            letterFlushed = true
                        }
                        if (!wordFlushed && decoded.isNotEmpty() &&
                            !decoded.endsWith(" ") && runMs >= unitMs * 5.0
                        ) {
                            decoded.append(' ')
                            wordFlushed = true
                        }
                    }
                } else {
                    if (toneOn) {
                        // Tone ended -> classify dot/dash, adapt the unit estimate.
                        val isDot = runMs < unitMs * 2.0
                        pending.append(if (isDot) MorseCode.DOT else MorseCode.DASH)
                        val observedUnit = if (isDot) runMs else runMs / 3.0
                        unitMs = (unitMs * 0.7 + observedUnit * 0.3)
                            .coerceIn(MIN_UNIT_MS, MAX_UNIT_MS)
                        toneOn = false
                        letterFlushed = false
                        wordFlushed = false
                    } else {
                        toneOn = true
                    }
                    runMs = dt
                }

                onUpdate(
                    DetectorState(
                        level = level.toFloat(),
                        toneOn = toneOn,
                        pending = pending.toString(),
                        decoded = decoded.toString(),
                        unitMillis = unitMs.toInt(),
                        detectedFrequency = if (toneOn) detectedFreq else 0,
                        calibrating = false,
                        waveform = wave.toFloatArray()
                    )
                )
            }
        } finally {
            if (pending.isNotEmpty()) {
                decoded.append(MorseCode.decodeToken(pending.toString()) ?: '¿')
                pending.setLength(0)
            }
            onUpdate(
                DetectorState(
                    pending = "",
                    decoded = decoded.toString(),
                    unitMillis = unitMs.toInt(),
                    waveform = wave.toFloatArray()
                )
            )
            runCatching { record.stop() }
            runCatching { record.release() }
        }
    }
}
