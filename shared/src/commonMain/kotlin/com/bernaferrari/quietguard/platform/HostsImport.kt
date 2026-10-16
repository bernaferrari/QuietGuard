package com.bernaferrari.quietguard.platform

import androidx.compose.runtime.Composable

/**
 * Returns a launcher that lets the user pick a hosts file and either replaces the
 * current one or appends to it, or null when the platform can't import files.
 */
@Composable
expect fun rememberHostsFileImporter(onComplete: (success: Boolean) -> Unit): ((append: Boolean) -> Unit)?
