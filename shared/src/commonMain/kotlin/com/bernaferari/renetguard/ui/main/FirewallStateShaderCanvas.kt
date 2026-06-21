package com.bernaferari.renetguard.ui.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import com.bernaferari.renetguard.ui.theme.LocalMotion
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
internal fun FirewallStateShaderCanvas(
    enabledProgress: Float,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotion.current
    val phase by rememberInfiniteTransition(label = "firewallShaderCanvas").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = motion.durationSlow * 24, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "firewallShaderCanvasPhase",
    )
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val blend = enabledProgress.coerceIn(0f, 1f)
    val signalDisabled =
        Color(
            red = surfaceVariant.red * (1f - 0.36f * (1f - if (isDark) 0.5f else 0.36f)) + outline.red * 0.36f,
            green = surfaceVariant.green * 0.64f + outline.green * 0.36f,
            blue = surfaceVariant.blue * 0.64f + outline.blue * 0.36f,
        )
    val signalEnabled =
        Color(
            red = secondary.red * 0.48f + tertiary.red * 0.52f,
            green = secondary.green * 0.48f + tertiary.green * 0.52f,
            blue = secondary.blue * 0.48f + tertiary.blue * 0.52f,
        )
    val signal =
        Color(
            red = signalDisabled.red + (signalEnabled.red - signalDisabled.red) * blend,
            green = signalDisabled.green + (signalEnabled.green - signalDisabled.green) * blend,
            blue = signalDisabled.blue + (signalEnabled.blue - signalDisabled.blue) * blend,
        )
    val lanePaths = remember { List(6) { Path() } }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val theta = phase * (2f * PI).toFloat()
        val tau = (2f * PI).toFloat()
        val sampleCount = width.toInt().coerceIn(48, 220)
        val stepX = width / sampleCount

        repeat(6) { laneIndex ->
            val laneT = laneIndex / 5f
            val centerY = (0.20f + laneT * 0.60f) * height
            val laneOff = laneIndex * 0.85f
            val freqDis = 1f + (laneIndex % 3).toFloat()
            val freqEn = 2f + (laneIndex % 2).toFloat()
            val path = lanePaths[laneIndex]
            path.rewind()

            for (sample in 0..sampleCount) {
                val x = sample * stepX
                val uvX = x / width
                val yDis =
                    centerY +
                        (0.005f + laneT * 0.003f) * height *
                        sin(uvX * tau * freqDis + theta * 0.55f + laneOff) +
                        0.002f * height * sin(theta * 0.18f + laneOff * 1.7f)
                val yEn =
                    centerY +
                        (0.008f + laneT * 0.005f) * height *
                        sin(uvX * tau * (freqEn + 1.2f) + theta * 1.85f + laneOff) +
                        0.003f * height * sin(uvX * tau * 3f - theta * 1.25f + laneOff * 0.6f)
                val y = yDis + (yEn - yDis) * blend
                if (sample == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            val strokeAlpha = (0.03f + 0.062f * blend) * if (isDark) 0.92f else 0.72f
            drawPath(
                path = path,
                color = signal.copy(alpha = strokeAlpha.coerceIn(0.02f, 0.2f)),
                style = Stroke(width = 1.25f, cap = StrokeCap.Round),
            )
        }

    }
}