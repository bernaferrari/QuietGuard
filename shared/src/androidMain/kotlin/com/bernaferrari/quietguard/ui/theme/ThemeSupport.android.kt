package com.bernaferrari.quietguard.ui.theme

import android.app.Activity
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.platform.LocalContext

actual fun supportsDynamicTheme(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
actual fun platformDynamicColorScheme(isDark: Boolean): ColorScheme? {
    if (!supportsDynamicTheme()) return null
    val context = LocalContext.current
    return if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}

@Composable
actual fun SystemBarsAppearance(darkTheme: Boolean) {
    val view = LocalView.current
    SideEffect {
        var context = view.context
        while (context is ContextWrapper && context !is Activity) context = context.baseContext
        val window = (context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}
