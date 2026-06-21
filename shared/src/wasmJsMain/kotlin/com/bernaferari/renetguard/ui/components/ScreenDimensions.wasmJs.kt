package com.bernaferari.renetguard.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import kotlinx.browser.window

@Composable
actual fun rememberScreenDimensions(): ScreenDimensions {
    val density = LocalDensity.current
    return ScreenDimensions(
        widthDp =
            with(density) {
                window.innerWidth
                    .toFloat()
                    .toDp()
                    .value
                    .toInt()
                    .coerceAtLeast(320)
            },
        heightDp =
            with(density) {
                window.innerHeight
                    .toFloat()
                    .toDp()
                    .value
                    .toInt()
                    .coerceAtLeast(480)
            },
    )
}