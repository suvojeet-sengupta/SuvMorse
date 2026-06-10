package com.suvojeet.suvmorse

import com.suvojeet.suvmorse.morse.MorseDecodeEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class MorseDecodeEngineTest {

    /** Feeds [ms] of a constant tone-present decision in small steps, like real analysis blocks. */
    private fun MorseDecodeEngine.feed(present: Boolean, ms: Double, step: Double = 10.0) {
        var t = 0.0
        while (t < ms) {
            process(present, step)
            t += step
        }
    }

    @Test
    fun decodesA() {
        val e = MorseDecodeEngine(initialUnitMs = 100.0, minToneMs = 30.0)
        e.feed(present = true, ms = 100.0)   // dot
        e.feed(present = false, ms = 100.0)  // intra-letter gap (1 unit, no flush)
        e.feed(present = true, ms = 300.0)   // dash
        e.feed(present = false, ms = 600.0)  // letter + word gap
        e.finish()
        assertEquals("A", e.decodedText.trim())
    }

    @Test
    fun shortBlipIsIgnoredAsNoise() {
        val e = MorseDecodeEngine(initialUnitMs = 100.0, minToneMs = 30.0)
        e.feed(present = true, ms = 10.0)    // 10 ms < minTone -> noise
        e.feed(present = false, ms = 600.0)
        e.finish()
        assertEquals("", e.decodedText.trim())
    }

    @Test
    fun undecodableTokenIsDroppedNotMarked() {
        val e = MorseDecodeEngine(initialUnitMs = 100.0, minToneMs = 30.0)
        repeat(8) {
            e.feed(present = true, ms = 100.0)   // dot
            e.feed(present = false, ms = 100.0)  // intra gap
        }
        e.feed(present = false, ms = 600.0)      // long gap -> flush "........"
        e.finish()
        // Eight dots is not a valid character; it should be dropped, not rendered as "¿".
        assertEquals("", e.decodedText)
    }

    @Test
    fun decodesSosWord() {
        val e = MorseDecodeEngine(initialUnitMs = 100.0, minToneMs = 30.0)
        // S = ... , O = --- , S = ...
        val s = listOf(true to 100.0, false to 100.0, true to 100.0, false to 100.0, true to 100.0)
        val o = listOf(true to 300.0, false to 100.0, true to 300.0, false to 100.0, true to 300.0)
        s.forEach { e.feed(it.first, it.second) }
        e.feed(present = false, ms = 300.0) // letter gap
        o.forEach { e.feed(it.first, it.second) }
        e.feed(present = false, ms = 300.0) // letter gap
        s.forEach { e.feed(it.first, it.second) }
        e.feed(present = false, ms = 700.0) // end
        e.finish()
        assertEquals("SOS", e.decodedText.trim())
    }
}
