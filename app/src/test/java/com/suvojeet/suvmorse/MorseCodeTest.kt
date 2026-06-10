package com.suvojeet.suvmorse

import com.suvojeet.suvmorse.morse.MorseCode
import com.suvojeet.suvmorse.morse.MorseSignal
import com.suvojeet.suvmorse.morse.MorseTiming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MorseCodeTest {

    @Test
    fun encodesLettersWithinAWord() {
        assertEquals(".... ..", MorseCode.encode("HI"))
    }

    @Test
    fun encodesWordsWithSlashSeparator() {
        assertEquals("... --- ... / -- .", MorseCode.encode("SOS me"))
    }

    @Test
    fun encodeIsCaseInsensitiveAndCollapsesWhitespace() {
        assertEquals(MorseCode.encode("HELLO WORLD"), MorseCode.encode("hello   world"))
    }

    @Test
    fun decodeRoundTripsEncode() {
        val text = "HELLO WORLD 123"
        assertEquals(text, MorseCode.decode(MorseCode.encode(text)))
    }

    @Test
    fun unitMillisFollowsParisStandard() {
        // 1200 / wpm
        assertEquals(100, MorseTiming.unitMillis(12))
        assertEquals(60, MorseTiming.unitMillis(20))
    }

    @Test
    fun signalsForEUseOneDotToneAndNoLeadingGap() {
        val signals = MorseTiming.buildSignals("E") // E -> "."
        assertEquals(1, signals.size)
        val tone = signals.first() as MorseSignal.Tone
        assertTrue(tone.isDot)
        assertEquals(MorseTiming.DOT_UNITS, tone.units)
        assertEquals(0, tone.symbolIndex)
    }

    @Test
    fun signalsInsertWordGapBetweenWords() {
        // "EE" (two letters) vs "E E" (two words): the latter has a 7-unit word gap.
        val sameWord = MorseTiming.buildSignals("EE")
        val twoWords = MorseTiming.buildSignals("E E")
        val sameWordGap = (sameWord[1] as MorseSignal.Gap).units
        val twoWordsGap = (twoWords[1] as MorseSignal.Gap).units
        assertEquals(MorseTiming.LETTER_GAP, sameWordGap)
        assertEquals(MorseTiming.WORD_GAP, twoWordsGap)
    }
}
