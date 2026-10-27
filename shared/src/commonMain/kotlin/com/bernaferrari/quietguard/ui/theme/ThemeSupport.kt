package com.bernaferrari.quietguard.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** Whether the platform supports the wallpaper-based dynamic theme option. */
expect fun supportsDynamicTheme(): Boolean

/** Wallpaper-based color scheme, or null when the platform has none. */
@Composable
expect fun platformDynamicColorScheme(isDark: Boolean): ColorScheme?

/** Keeps system bar icons readable: dark icons on the light theme, light icons on the dark one. */
@Composable
expect fun SystemBarsAppearance(darkTheme: Boolean)
