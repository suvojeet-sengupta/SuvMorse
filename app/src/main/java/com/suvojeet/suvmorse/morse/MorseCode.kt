package com.suvojeet.suvmorse.morse

/**
 * International (ITU) Morse code dictionary plus encode/decode helpers and a
 * timing/signal model used by the player.
 *
 * Display conventions:
 *  - symbols within a letter are adjacent (e.g. "A" -> ".-")
 *  - letters are separated by a single space
 *  - words are separated by " / "
 */
object MorseCode {

    const val DOT = '.'
    const val DASH = '-'
    const val LETTER_SEPARATOR = " "
    const val WORD_SEPARATOR = " / "

    /** Maximum input length we accept. Generous, but bounded for performance/safety. */
    const val MAX_INPUT_LENGTH = 2000

    val charToMorse: Map<Char, String> = linkedMapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
        'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
        'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
        'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
        'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
        'Z' to "--..",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--",
        '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...",
        '8' to "---..", '9' to "----.",
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '\'' to ".----.",
        '!' to "-.-.--", '/' to "-..-.", '(' to "-.--.", ')' to "-.--.-",
        '&' to ".-...", ':' to "---...", ';' to "-.-.-.", '=' to "-...-",
        '+' to ".-.-.", '-' to "-....-", '_' to "..--.-", '"' to ".-..-.",
        '$' to "...-..-", '@' to ".--.-."
    )

    private val morseToChar: Map<String, Char> =
        charToMorse.entries.associate { (k, v) -> v to k }

    /** True if [c] (case-insensitive) can be represented in Morse. */
    fun isSupported(c: Char): Boolean =
        c.isWhitespace() || charToMorse.containsKey(c.uppercaseChar())

    /** Encodes plain text into a Morse string. Unsupported characters are dropped. */
    fun encode(text: String): String {
        val words = text.trim().uppercase()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }

        return words.joinToString(WORD_SEPARATOR) { word ->
            word.mapNotNull { ch -> charToMorse[ch] }
                .joinToString(LETTER_SEPARATOR)
        }
    }

    /** Decodes a Morse string (dots/dashes, " " between letters, "/" between words). */
    fun decode(morse: String): String {
        if (morse.isBlank()) return ""
        return morse.trim().split(Regex("\\s*/\\s*")).joinToString(" ") { word ->
            word.trim().split(Regex("\\s+"))
                .filter { it.isNotEmpty() }
                .map { token -> morseToChar[token] ?: '¿' } // ¿ marks an undecodable token
                .joinToString("")
        }.trim()
    }

    /** Decodes a single letter token (only dots/dashes) or returns null if unknown. */
    fun decodeToken(token: String): Char? = morseToChar[token]
}
