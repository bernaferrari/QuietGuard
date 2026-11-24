package com.bernaferrari.quietguard

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.bernaferrari.quietguard.data.preferences
import com.bernaferrari.quietguard.ui.Home
import com.bernaferrari.quietguard.ui.main.MainViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class ActivityMain : ComponentActivity() {
    private val pendingRoute = mutableStateOf<String?>(null)
    private var requestEnableFirewall by mutableStateOf(false)
    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pendingRoute.value = intent.getStringExtra(EXTRA_ROUTE)
        requestEnableFirewall = savedInstanceState == null &&
            intent.getBooleanExtra(EXTRA_ENABLE_FIREWALL, false)

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

            var showVpnDisclosure by rememberSaveable { mutableStateOf(false) }
            val requestVpnConnection: () -> Unit = {
                val vpnIntent = VpnService.prepare(this@ActivityMain)
                if (vpnIntent == null) {
                    viewModel.setEnabled(true)
                    ServiceSinkhole.start("UI", this@ActivityMain)
                } else {
                    vpnLauncher.launch(vpnIntent)
                }
            }

            val handleToggleEnabled: (Boolean) -> Unit = { enable ->
                when {
                    !enable -> {
                        viewModel.setEnabled(false)
                        ServiceSinkhole.stop("UI", this@ActivityMain, false)
                    }
                    preferences().getBoolean(VPN_DISCLOSURE_ACCEPTED, false) -> requestVpnConnection()
                    else -> showVpnDisclosure = true
                }
            }

            LaunchedEffect(requestEnableFirewall) {
                if (requestEnableFirewall) {
                    requestEnableFirewall = false
                    handleToggleEnabled(true)
                }
            }

            AppContent(
                onToggleEnabled = handleToggleEnabled,
                startRoute = pendingRoute.value ?: Home.route,
                pendingRoute = pendingRoute.value,
                onRouteNavigated = { pendingRoute.value = null },
                viewModel = viewModel,
            )

            if (showVpnDisclosure) {
                VpnDisclosureSheet(
                    onDismiss = { showVpnDisclosure = false },
                    onAccept = {
                        preferences().putBoolean(VPN_DISCLOSURE_ACCEPTED, true)
                        showVpnDisclosure = false
                        requestVpnConnection()
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute.value = intent.getStringExtra(EXTRA_ROUTE)
        requestEnableFirewall = intent.getBooleanExtra(EXTRA_ENABLE_FIREWALL, false)
    }

    companion object {
        const val VPN_DISCLOSURE_ACCEPTED = "vpn_disclosure_accepted"
        const val EXTRA_ROUTE = "Route"
        const val ACTION_RULES_CHANGED = "com.bernaferrari.quietguard.ACTION_RULES_CHANGED"
        const val ACTION_QUEUE_CHANGED = "com.bernaferrari.quietguard.ACTION_QUEUE_CHANGED"
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
