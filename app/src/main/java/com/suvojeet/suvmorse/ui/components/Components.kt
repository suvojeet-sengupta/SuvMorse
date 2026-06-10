package com.suvojeet.suvmorse.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.suvmorse.morse.MorseCode
import com.suvojeet.suvmorse.util.ShareUtils

/** A titled surface card used to group related controls. */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                trailing?.invoke()
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/** A slider with an inline label and value readout. */
@Composable
fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                valueText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

/**
 * Renders a Morse string as monospaced glyphs. When [activeSymbol] >= 0, the dot/dash with that
 * ordinal (counting only dots/dashes) is highlighted to follow audio playback.
 */
@Composable
fun MorseGlyphs(
    morse: String,
    activeSymbol: Int,
    modifier: Modifier = Modifier
) {
    val base = MaterialTheme.colorScheme.onSurface
    val highlight = MaterialTheme.colorScheme.primary
    val dim = MaterialTheme.colorScheme.onSurfaceVariant

    val text: AnnotatedString = remember(morse, activeSymbol, base, highlight, dim) {
        buildAnnotatedString {
            var ordinal = 0
            for (c in morse) {
                when (c) {
                    MorseCode.DOT, MorseCode.DASH -> {
                        val isActive = ordinal == activeSymbol
                        withStyle(
                            SpanStyle(
                                color = if (isActive) highlight else base,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                background = if (isActive) highlight.copy(alpha = 0.18f)
                                else Color.Transparent
                            )
                        ) { append(c) }
                        ordinal++
                    }
                    else -> withStyle(SpanStyle(color = dim)) { append(c) }
                }
            }
        }
    }

    Text(
        text = text,
        modifier = modifier,
        fontFamily = FontFamily.Monospace,
        fontSize = 22.sp,
        lineHeight = 34.sp,
        letterSpacing = 2.sp
    )
}

/** A horizontal audio level meter, 0..1. */
@Composable
fun LevelMeter(
    level: Float,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val target = (level * 6f).coerceIn(0f, 1f) // scale faint tone energy into a visible range
    val animated by animateFloatAsState(targetValue = target, label = "level")
    val barColor by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outline,
        label = "barColor"
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(barColor)
        )
    }
}

/** A scrolling bar visualiser of the recent microphone level history. */
@Composable
fun WaveformView(
    levels: FloatArray,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val barColor = if (active) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val baseline = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(baseline)
    ) {
        Canvas(Modifier.fillMaxWidth().height(72.dp)) {
            if (levels.isEmpty()) return@Canvas
            val midY = size.height / 2f
            val count = levels.size
            val slot = size.width / count
            val barW = (slot * 0.6f).coerceAtLeast(1.5f)
            // Scale faint tone energy up into a readable range.
            levels.forEachIndexed { i, raw ->
                val mag = (raw * 5f).coerceIn(0f, 1f)
                val half = (mag * (size.height / 2f - 4f)).coerceAtLeast(1f)
                val x = i * slot + slot / 2f
                drawLine(
                    color = barColor,
                    start = Offset(x, midY - half),
                    end = Offset(x, midY + half),
                    strokeWidth = barW,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/** A label with a trailing switch, used for on/off feature toggles. */
@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supporting: String? = null
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (supporting != null) {
                Text(
                    supporting,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

/** A small "key down" beacon that glows while a tone is sounding. */
@Composable
fun TransmitBeacon(active: Boolean, modifier: Modifier = Modifier) {
    val glow by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(120),
        label = "beacon"
    )
    val core = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier.size(28.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(core.copy(alpha = 0.28f * glow))
        )
        Box(
            Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(lerp(idle, core, glow))
        )
    }
}

/** A WhatsApp fast-share button plus a generic system-share button. */
@Composable
fun ShareButtons(
    text: String,
    enabled: Boolean,
    showMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val whatsappGreen = Color(0xFF25D366)
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = {
                if (!ShareUtils.shareToWhatsApp(context, text)) {
                    showMessage("WhatsApp isn't installed")
                }
            },
            enabled = enabled,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = whatsappGreen,
                contentColor = Color.White
            )
        ) {
            Text("WhatsApp", fontWeight = FontWeight.SemiBold)
        }
        OutlinedButton(
            onClick = { ShareUtils.share(context, text) },
            enabled = enabled,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.Share, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Share")
        }
    }
}
