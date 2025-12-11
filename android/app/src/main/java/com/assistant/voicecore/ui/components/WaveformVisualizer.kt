package com.assistant.voicecore.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * Animated waveform visualizer for audio input
 */
@Composable
fun WaveformVisualizer(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 40,
    color: Color = Color(0xFF2196F3),
    backgroundColor: Color = Color(0x1A000000)
) {
    val density = LocalDensity.current
    var animationPhase by remember { mutableStateOf(0f) }
    
    LaunchedEffect(isActive) {
        if (isActive) {
            while (isActive) {
                animationPhase += 0.1f
                delay(50) // 20 FPS animation
            }
        }
    }
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        if (isActive) {
            drawWaveformBars(
                barCount = barCount,
                phase = animationPhase,
                color = color,
                backgroundColor = backgroundColor
            )
        } else {
            drawStaticWaveform(barCount, backgroundColor)
        }
    }
}

private fun DrawScope.drawWaveformBars(
    barCount: Int,
    phase: Float,
    color: Color,
    backgroundColor: Color
) {
    val barWidth = size.width / barCount
    val barSpacing = barWidth * 0.2f
    val maxBarHeight = size.height * 0.8f
    
    // Draw background bars
    for (i in 0 until barCount) {
        val x = i * barWidth + barSpacing / 2
        val barHeight = maxBarHeight * 0.3f // Base height
        
        drawRoundRect(
            color = backgroundColor,
            topLeft = androidx.compose.ui.geometry.Offset(x, (size.height - barHeight) / 2),
            size = androidx.compose.ui.geometry.Size(barWidth - barSpacing, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
        )
    }
    
    // Draw animated bars
    for (i in 0 until barCount) {
        val x = i * barWidth + barSpacing / 2
        val progress = i.toFloat() / barCount
        val amplitude = 0.3f + 0.7f * (0.5f + 0.5f * sin(phase + progress * 4f))
        val barHeight = maxBarHeight * amplitude
        
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(x, (size.height - barHeight) / 2),
            size = androidx.compose.ui.geometry.Size(barWidth - barSpacing, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
        )
    }
}

private fun DrawScope.drawStaticWaveform(
    barCount: Int,
    backgroundColor: Color
) {
    val barWidth = size.width / barCount
    val barSpacing = barWidth * 0.2f
    val maxBarHeight = size.height * 0.3f
    
    for (i in 0 until barCount) {
        val x = i * barWidth + barSpacing / 2
        val barHeight = maxBarHeight
        
        drawRoundRect(
            color = backgroundColor,
            topLeft = androidx.compose.ui.geometry.Offset(x, (size.height - barHeight) / 2),
            size = androidx.compose.ui.geometry.Size(barWidth - barSpacing, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
        )
    }
}