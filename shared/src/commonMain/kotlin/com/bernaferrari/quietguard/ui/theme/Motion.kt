package com.bernaferrari.quietguard.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Design tokens for consistent, subtle animations throughout the app.
 * Follows Material Design 3 motion principles with a professional, minimal approach.
 */
@Immutable
data class Motion(
    val reducedMotion: Boolean = false,
    // Duration constants - subtle and professional
    /** 150ms - Quick interactions like button presses */
    val durationFast: Int = if (reducedMotion) 0 else 150,
    /** 250ms - Standard transitions like expand/collapse */
    val durationMedium: Int = if (reducedMotion) 0 else 250,
    /** 350ms - Deliberate animations like screen transitions */
    val durationSlow: Int = if (reducedMotion) 0 else 350,

    // Easing curves - Material Design 3 standard
    /** Standard easing for most transitions */
    val easingStandard: CubicBezierEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    /** Emphasized easing for important state changes */
    val easingEmphasized: CubicBezierEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    /** Decelerate for elements entering the screen */
    val easingDecelerate: CubicBezierEasing = CubicBezierEasing(0f, 0f, 0f, 1f),
    /** Accelerate for elements leaving the screen */
    val easingAccelerate: CubicBezierEasing = CubicBezierEasing(0.3f, 0f, 1f, 1f),
)

val LocalMotion = staticCompositionLocalOf { Motion() }

// MotionScheme-backed specs. Prefer these over hand-tuned tween()s: they follow the
// theme's expressive springs and snap when the user has reduced motion enabled.

/** Non-overshooting spec for color, alpha and progress values. */
@Composable
@ReadOnlyComposable
fun <T> effectsSpec(): FiniteAnimationSpec<T> =
    if (LocalMotion.current.reducedMotion) snap() else MaterialTheme.motionScheme.defaultEffectsSpec()

/** Quicker [effectsSpec] for small, immediate feedback (fades, tints). */
@Composable
@ReadOnlyComposable
fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> =
    if (LocalMotion.current.reducedMotion) snap() else MaterialTheme.motionScheme.fastEffectsSpec()

/** Spatial spec (may overshoot) for sizes, offsets and corner radii. */
@Composable
@ReadOnlyComposable
fun <T> spatialSpec(): FiniteAnimationSpec<T> =
    if (LocalMotion.current.reducedMotion) snap() else MaterialTheme.motionScheme.defaultSpatialSpec()

// Extension functions for common animation specs

/** Fade animation spec for appearing/disappearing content */
fun Motion.fadeSpec() = tween<Float>(
    durationMillis = durationMedium,
    easing = easingStandard
)

/** Expand/collapse animation spec */
fun Motion.expandSpec() = tween<Int>(
    durationMillis = durationMedium,
    easing = easingEmphasized
)

/** Color transition animation spec */
fun Motion.colorSpec() = tween<androidx.compose.ui.graphics.Color>(
    durationMillis = durationMedium,
    easing = easingStandard
)

/** Scale animation spec for press effects */
fun Motion.scaleSpec() = tween<Float>(
    durationMillis = durationFast,
    easing = easingStandard
)

/** Rotation animation spec for icons */
fun Motion.rotationSpec() = tween<Float>(
    durationMillis = durationMedium,
    easing = easingStandard
)
