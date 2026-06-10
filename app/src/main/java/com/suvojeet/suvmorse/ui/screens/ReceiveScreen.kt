package com.suvojeet.suvmorse.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suvojeet.suvmorse.ui.ReceiveViewModel
import com.suvojeet.suvmorse.ui.components.LabeledSlider
import com.suvojeet.suvmorse.ui.components.LevelMeter
import com.suvojeet.suvmorse.ui.components.SectionCard
import com.suvojeet.suvmorse.ui.components.ShareButtons
import com.suvojeet.suvmorse.ui.components.WaveformView

@Composable
fun ReceiveScreen(
    modifier: Modifier = Modifier,
    showMessage: (String) -> Unit = {},
    vm: ReceiveViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) vm.start()
    }

    // Stop the mic whenever this screen leaves the composition (e.g. switching tabs).
    DisposableEffect(Unit) {
        onDispose { vm.stop() }
    }

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ListenButton(
            isListening = vm.isListening,
            onClick = {
                if (hasPermission) {
                    vm.toggle()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        )

        Text(
            text = when {
                !hasPermission -> "Tap to grant microphone access and start listening."
                vm.state.calibrating -> "Calibrating to the room… stay quiet for a moment."
                vm.isListening -> "Listening… point the mic at a Morse tone."
                else -> "Tap the mic to start decoding Morse tones."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        vm.error?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        SectionCard(title = "Live signal") {
            WaveformView(levels = vm.state.waveform, active = vm.state.toneOn)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ToneDot(active = vm.state.toneOn)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    LevelMeter(level = vm.state.level, active = vm.state.toneOn)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = buildString {
                            append("Unit ≈ ${vm.state.unitMillis} ms")
                            if (vm.state.detectedFrequency > 0) {
                                append("  ·  ${vm.state.detectedFrequency} Hz")
                            }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = vm.state.pending.ifEmpty { "·" },
                fontFamily = FontFamily.Monospace,
                fontSize = 30.sp,
                letterSpacing = 6.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        SectionCard(
            title = "Decoded",
            trailing = {
                Row {
                    IconButton(
                        onClick = {
                            if (vm.state.decoded.isNotBlank()) {
                                clipboard.setText(AnnotatedString(vm.state.decoded))
                                showMessage("Decoded text copied")
                            }
                        },
                        enabled = vm.state.decoded.isNotBlank()
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy text")
                    }
                    IconButton(
                        onClick = vm::clear,
                        enabled = vm.state.decoded.isNotEmpty() || vm.state.pending.isNotEmpty()
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Clear")
                    }
                }
            }
        ) {
            Text(
                text = vm.state.decoded.ifBlank { "Decoded text will appear here…" },
                style = MaterialTheme.typography.titleMedium,
                color = if (vm.state.decoded.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            )
        }

        SectionCard(title = "Tuning") {
            LabeledSlider(
                label = "Sensitivity",
                valueText = "${(vm.sensitivity * 100).toInt()}%",
                value = vm.sensitivity,
                onValueChange = vm::updateSensitivity,
                valueRange = 0f..1f
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Pitch is detected automatically (400–1000 Hz). Raise sensitivity for faint or " +
                    "distant tones; lower it in a noisy room.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionCard(title = "Share") {
            ShareButtons(
                text = vm.state.decoded,
                enabled = vm.state.decoded.isNotBlank(),
                showMessage = showMessage
            )
        }
    }
}

@Composable
private fun ListenButton(isListening: Boolean, onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "scale"
    )
    val container = if (isListening) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.primary
    val onContainer = if (isListening) MaterialTheme.colorScheme.onError
    else MaterialTheme.colorScheme.onPrimary

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
        if (isListening) {
            Box(
                Modifier
                    .size(130.dp)
                    .scale(scale)
                    .background(container.copy(alpha = 0.18f), CircleShape)
            )
        }
        Box(
            Modifier
                .size(108.dp)
                .background(container, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription = if (isListening) "Stop listening" else "Start listening",
                tint = onContainer,
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

@Composable
private fun ToneDot(active: Boolean) {
    val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(
        Modifier
            .size(20.dp)
            .background(color, CircleShape)
    )
}
