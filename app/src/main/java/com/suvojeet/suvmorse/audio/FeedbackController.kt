package com.suvojeet.suvmorse.audio

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.suvojeet.suvmorse.morse.MorseSignal

/**
 * Drives non-audio Morse feedback: the camera flashlight (toggled live in sync with tones) and
 * the vibrator (fired once as a full waveform for the whole message). All calls are best-effort
 * and never throw — missing hardware is simply ignored.
 */
class FeedbackController(context: Context) {

    private val appContext = context.applicationContext
    private val cameraManager =
        appContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private val torchCameraId: String? = runCatching {
        cameraManager?.cameraIdList?.firstOrNull { id ->
            val chars = cameraManager.getCameraCharacteristics(id)
            chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }.getOrNull()

    @Suppress("DEPRECATION")
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    fun hasTorch(): Boolean = torchCameraId != null

    fun hasVibrator(): Boolean = vibrator?.hasVibrator() == true

    /** Turn the flashlight on/off. Safe to call rapidly from a background thread. */
    fun setTorch(on: Boolean) {
        val id = torchCameraId ?: return
        runCatching { cameraManager?.setTorchMode(id, on) }
    }

    /** Fire the whole message as a single vibration waveform (no repeat). */
    @Suppress("DEPRECATION")
    fun vibratePattern(signals: List<MorseSignal>, unitMillis: Int) {
        val v = vibrator ?: return
        if (signals.isEmpty()) return
        // Waveform timings alternate OFF, ON, OFF, … . buildSignals starts with a Tone and always
        // alternates Tone/Gap, so a leading 0 ("no initial wait") lines the rest up correctly.
        val timings = LongArray(signals.size + 1)
        timings[0] = 0L
        signals.forEachIndexed { i, s -> timings[i + 1] = (s.units.toLong() * unitMillis) }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                v.vibrate(timings, -1)
            }
        }
    }

    /** Stop everything: flashlight off and cancel any running vibration. */
    fun stop() {
        setTorch(false)
        runCatching { vibrator?.cancel() }
    }
}
