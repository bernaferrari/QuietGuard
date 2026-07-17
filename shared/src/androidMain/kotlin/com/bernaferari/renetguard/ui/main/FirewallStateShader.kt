package com.bernaferari.renetguard.ui.main

import android.graphics.RuntimeShader
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import com.bernaferari.renetguard.ui.theme.LocalMotion
import android.graphics.Paint as AndroidPaint

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
internal fun FirewallStateShader(
    enabledProgress: Float,
    modifier: Modifier = Modifier,
) {
    val shader = remember {
        runCatching { RuntimeShader(FIREWALL_AGSL) }
            .onFailure { error -> Log.w("FirewallShader", "AGSL disabled: ${error.message}") }
            .getOrNull()
    }
    if (shader == null) {
        FirewallStateShaderCanvas(
            enabledProgress = enabledProgress,
            modifier = modifier,
        )
        return
    }

    val motion = LocalMotion.current
    val phase by rememberInfiniteTransition(label = "firewallShader").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = motion.durationSlow * 40, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "firewallShaderPhase",
    )

    val paint = remember { AndroidPaint().apply { isAntiAlias = true } }
    val density = LocalDensity.current.density
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val error = MaterialTheme.colorScheme.error
    val outline = MaterialTheme.colorScheme.outlineVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Canvas(modifier = modifier) {
        shader.setFloatUniform("iResolution", size.width, size.height)
        shader.setFloatUniform("iTime", phase)
        shader.setFloatUniform("uEnabled", enabledProgress.coerceIn(0f, 1f))
        shader.setFloatUniform("uDark", if (isDarkTheme) 1f else 0f)
        shader.setFloatUniform("uPixelSize", 8f * density)
        shader.setFloatUniform(
            "uSecondary",
            secondary.red,
            secondary.green,
            secondary.blue,
            secondary.alpha
        )
        shader.setFloatUniform(
            "uTertiary",
            tertiary.red,
            tertiary.green,
            tertiary.blue,
            tertiary.alpha
        )
        shader.setFloatUniform("uError", error.red, error.green, error.blue, error.alpha)
        shader.setFloatUniform("uOutline", outline.red, outline.green, outline.blue, outline.alpha)
        shader.setFloatUniform(
            "uSurfaceVariant",
            surfaceVariant.red,
            surfaceVariant.green,
            surfaceVariant.blue,
            surfaceVariant.alpha,
        )

        paint.shader = shader
        drawContext.canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
    }
}

/**
 * Adapted from Paper Shaders' Dithering shader (swirl, simplex noise, and 4x4 Bayer thresholding).
 * Modified for a transparent, low-contrast Compose background and a mathematically closed loop.
 * Source: https://github.com/paper-design/shaders/tree/4d78ae940db2c73c22b6633fb05a3e4743f22551
 */
private const val FIREWALL_AGSL =
    """
uniform float2 iResolution;
uniform float iTime;
uniform float uEnabled;
uniform float uDark;
uniform float uPixelSize;
uniform half4 uSecondary;
uniform half4 uTertiary;
uniform half4 uError;
uniform half4 uOutline;
uniform half4 uSurfaceVariant;

half3 mix3(half3 a, half3 b, float t) {
    return a + (b - a) * half(t);
}

float3 permute(float3 x) {
    return mod(((x * 34.0) + 1.0) * x, 289.0);
}

float snoise(float2 v) {
    const float4 C = float4(
        0.211324865405187,
        0.366025403784439,
        -0.577350269189626,
        0.024390243902439
    );
    float2 i = floor(v + dot(v, C.yy));
    float2 x0 = v - i + dot(i, C.xx);
    float2 i1 = x0.x > x0.y ? float2(1.0, 0.0) : float2(0.0, 1.0);
    float4 x12 = x0.xyxy + C.xxzz;
    x12.xy -= i1;
    i = mod(i, 289.0);
    float3 p = permute(
        permute(i.y + float3(0.0, i1.y, 1.0)) +
            i.x + float3(0.0, i1.x, 1.0)
    );
    float3 m = max(
        0.5 - float3(dot(x0, x0), dot(x12.xy, x12.xy), dot(x12.zw, x12.zw)),
        0.0
    );
    m = m * m;
    m = m * m;
    float3 x = 2.0 * fract(p * C.www) - 1.0;
    float3 h = abs(x) - 0.5;
    float3 ox = floor(x + 0.5);
    float3 a0 = x - ox;
    m *= 1.79284291400159 - 0.85373472095314 * (a0 * a0 + h * h);
    float3 g;
    g.x = a0.x * x0.x + h.x * x0.y;
    g.yz = a0.yz * x12.xz + h.yz * x12.yw;
    return 130.0 * dot(m, g);
}

float bayer2(float x, float y) {
    return mod(2.0 * x + 3.0 * y, 4.0);
}

float bayer4(float2 cell) {
    float low = bayer2(mod(cell.x, 2.0), mod(cell.y, 2.0));
    float high = bayer2(mod(floor(cell.x / 2.0), 2.0), mod(floor(cell.y / 2.0), 2.0));
    return (4.0 * low + high) / 16.0;
}

half4 main(float2 fragCoord) {
    float tau = 6.2831853;
    float theta = iTime * tau;
    float en = clamp(uEnabled, 0.0, 1.0);
    float pixelSize = max(uPixelSize, 1.0);
    float2 cell = floor(fragCoord / pixelSize);
    float2 pixelCoord = (cell + 0.5) * pixelSize;
    float2 uv = pixelCoord / iResolution;
    float aspect = iResolution.x / iResolution.y;
    float2 p = float2((uv.x - 0.5) * aspect * 1.35, (uv.y - 0.53) * 1.35);
    float noise = snoise(p * 1.9 + float2(cos(theta), sin(theta)) * 0.58);
    float radius = length(p);
    float angle = 6.0 * atan(p.y, p.x) + theta + noise * 0.62;
    float inverseRadius = 1.0 / pow(max(radius, 0.08), 1.2);
    float swirl = fract(inverseRadius + angle / tau);
    float middle = smoothstep(0.0, 1.0, pow(radius, 1.2));
    float noiseShape = 0.5 + 0.5 * noise;
    float shape = swirl * middle * 0.76 + noiseShape * 0.24;
    float dithered = step(bayer4(cell), shape);
    half3 sigDisabled = mix3(
        mix3(uSurfaceVariant.rgb, uOutline.rgb, 0.35),
        uError.rgb,
        0.20
    );
    half3 sigEnabled = mix3(uSecondary.rgb, uTertiary.rgb, 0.52);
    half3 signal = mix3(sigDisabled, sigEnabled, en);
    float centerQuiet = 0.18 + 0.82 * smoothstep(0.16, 0.52, radius);
    float texture = 0.78 + noiseShape * 0.22;
    float alpha =
        dithered * mix(0.70, 1.0, uDark) *
        (0.030 + 0.032 * en) * centerQuiet * texture;
    return half4(signal * half(alpha), half(alpha));
}
"""
