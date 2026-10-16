package com.bernaferrari.quietguard.platform

import androidx.compose.runtime.Composable

@Composable
actual fun rememberHostsFileImporter(onComplete: (success: Boolean) -> Unit): ((append: Boolean) -> Unit)? = null
