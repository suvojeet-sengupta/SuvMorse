package com.suvojeet.suvmorse.morse

/**
 * Pure, audio-independent Morse decoding state machine.
 *
 * You feed it a stream of tone-present/absent decisions with the elapsed time of each step
 * ([process]); it classifies dot/dash and intra/letter/word gaps against an adaptively-estimated
 * unit length and accumulates decoded text. Keeping this separate from [com.suvojeet.suvmorse.audio.MorseDetector]
 * makes the timing logic unit-testable without a microphone.
 *
 * Robustness choices:
 *  - tones shorter than [minToneMs] are treated as noise glitches and ignored
 *  - undecodable letter tokens are dropped silently (no "¿" spam in live output)
 */
class MorseDecodeEngine(
    initialUnitMs: Double = 120.0,
    private val minToneMs: Double = 30.0,
    private val minUnitMs: Double = 40.0,
    private val maxUnitMs: Double = 500.0
) {
    var unitMs: Double = initialUnitMs
        private set
    var toneOn: Boolean = false
        private set

    private var runMs = 0.0
    private val pending = StringBuilder()
    private val decoded = StringBuilder()
    private var letterFlushed = false
    private var wordFlushed = false

    val pendingText: String get() = pending.toString()
    val decodedText: String get() = decoded.toString()

    /** Advance the machine by one analysis step. [dtMs] is the duration that step represented. */
    fun process(present: Boolean, dtMs: Double) {
        if (present == toneOn) {
            runMs += dtMs
            if (!toneOn) {
                // We're in silence: finalise the letter, then the word, as gaps grow.
                if (!letterFlushed && pending.isNotEmpty() && runMs >= unitMs * 2.0) {
                    flushLetter()
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
                // A tone just ended.
                if (runMs >= minToneMs) {
                    val isDot = runMs < unitMs * 2.0
                    pending.append(if (isDot) MorseCode.DOT else MorseCode.DASH)
                    val observedUnit = if (isDot) runMs else runMs / 3.0
                    unitMs = (unitMs * 0.7 + observedUnit * 0.3).coerceIn(minUnitMs, maxUnitMs)
                }
                // else: too short to be a real key-down — ignore as noise.
                toneOn = false
                letterFlushed = false
                wordFlushed = false
            } else {
                toneOn = true
            }
            runMs = dtMs
        }
    }

    /** Flush any letter still being held (e.g. when listening stops). */
    fun finish() {
        if (pending.isNotEmpty()) flushLetter()
        toneOn = false
    }

    private fun flushLetter() {
        MorseCode.decodeToken(pending.toString())?.let { decoded.append(it) }
        pending.setLength(0)
        letterFlushed = true
    }
}
