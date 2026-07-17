package com.bernaferrari.quietguard.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.bernaferrari.quietguard.AppContent
import com.bernaferrari.quietguard.di.initKoin
import com.bernaferrari.quietguard.platform.installWasmPlatformBindings

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Koin provides Room (WebWorkerSQLiteDriver + OPFS) and the rest of the graph.
    initKoin()
    installWasmPlatformBindings()
    ComposeViewport {
        AppContent(
            onToggleEnabled = { /* Demo VPN handled by onDemoFirewallToggled */ },
        )
    }
}
