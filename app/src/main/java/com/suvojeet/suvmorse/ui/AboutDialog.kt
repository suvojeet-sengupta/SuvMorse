package com.suvojeet.suvmorse.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** A full, hand-written "About" sheet: what the app is, what it's for, privacy, and credits. */
@Composable
fun AboutDialog(
    onDismiss: () -> Unit,
    themeMode: Int = 0,
    onThemeModeChange: (Int) -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                BrandStrip(onClose = onDismiss)

                Column(Modifier.padding(20.dp)) {
                    Text(
                        "SuvMorse turns your words into Morse code — and Morse back into words. " +
                            "Hear it, see it flash, feel it buzz, or decode a real Morse tone live " +
                            "through the microphone. Everything works offline.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Heading("What you can do")
                    Bullet("Encode", "Type a message, watch its Morse build up, then play it at any speed and pitch — on a loop if you like.")
                    Bullet("Flash & buzz", "Send the same message as flashlight blinks or vibration, not just sound.")
                    Bullet("Decode", "Paste dots and dashes and read the plain text instantly.")
                    Bullet("Receive", "Point the mic at a Morse signal and decode it in real time — the pitch is found automatically and the room noise is calibrated out.")
                    Bullet("Chart", "A full reference for every letter, number and symbol.")
                    Bullet("Share", "Send any message or decoded text straight to WhatsApp or anywhere else.")

                    Heading("Where it's useful")
                    Bullet("Learning", "Pick up Morse from scratch or practise for an amateur (ham) radio licence.")
                    Bullet("Emergencies", "Flash or sound an SOS with no signal and no internet.")
                    Bullet("Quiet signalling", "Send vibration patterns when you can't make noise.")
                    Bullet("Scouts & classrooms", "A hands-on way to teach and play with signalling.")
                    Bullet("Real decoding", "Read beeps from a buzzer, radio or another phone.")
                    Bullet("Just for fun", "Trade secret messages with friends.")

                    Heading("Appearance")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("System", "Light", "Dark").forEachIndexed { mode, label ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Heading("Your privacy")
                    Text(
                        "The microphone is used only while you're on the Receive screen and actively " +
                            "listening. Audio is analysed on the spot — nothing is recorded, saved, " +
                            "or sent anywhere.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Built by Suvojeet Sengupta",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Version 1.0  ·  International (ITU) Morse standard",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun BrandStrip(onClose: () -> Unit) {
    val gradient = Brush.horizontalGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
    )
    val onGradient = MaterialTheme.colorScheme.onPrimary
    Row(
        Modifier
            .fillMaxWidth()
            .background(gradient)
            .padding(start = 20.dp, top = 18.dp, end = 8.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "·−",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = onGradient
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "About SuvMorse",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = onGradient
            )
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = onGradient)
        }
    }
}

@Composable
private fun Heading(text: String) {
    Spacer(Modifier.height(20.dp))
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.secondary
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun Bullet(title: String, body: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
