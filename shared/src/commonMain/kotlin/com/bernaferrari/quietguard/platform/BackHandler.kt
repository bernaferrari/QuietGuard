package com.bernaferrari.quietguard.platform

import androidx.compose.runtime.Composable

@Composable
expect fun HandleBackPress(enabled: Boolean, onBack: () -> Unit)