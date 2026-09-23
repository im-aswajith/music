package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MediumGray
import com.example.ui.theme.ObsidianBlack
import java.util.Locale
import kotlin.math.sin

/**
 * High-fidelity Interactive Audio Waveform Scrubber
 * Renders an organic audio amplitude wave matching design-example.png
 * Supports drag & tap scrubbing, animated active waveform pulses, and live timecodes.
 */
@Composable
fun AudioWaveformScrubber(
    currentPositionMs: Long,
    totalDurationMs: Long,
    isPlaying: Boolean,
    isLive: Boolean,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalMs = totalDurationMs.coerceAtLeast(1000L)
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val currentFraction = if (isDragging) {
        dragFraction
    } else {
        (currentPositionMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    }

    val displayCurrentMs = if (isDragging) {
        (dragFraction * totalMs).toLong()
    } else {
        currentPositionMs
    }

    // Subtle breathing pulse for active playing bars
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_pulse")
    val pulseFactor by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Precomputed natural waveform amplitudes
    val baseAmplitudes = remember {
        val count = 46
        List(count) { i ->
            val normalized = i.toFloat() / count.toFloat()
            // Combination of sine frequencies for organic music shape
            val wave = 0.25f +
                    0.35f * sin(normalized * Math.PI.toFloat()) +
                    0.20f * sin(normalized * 3.5f * Math.PI.toFloat() + 0.8f) +
                    0.15f * sin(normalized * 7f * Math.PI.toFloat() + 1.2f)
            wave.coerceIn(0.12f, 1.0f)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("audio_waveform_scrubber")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .pointerInput(totalMs) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeekTo((fraction * totalMs).toLong())
                    }
                }
                .pointerInput(totalMs) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            isDragging = false
                            onSeekTo((dragFraction * totalMs).toLong())
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barCount = baseAmplitudes.size
            val barSpacing = 3.5.dp.toPx()
            val totalSpacing = barSpacing * (barCount - 1)
            val barWidth = ((canvasWidth - totalSpacing) / barCount).coerceAtLeast(2.5.dp.toPx())
            val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

            baseAmplitudes.forEachIndexed { index, rawAmp ->
                val barFraction = index.toFloat() / (barCount - 1).toFloat()
                val isPlayed = barFraction <= currentFraction
                val isNearPlayhead = isPlayed && (currentFraction - barFraction) < 0.1f

                // Dynamic height with subtle live animation for played bars
                val effectiveAmp = if (isPlaying && isNearPlayhead) {
                    (rawAmp * pulseFactor).coerceIn(0.15f, 1.0f)
                } else {
                    rawAmp
                }

                val barHeight = (canvasHeight * effectiveAmp).coerceAtLeast(4.dp.toPx())
                val x = index * (barWidth + barSpacing)
                val y = (canvasHeight - barHeight) / 2f

                val barColor = if (isPlayed) {
                    ObsidianBlack
                } else {
                    Color(0xFFD1D5DB) // Soft light gray unplayed
                }

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = cornerRadius
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Timecodes Row: e.g. 1:04 on left, 2:52 on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTimecode(displayCurrentMs),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MediumGray
            )
            Text(
                text = if (isLive) "LIVE" else formatTimecode(totalMs),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isLive) Color(0xFFFF2D55) else MediumGray
            )
        }
    }
}

private fun formatTimecode(millis: Long): String {
    val totalSecs = (millis / 1000).coerceAtLeast(0)
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format(Locale.getDefault(), "%d:%02d", mins, secs)
}
