package com.suvojeet.suvmorse.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmorse.morse.MorseCode
import com.suvojeet.suvmorse.ui.components.SectionCard
import com.suvojeet.suvmorse.ui.components.ShareButtons

/** Manual Morse → text decoder. Type or paste dots/dashes; spaces split letters, "/" splits words. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DecodeScreen(
    modifier: Modifier = Modifier,
    showMessage: (String) -> Unit = {}
) {
    var input by rememberSaveable { mutableStateOf("") }
    val decoded = remember(input) { MorseCode.decode(input) }
    val clipboard = LocalClipboardManager.current

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionCard(title = "Morse input") {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                placeholder = { Text("Paste Morse, e.g. ... --- ...") }
            )
            FlowRow(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { input += "." }) { Text("· dot") }
                OutlinedButton(onClick = { input += "-" }) { Text("− dash") }
                OutlinedButton(onClick = { input += " " }) { Text("letter ⎵") }
                OutlinedButton(onClick = { input += " / " }) { Text("word /") }
                OutlinedButton(
                    onClick = { if (input.isNotEmpty()) input = input.dropLast(1) }
                ) {
                    Text("⌫")
                }
                OutlinedButton(onClick = { input = "" }) { Text("Clear") }
            }
        }

        SectionCard(
            title = "Text",
            trailing = {
                IconButton(
                    onClick = {
                        if (decoded.isNotBlank()) {
                            clipboard.setText(AnnotatedString(decoded))
                            showMessage("Text copied to clipboard")
                        }
                    },
                    enabled = decoded.isNotBlank()
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy text")
                }
            }
        ) {
            Text(
                text = decoded.ifBlank { "Decoded text will appear here…" },
                style = MaterialTheme.typography.titleMedium,
                color = if (decoded.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            )
        }

        SectionCard(title = "Share") {
            ShareButtons(
                text = decoded,
                enabled = decoded.isNotBlank(),
                showMessage = showMessage
            )
        }
    }
}
