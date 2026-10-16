package com.bernaferrari.quietguard.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

actual fun supportsDynamicTheme(): Boolean = false

@Composable
actual fun platformDynamicColorScheme(isDark: Boolean): ColorScheme? = null
