package com.suvojeet.suvmorse.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmorse.audio.MorsePlayer
import com.suvojeet.suvmorse.morse.MorseTiming
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Plays the Morse for a single tapped character on the reference chart. */
class ChartViewModel(app: Application) : AndroidViewModel(app) {

    private val player = MorsePlayer()
    private var job: Job? = null

    fun play(symbol: Char) {
        val signals = MorseTiming.buildSignals(symbol.toString())
        if (signals.isEmpty()) return
        job?.cancel()
        job = viewModelScope.launch {
            player.play(
                signals = signals,
                unitMillis = MorseTiming.unitMillis(15),
                frequencyHz = 700.0,
                onSymbol = {}
            )
        }
    }

    override fun onCleared() {
        job?.cancel()
    }
}
