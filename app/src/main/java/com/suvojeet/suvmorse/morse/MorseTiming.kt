package com.suvojeet.suvmorse.morse

/** A single element of a Morse transmission: either a [Tone] (key down) or a [Gap] (key up). */
sealed interface MorseSignal {
    val units: Int

    /**
     * A tone (key down). [symbolIndex] is the 0-based ordinal of this dot/dash across the
     * whole message, used by the UI to highlight the symbol currently being played.
     * [isDot] distinguishes a dot (1 unit) from a dash (3 units).
     */
    data class Tone(override val units: Int, val symbolIndex: Int, val isDot: Boolean) : MorseSignal

    /** Silence (key up). */
    data class Gap(override val units: Int) : MorseSignal
}

object MorseTiming {
    // Standard PARIS timing, expressed in "units".
    const val DOT_UNITS = 1
    const val DASH_UNITS = 3
    const val INTRA_CHAR_GAP = 1   // between symbols of the same letter
    const val LETTER_GAP = 3       // between letters
    const val WORD_GAP = 7         // between words

    /** Milliseconds for a single unit at the given words-per-minute (PARIS standard). */
    fun unitMillis(wpm: Int): Int = (1200.0 / wpm).toInt().coerceAtLeast(1)

    /**
     * Builds the on/off signal sequence for [text]. The dot/dash ordinals in the returned
     * [MorseSignal.Tone]s line up with the order of dots/dashes in [MorseCode.encode] (ignoring
     * spaces and slashes), so the UI can highlight them as they play.
     */
    fun buildSignals(text: String): List<MorseSignal> {
        val signals = mutableListOf<MorseSignal>()
        val words = text.trim().uppercase()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }

        var symbolIndex = 0
        var firstWord = true
        for (word in words) {
            val codes = word.mapNotNull { MorseCode.charToMorse[it] }
            if (codes.isEmpty()) continue
            if (!firstWord) signals += MorseSignal.Gap(WORD_GAP)
            firstWord = false

            codes.forEachIndexed { letterIdx, code ->
                if (letterIdx > 0) signals += MorseSignal.Gap(LETTER_GAP)
                code.forEachIndexed { symIdx, s ->
                    if (symIdx > 0) signals += MorseSignal.Gap(INTRA_CHAR_GAP)
                    val isDot = s == MorseCode.DOT
                    signals += MorseSignal.Tone(
                        units = if (isDot) DOT_UNITS else DASH_UNITS,
                        symbolIndex = symbolIndex++,
                        isDot = isDot
                    )
                }
            }
        }
        return signals
    }
}
