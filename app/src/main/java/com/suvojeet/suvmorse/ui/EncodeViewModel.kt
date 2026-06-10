package com.suvojeet.suvmorse.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmorse.audio.FeedbackController
import com.suvojeet.suvmorse.audio.MorsePlayer
import com.suvojeet.suvmorse.data.SettingsStore
import com.suvojeet.suvmorse.morse.MorseCode
import com.suvojeet.suvmorse.morse.MorseTiming
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Holds the state for the Encode/Play screen. */
class EncodeViewModel(app: Application) : AndroidViewModel(app) {

    private val player = MorsePlayer()
    private val feedback = FeedbackController(app)
    private val settings = SettingsStore(app)
    private var playJob: Job? = null

    var input by mutableStateOf("")
        private set
    var wpm by mutableIntStateOf(settings.wpm)
        private set
    var frequency by mutableStateOf(settings.frequency.toDouble())
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var torchEnabled by mutableStateOf(settings.torch)
        private set
    var hapticEnabled by mutableStateOf(settings.haptic)
        private set
    var loopEnabled by mutableStateOf(settings.loop)
        private set

    /** When on, plays a ~16 kHz near-inaudible tone instead of the audible pitch. */
    var silentMode by mutableStateOf(settings.silent)
        private set

    /** Ordinal of the dot/dash currently sounding, or -1 when idle. Drives UI highlighting. */
    var currentSymbol by mutableIntStateOf(-1)
        private set

    val morse: String get() = MorseCode.encode(input)
    val charCount: Int get() = input.length
    val maxChars: Int get() = MorseCode.MAX_INPUT_LENGTH
    val wordCount: Int
        get() = input.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    val torchAvailable: Boolean get() = feedback.hasTorch()
    val hapticAvailable: Boolean get() = feedback.hasVibrator()

    /** Frequency actually sent to the speaker: 16 kHz in silent mode, else the chosen pitch. */
    val effectiveFrequency: Double get() = if (silentMode) SILENT_FREQUENCY_HZ else frequency

    fun onInputChange(text: String) {
        input = text.take(MorseCode.MAX_INPUT_LENGTH)
    }

    fun appendQuick(text: String) {
        input = (input + text).take(MorseCode.MAX_INPUT_LENGTH)
    }

    fun updateWpm(value: Int) {
        wpm = value.coerceIn(5, 40)
        settings.wpm = wpm
    }

    fun updateFrequency(value: Double) {
        frequency = value.coerceIn(300.0, 1200.0)
        settings.frequency = frequency.toFloat()
    }

    fun toggleTorch() { torchEnabled = !torchEnabled; settings.torch = torchEnabled }

    fun toggleHaptic() { hapticEnabled = !hapticEnabled; settings.haptic = hapticEnabled }

    fun toggleLoop() { loopEnabled = !loopEnabled; settings.loop = loopEnabled }

    fun toggleSilent() { silentMode = !silentMode; settings.silent = silentMode }

    fun togglePlay() = if (isPlaying) stop() else play()

    fun play() {
        val signals = MorseTiming.buildSignals(input)
        if (signals.isEmpty()) return
        playJob?.cancel()
        val unit = MorseTiming.unitMillis(wpm)
        playJob = viewModelScope.launch {
            isPlaying = true
            try {
                do {
                    if (hapticEnabled) feedback.vibratePattern(signals, unit)
                    player.play(
                        signals = signals,
                        unitMillis = unit,
                        frequencyHz = effectiveFrequency,
                        onSymbol = { idx ->
                            currentSymbol = idx
                            if (torchEnabled) feedback.setTorch(idx >= 0)
                        }
                    )
                    if (loopEnabled && isActive) delay((unit * MorseTiming.WORD_GAP).toLong())
                } while (loopEnabled && isActive)
            } finally {
                isPlaying = false
                currentSymbol = -1
                feedback.stop()
            }
        }
    }

    fun stop() {
        playJob?.cancel()
        playJob = null
        feedback.stop()
    }

    fun clear() {
        stop()
        input = ""
    }

    override fun onCleared() {
        stop()
    }

    companion object {
        const val SILENT_FREQUENCY_HZ = 16_000.0
    }
}
