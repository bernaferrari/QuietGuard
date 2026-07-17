package com.bernaferrari.quietguard.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun FirewallStateShaderBackground(
    enabledProgress: Float,
    modifier: Modifier,
) {
    FirewallStateShaderCanvas(
        enabledProgress = enabledProgress,
        modifier = modifier,
    )
}