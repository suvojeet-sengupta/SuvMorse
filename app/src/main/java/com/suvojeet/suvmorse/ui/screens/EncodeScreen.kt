package com.suvojeet.suvmorse.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suvojeet.suvmorse.ui.EncodeViewModel
import com.suvojeet.suvmorse.ui.components.LabeledSlider
import com.suvojeet.suvmorse.ui.components.MorseGlyphs
import com.suvojeet.suvmorse.ui.components.SectionCard
import com.suvojeet.suvmorse.ui.components.ShareButtons
import com.suvojeet.suvmorse.ui.components.TransmitBeacon

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EncodeScreen(
    modifier: Modifier = Modifier,
    showMessage: (String) -> Unit = {},
    vm: EncodeViewModel = viewModel()
) {
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val morse = vm.morse
    val hasMorse = morse.isNotEmpty()
    val nearLimit = vm.charCount >= vm.maxChars * 0.9
    val shareText = if (vm.input.isBlank()) "" else "${vm.input}\n\n$morse"

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionCard(title = "Message") {
            OutlinedTextField(
                value = vm.input,
                onValueChange = vm::onInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                placeholder = { Text("Type something to convert to Morse…") },
                supportingText = {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${vm.wordCount} words")
                        Text(
                            "${vm.charCount} / ${vm.maxChars}",
                            color = if (nearLimit) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (nearLimit) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.appendQuick("SOS ") }) { Text("SOS") }
                OutlinedButton(
                    onClick = { vm.clear() },
                    enabled = vm.input.isNotEmpty()
                ) { Text("Clear") }
            }
        }

        // ── Transmission hero ──
        SectionCard(
            title = "Morse",
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TransmitBeacon(active = vm.currentSymbol >= 0)
                    IconButton(
                        onClick = {
                            if (hasMorse) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                clipboard.setText(AnnotatedString(morse))
                                showMessage("Morse copied to clipboard")
                            }
                        },
                        enabled = hasMorse
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy Morse")
                    }
                }
            }
        ) {
            if (!hasMorse) {
                Text(
                    "Type above — your message becomes · — · · here, ready to play.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                MorseGlyphs(
                    morse = morse,
                    activeSymbol = vm.currentSymbol,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(18.dp))
            PlayButton(
                isPlaying = vm.isPlaying,
                enabled = hasMorse,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.togglePlay()
                }
            )
        }

        SectionCard(title = "Playback") {
            Text("Speed", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpeedPreset("Slow", 8, vm.wpm) { vm.updateWpm(it) }
                SpeedPreset("Medium", 15, vm.wpm) { vm.updateWpm(it) }
                SpeedPreset("Fast", 25, vm.wpm) { vm.updateWpm(it) }
            }
            Spacer(Modifier.height(8.dp))
            LabeledSlider(
                label = "Fine",
                valueText = "${vm.wpm} WPM",
                value = vm.wpm.toFloat(),
                onValueChange = { vm.updateWpm(it.toInt()) },
                valueRange = 5f..40f,
                steps = 34
            )
            if (!vm.silentMode) {
                Spacer(Modifier.height(12.dp))
                LabeledSlider(
                    label = "Pitch",
                    valueText = "${vm.frequency.toInt()} Hz",
                    value = vm.frequency.toFloat(),
                    onValueChange = { vm.updateFrequency(it.toDouble()) },
                    valueRange = 300f..1200f
                )
            }
            Spacer(Modifier.height(14.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = vm.silentMode,
                    onClick = { vm.toggleSilent() },
                    label = { Text("Silent 16 kHz") }
                )
                FilterChip(
                    selected = vm.loopEnabled,
                    onClick = { vm.toggleLoop() },
                    label = { Text("Loop") }
                )
                FilterChip(
                    selected = vm.torchEnabled,
                    onClick = { vm.toggleTorch() },
                    enabled = vm.torchAvailable,
                    label = { Text("Flashlight") }
                )
                FilterChip(
                    selected = vm.hapticEnabled,
                    onClick = { vm.toggleHaptic() },
                    enabled = vm.hapticAvailable,
                    label = { Text("Vibration") }
                )
            }
        }

        SectionCard(title = "Share") {
            ShareButtons(
                text = shareText,
                enabled = hasMorse,
                showMessage = showMessage
            )
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = "Crafted by Suvojeet Sengupta",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SpeedPreset(label: String, wpm: Int, current: Int, onPick: (Int) -> Unit) {
    FilterChip(
        selected = current == wpm,
        onClick = { onPick(wpm) },
        label = { Text(label) }
    )
}

@Composable
private fun PlayButton(
    isPlaying: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val label = if (isPlaying) "Stop" else "Play"
    val icon: ImageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = if (isPlaying) ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
        ) else ButtonDefaults.buttonColors()
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}
