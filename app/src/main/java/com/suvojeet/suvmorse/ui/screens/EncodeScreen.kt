package com.suvojeet.suvmorse.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suvojeet.suvmorse.ui.EncodeViewModel
import com.suvojeet.suvmorse.ui.components.LabeledSlider
import com.suvojeet.suvmorse.ui.components.MorseGlyphs
import com.suvojeet.suvmorse.ui.components.SectionCard

@Composable
fun EncodeScreen(
    modifier: Modifier = Modifier,
    vm: EncodeViewModel = viewModel()
) {
    val clipboard = LocalClipboardManager.current
    val nearLimit = vm.charCount >= vm.maxChars * 0.9

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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text(
                            "${vm.charCount} / ${vm.maxChars}",
                            color = if (nearLimit) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (nearLimit) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            )
        }

        SectionCard(
            title = "Morse",
            trailing = {
                IconButton(
                    onClick = {
                        if (vm.morse.isNotEmpty()) clipboard.setText(AnnotatedString(vm.morse))
                    },
                    enabled = vm.morse.isNotEmpty()
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy Morse")
                }
            }
        ) {
            if (vm.morse.isEmpty()) {
                Text(
                    "· — · ·   will appear here",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                MorseGlyphs(
                    morse = vm.morse,
                    activeSymbol = vm.currentSymbol,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        SectionCard(title = "Playback") {
            LabeledSlider(
                label = "Speed",
                valueText = "${vm.wpm} WPM",
                value = vm.wpm.toFloat(),
                onValueChange = { vm.updateWpm(it.toInt()) },
                valueRange = 5f..40f,
                steps = 34
            )
            Spacer(Modifier.height(12.dp))
            LabeledSlider(
                label = "Pitch",
                valueText = "${vm.frequency.toInt()} Hz",
                value = vm.frequency.toFloat(),
                onValueChange = { vm.updateFrequency(it.toDouble()) },
                valueRange = 300f..1200f
            )
            Spacer(Modifier.height(20.dp))
            PlayButton(
                isPlaying = vm.isPlaying,
                enabled = vm.morse.isNotEmpty(),
                onClick = vm::togglePlay
            )
        }
    }
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
