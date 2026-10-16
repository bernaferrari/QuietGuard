package com.bernaferrari.quietguard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec

/**
 * Semantic "allowed" colors. Material 3 has no success role, so derive one from a
 * green seed with the same tonal rules as the main scheme; it stays readable in
 * light and dark and next to `error` for "blocked".
 */
@Immutable
data class StatusColors(
    val allowed: Color,
    val allowedContainer: Color,
    val onAllowedContainer: Color,
)

private val AllowedSeed = Color(0xFF2E7D32)

internal fun statusColors(isDark: Boolean): StatusColors {
    val scheme = dynamicColorScheme(
        seedColor = AllowedSeed,
        isDark = isDark,
        style = PaletteStyle.TonalSpot,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
    )
    return StatusColors(
        allowed = scheme.primary,
        allowedContainer = scheme.primaryContainer,
        onAllowedContainer = scheme.onPrimaryContainer,
    )
}

internal val LocalStatusColors = staticCompositionLocalOf { statusColors(isDark = false) }

val MaterialTheme.statusColors: StatusColors
    @Composable
    @ReadOnlyComposable
    get() = LocalStatusColors.current
