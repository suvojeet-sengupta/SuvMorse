package com.suvojeet.suvmorse.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.suvmorse.morse.MorseCode

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

    val text: AnnotatedString = buildAnnotatedString {
        var ordinal = 0
        for (c in morse) {
            when (c) {
                MorseCode.DOT, MorseCode.DASH -> {
                    val isActive = ordinal == activeSymbol
                    withStyle(
                        SpanStyle(
                            color = if (isActive) highlight else base,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            background = if (isActive) highlight.copy(alpha = 0.18f) else Color.Transparent
                        )
                    ) { append(c) }
                    ordinal++
                }
                else -> withStyle(SpanStyle(color = dim)) { append(c) }
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
