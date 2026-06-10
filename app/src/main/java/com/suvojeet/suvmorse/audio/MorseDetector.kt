package com.suvojeet.suvmorse.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.suvojeet.suvmorse.morse.MorseDecodeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

/** Snapshot of the detector emitted to the UI. */
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
    // Identity-based equals/hashCode are fine: a fresh snapshot is emitted on each update.
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

/**
 * Listens to the microphone and decodes Morse code tones in real time.
 *
 * Pipeline per ~12 ms block:
 *  1. A Goertzel filterbank (audible 400–1000 Hz plus ~16 kHz "silent mode" bins) finds the
 *     dominant tone and its energy.
 *  2. The first ~0.7 s calibrates the ambient noise floor.
 *  3. An adaptive threshold with hysteresis plus a 3-block median debounce decides tone on/off.
 *  4. [MorseDecodeEngine] turns the on/off stream into text.
 *
 * UI snapshots are throttled to ~25/s (plus an immediate emit whenever the decode state changes)
 * to avoid recomposing the screen on every audio block.
 */
class MorseDetector(
    private val sampleRate: Int = 44_100,
    private val blockSize: Int = 512
) {
    companion object {
        private const val BAND_MIN_HZ = 400
        private const val BAND_MAX_HZ = 1000
        private const val BAND_STEP_HZ = 50
        private const val WAVEFORM_POINTS = 96
        private const val CALIBRATION_MS = 700.0
        private const val EMIT_INTERVAL_MS = 40.0 // ~25 UI updates per second
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

        val record = openRecord(minBuf)
        check(record.state == AudioRecord.STATE_INITIALIZED) {
            "Could not initialise the microphone."
        }

        // Goertzel coefficients: audible band + high bins for the 16 kHz silent mode.
        val freqs = (BAND_MIN_HZ..BAND_MAX_HZ step BAND_STEP_HZ).toList() +
            listOf(15_500, 16_000, 16_500)
        val coeffs = DoubleArray(freqs.size) { 2.0 * cos(2.0 * PI * freqs[it] / sampleRate) }

        val blockMs = blockSize * 1000.0 / sampleRate
        val frac = (0.55 - sensitivity * 0.30).coerceIn(0.20, 0.60)
        val buffer = ShortArray(blockSize)
        val wave = ArrayDeque<Float>()

        val calibrationBlocks = (CALIBRATION_MS / blockMs).toInt().coerceAtLeast(10)
        var blockCount = 0
        var calibrating = true
        var noiseAccum = 0.0
        var calibSamples = 0

        var noiseFloor = 0.01
        var peak = 0.02
        var d0 = false; var d1 = false; var d2 = false
        var detectedFreq = 0

        val engine = MorseDecodeEngine()
        var msSinceEmit = 0.0
        var lastPendingLen = 0
        var lastDecodedLen = 0
        var lastToneOn = false

        fun pushWave(v: Float) {
            wave.addLast(v)
            while (wave.size > WAVEFORM_POINTS) wave.removeFirst()
        }

        fun snapshot(level: Double, calibratingNow: Boolean) = DetectorState(
            level = level.toFloat(),
            toneOn = engine.toneOn,
            pending = engine.pendingText,
            decoded = engine.decodedText,
            unitMillis = engine.unitMs.toInt(),
            detectedFrequency = if (engine.toneOn) detectedFreq else 0,
            calibrating = calibratingNow,
            waveform = wave.toFloatArray()
        )

        try {
            record.startRecording()
            onUpdate(DetectorState(calibrating = true, unitMillis = engine.unitMs.toInt()))

            while (true) {
                coroutineContext.ensureActive()
                val n = record.read(buffer, 0, blockSize)
                if (n <= 0) continue
                val dt = n * 1000.0 / sampleRate

                // Goertzel filterbank: dominant tone + its energy.
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

                if (calibrating) {
                    noiseAccum += level
                    calibSamples++
                    if (++blockCount >= calibrationBlocks) {
                        noiseFloor = (noiseAccum / calibSamples).coerceAtLeast(0.005)
                        peak = noiseFloor * 3
                        calibrating = false
                    }
                    msSinceEmit += dt
                    if (msSinceEmit >= EMIT_INTERVAL_MS) {
                        onUpdate(snapshot(level, calibratingNow = true))
                        msSinceEmit = 0.0
                    }
                    continue
                }

                // Adaptive envelope + hysteresis.
                peak = maxOf(peak * 0.997, level)
                if (!engine.toneOn) noiseFloor = noiseFloor * 0.995 + level * 0.005
                val span = (peak - noiseFloor).coerceAtLeast(0.0)
                val gated = peak > noiseFloor * 2.2 + 0.004
                val onThresh = noiseFloor + span * frac
                val offThresh = noiseFloor + span * frac * 0.55
                val raw = gated && if (engine.toneOn) level > offThresh else level > onThresh

                // 3-block median debounce.
                d0 = d1; d1 = d2; d2 = raw
                val present = (if (d0) 1 else 0) + (if (d1) 1 else 0) + (if (d2) 1 else 0) >= 2
                if (present) detectedFreq = bestFreq

                engine.process(present, dt)

                // Emit on any decode-relevant change, otherwise at the throttled interval.
                msSinceEmit += dt
                val changed = engine.toneOn != lastToneOn ||
                    engine.pendingText.length != lastPendingLen ||
                    engine.decodedText.length != lastDecodedLen
                if (changed || msSinceEmit >= EMIT_INTERVAL_MS) {
                    onUpdate(snapshot(level, calibratingNow = false))
                    msSinceEmit = 0.0
                    lastToneOn = engine.toneOn
                    lastPendingLen = engine.pendingText.length
                    lastDecodedLen = engine.decodedText.length
                }
            }
        } finally {
            engine.finish()
            onUpdate(snapshot(level = 0.0, calibratingNow = false))
            runCatching { record.stop() }
            runCatching { record.release() }
        }
    }

    /**
     * Prefer the UNPROCESSED source (no AGC/noise-suppression, best for the 16 kHz tone); fall
     * back to MIC if the device doesn't support it.
     */
    @SuppressLint("MissingPermission")
    private fun openRecord(minBuf: Int): AudioRecord {
        fun build(source: Int) = AudioRecord(
            source, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf
        )
        val unprocessed = runCatching { build(MediaRecorder.AudioSource.UNPROCESSED) }.getOrNull()
        if (unprocessed != null && unprocessed.state == AudioRecord.STATE_INITIALIZED) {
            return unprocessed
        }
        runCatching { unprocessed?.release() }
        return build(MediaRecorder.AudioSource.MIC)
    }
}
