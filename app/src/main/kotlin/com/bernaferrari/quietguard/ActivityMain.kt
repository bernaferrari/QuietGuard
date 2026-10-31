package com.bernaferrari.quietguard

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import com.bernaferrari.quietguard.data.preferences
import com.bernaferrari.quietguard.ui.Home
import com.bernaferrari.quietguard.ui.main.MainViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class ActivityMain : ComponentActivity() {
    private val pendingRoute = mutableStateOf<String?>(null)
    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pendingRoute.value = intent.getStringExtra(EXTRA_ROUTE)
        val requestEnableFirewall = intent.getBooleanExtra(EXTRA_ENABLE_FIREWALL, false)

        setContent {
            val vpnLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        viewModel.setEnabled(true)
                        ServiceSinkhole.start("UI", this@ActivityMain)
                    } else {
                        viewModel.setEnabled(false)
                    }
                }

            val showVpnDisclosure = remember { mutableStateOf(false) }
            val handleToggleEnabled: (Boolean) -> Unit = { enable ->
                if (enable && !preferences().getBoolean(VPN_DISCLOSURE_ACCEPTED, false)) {
                    showVpnDisclosure.value = true
                } else if (enable) {
                    val vpnIntent = VpnService.prepare(this@ActivityMain)
                    if (vpnIntent == null) {
                        viewModel.setEnabled(true)
                        ServiceSinkhole.start("UI", this@ActivityMain)
                    } else {
                        vpnLauncher.launch(vpnIntent)
                    }
                } else {
                    viewModel.setEnabled(false)
                    ServiceSinkhole.stop("UI", this@ActivityMain, false)
                }
            }

            LaunchedEffect(Unit) {
                if (requestEnableFirewall) handleToggleEnabled(true)
            }

            if (showVpnDisclosure.value) {
                AlertDialog(
                    onDismissRequest = { showVpnDisclosure.value = false },
                    title = { Text(stringResource(R.string.vpn_disclosure_title)) },
                    text = { Text(stringResource(R.string.vpn_disclosure_body)) },
                    confirmButton = { TextButton(onClick = {
                        preferences().putBoolean(VPN_DISCLOSURE_ACCEPTED, true)
                        showVpnDisclosure.value = false
                        handleToggleEnabled(true)
                    }) { Text(stringResource(R.string.vpn_disclosure_accept)) } },
                    dismissButton = { TextButton(onClick = { showVpnDisclosure.value = false }) {
                        Text(stringResource(android.R.string.cancel))
                    } },
                )
            }

            AppContent(
                onToggleEnabled = handleToggleEnabled,
                startRoute = pendingRoute.value ?: Home.route,
                pendingRoute = pendingRoute.value,
                onRouteNavigated = { pendingRoute.value = null },
                viewModel = viewModel,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute.value = intent.getStringExtra(EXTRA_ROUTE)
    }

    companion object {
        const val VPN_DISCLOSURE_ACCEPTED = "vpn_disclosure_accepted"
        const val EXTRA_ROUTE = "Route"
        const val ACTION_RULES_CHANGED = "com.bernaferrari.quietguard.ACTION_RULES_CHANGED"
        const val ACTION_QUEUE_CHANGED = "com.bernaferrari.quietguard.ACTION_QUEUE_CHANGED"
        const val EXTRA_REFRESH = "Refresh"
        const val EXTRA_SEARCH = "Search"
        const val EXTRA_RELATED = "Related"
        const val EXTRA_APPROVE = "Approve"
        const val EXTRA_CONNECTED = "Connected"
        const val EXTRA_METERED = "Metered"
        const val EXTRA_SIZE = "Size"

        const val EXTRA_ENABLE_FIREWALL = "EnableFirewall"

        fun createRouteIntent(context: android.content.Context, route: String): Intent {
            return Intent(context, ActivityMain::class.java).apply {
                putExtra(EXTRA_ROUTE, route)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        }

        fun createEnableFirewallIntent(context: android.content.Context): Intent {
            return Intent(context, ActivityMain::class.java).apply {
                putExtra(EXTRA_ENABLE_FIREWALL, true)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        }
    }
}
