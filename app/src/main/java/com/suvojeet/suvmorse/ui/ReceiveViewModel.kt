package com.suvojeet.suvmorse.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmorse.audio.DetectorState
import com.suvojeet.suvmorse.audio.MorseDetector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Holds the state for the Receive/Decode screen. */
class ReceiveViewModel : ViewModel() {

    private val detector = MorseDetector()
    private var listenJob: Job? = null

    var isListening by mutableStateOf(false)
        private set
    var state by mutableStateOf(DetectorState())
        private set
    var sensitivity by mutableStateOf(0.5f)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun updateSensitivity(value: Float) {
        sensitivity = value.coerceIn(0f, 1f)
        if (isListening) restart()
    }

    /** Begin listening. The caller must already hold the RECORD_AUDIO permission. */
    fun start() {
        if (isListening) return
        error = null
        listenJob = viewModelScope.launch {
            isListening = true
            try {
                detector.listen(
                    sensitivity = sensitivity,
                    onUpdate = { snapshot -> state = snapshot }
                )
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                error = e.message ?: "Microphone error"
            } finally {
                isListening = false
            }
        }
    }

    fun stop() {
        listenJob?.cancel()
        listenJob = null
    }

    fun toggle() = if (isListening) stop() else start()

    private fun restart() {
        stop()
        start()
    }

    fun clear() {
        state = DetectorState(unitMillis = state.unitMillis)
    }

    override fun onCleared() {
        stop()
    }
}
