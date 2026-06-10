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
    val level: Float = 0f,            // normalised tone energy at the target frequency, 0..1
    val toneOn: Boolean = false,      // is a tone currently keyed down?
    val pending: String = "",         // dots/dashes of the letter being received
    val decoded: String = "",         // text decoded so far
    val unitMillis: Int = 0           // detector's current estimate of one Morse unit
)

/**
 * Listens to the microphone and decodes Morse code tones in real time.
 *
 * Detection pipeline per ~12 ms block:
 *  1. Goertzel filter isolates energy at [defaultFrequencyHz].
 *  2. An adaptive threshold (noise floor + fraction of the running peak, with hysteresis)
 *     decides tone on/off.
 *  3. On/off run lengths are classified into dot/dash and intra/letter/word gaps against an
 *     adaptively-estimated unit length, then decoded incrementally to text.
 *
 * Works best with a clear, steady tone (e.g. another device playing SuvMorse) in a quiet room.
 */
class MorseDetector(
    private val sampleRate: Int = 44_100,
    private val blockSize: Int = 512
) {
    companion object {
        const val DEFAULT_FREQUENCY_HZ = 700.0
        private const val INITIAL_UNIT_MS = 120.0
        private const val MIN_UNIT_MS = 40.0
        private const val MAX_UNIT_MS = 500.0
    }

    @SuppressLint("MissingPermission") // caller is responsible for the RECORD_AUDIO grant
    suspend fun listen(
        frequencyHz: Double = DEFAULT_FREQUENCY_HZ,
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

        // Pre-computed Goertzel coefficient for the target frequency.
        val omega = 2.0 * PI * frequencyHz / sampleRate
        val coeff = 2.0 * cos(omega)
        val blockMs = blockSize * 1000.0 / sampleRate
        val frac = (0.55 - sensitivity * 0.30).coerceIn(0.20, 0.60)

        val buffer = ShortArray(blockSize)

        // Adaptive detection state.
        var noiseFloor = 0.01
        var peak = 0.02
        var toneOn = false

        // Timing / decode state.
        var unitMs = INITIAL_UNIT_MS
        var runMs = 0.0
        val pending = StringBuilder()
        val decoded = StringBuilder()
        var letterFlushed = false
        var wordFlushed = false

        fun snapshot() = DetectorState(
            level = ((peakLevelOf - noiseFloor).coerceAtLeast(0.0)).toFloat(),
            toneOn = toneOn,
            pending = pending.toString(),
            decoded = decoded.toString(),
            unitMillis = unitMs.toInt()
        )

        try {
            record.startRecording()
            onUpdate(DetectorState(unitMillis = unitMs.toInt()))

            while (true) {
                coroutineContext.ensureActive()
                val n = record.read(buffer, 0, blockSize)
                if (n <= 0) continue
                val dt = n * 1000.0 / sampleRate

                // ── Goertzel power at the target frequency ──
                var s0: Double
                var s1 = 0.0
                var s2 = 0.0
                for (i in 0 until n) {
                    s0 = coeff * s1 - s2 + (buffer[i] / 32768.0)
                    s2 = s1
                    s1 = s0
                }
                val power = s1 * s1 + s2 * s2 - coeff * s1 * s2
                val level = (sqrt(power.coerceAtLeast(0.0)) * 2.0 / n).coerceIn(0.0, 1.0)
                peakLevelOf = level

                // ── Adaptive envelope ──
                peak = maxOf(peak * 0.997, level)
                if (!toneOn) noiseFloor = noiseFloor * 0.995 + level * 0.005
                val span = (peak - noiseFloor).coerceAtLeast(0.0)
                val gated = peak > noiseFloor * 2.5 + 0.004
                val onThresh = noiseFloor + span * frac
                val offThresh = noiseFloor + span * frac * 0.55

                val present = gated && if (toneOn) level > offThresh else level > onThresh

                if (present == toneOn) {
                    runMs += dt
                    // While silent, finalise the letter/word once enough silence has elapsed.
                    if (!toneOn) {
                        if (!letterFlushed && pending.isNotEmpty() && runMs >= unitMs * 2.0) {
                            val ch = MorseCode.decodeToken(pending.toString()) ?: '¿'
                            decoded.append(ch)
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
                    // Edge: a run just ended.
                    if (toneOn) {
                        // Tone ended -> classify dot/dash and adapt the unit estimate.
                        val isDot = runMs < unitMs * 2.0
                        pending.append(if (isDot) MorseCode.DOT else MorseCode.DASH)
                        val observedUnit = if (isDot) runMs else runMs / 3.0
                        unitMs = (unitMs * 0.7 + observedUnit * 0.3)
                            .coerceIn(MIN_UNIT_MS, MAX_UNIT_MS)
                        toneOn = false
                        letterFlushed = false
                        wordFlushed = false
                    } else {
                        // Silence ended -> a new tone begins.
                        toneOn = true
                    }
                    runMs = dt
                }
                onUpdate(snapshot())
            }
        } finally {
            // Flush any letter still being held when listening stops.
            if (pending.isNotEmpty()) {
                decoded.append(MorseCode.decodeToken(pending.toString()) ?: '¿')
                pending.setLength(0)
            }
            toneOn = false
            onUpdate(snapshot())
            runCatching { record.stop() }
            runCatching { record.release() }
        }
    }

    // Most recent normalised level; kept as a field so snapshot() can read it without re-deriving.
    private var peakLevelOf: Double = 0.0
}
