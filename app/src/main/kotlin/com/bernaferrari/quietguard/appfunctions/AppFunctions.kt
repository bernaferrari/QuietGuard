package com.bernaferrari.quietguard.appfunctions

import android.app.PendingIntent
import android.net.VpnService
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.AppFunctionStringValueConstraint
import com.bernaferrari.quietguard.ActivityMain
import com.bernaferrari.quietguard.AndroidFirewallDatabase
import com.bernaferrari.quietguard.ServiceSinkhole
import com.bernaferrari.quietguard.Util
import com.bernaferrari.quietguard.Widgets
import com.bernaferrari.quietguard.data.preferences
import com.bernaferrari.quietguard.domain.AndroidRulesRepository
import com.bernaferrari.quietguard.domain.FirewallRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Exposes NetGuard firewall controls to the Android intelligence system. */
@RequiresApi(36)
@AppFunctionServiceEntryPoint(serviceName = "QuietGuardAppFunctionService", appFunctionXmlFileName = "quietguard_functions")
abstract class AppFunctions : AppFunctionService(), KoinComponent {
    private val context get() = applicationContext
    private val rulesRepository by inject<AndroidRulesRepository>()

    /**
     * Returns the current global firewall and lockdown state.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getFirewallStatus(): FirewallStatus =
        withContext(Dispatchers.IO) {
            val rules = rulesRepository.loadAllRules()
            val blockedApps = rules.count { rule ->
                rule.installedPackage && rule.internet && (rule.wifi_blocked || rule.other_blocked)
            }
            FirewallStatus(
                enabled = context.preferences().getBoolean("enabled", false),
                lockdown = context.preferences().getBoolean("lockdown", false),
                blockedAppCount = blockedApps,
            )
        }

    /**
     * Enables or disables the NetGuard VPN firewall. When VPN permission has not been granted yet,
     * returns a [PendingIntent] that opens the app so the user can approve VPN access.
     *
     * @param enabled Whether the firewall should be active.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun setFirewallEnabled(
        enabled: Boolean,
    ): FirewallToggleResult =
        withContext(Dispatchers.IO) {
            if (enabled) {
                val vpnIntent = VpnService.prepare(context)
                if (
                    vpnIntent != null ||
                        !context
                            .preferences()
                            .getBoolean(ActivityMain.VPN_DISCLOSURE_ACCEPTED, false)
                ) {
                    val confirmation =
                        PendingIntent.getActivity(
                            context,
                            0,
                            ActivityMain.createEnableFirewallIntent(context),
                            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
                        )
                    return@withContext FirewallToggleResult(
                        enabled = context.preferences().getBoolean("enabled", false),
                        message = "VPN permission is required before the firewall can start.",
                        confirmationIntent = confirmation,
                    )
                }
                context.preferences().setEnabled(true)
                ServiceSinkhole.start("appfunction", context)
            } else {
                context.preferences().setEnabled(false)
                ServiceSinkhole.stop("appfunction", context, false)
            }
            Widgets.updateFirewall(context)
            FirewallToggleResult(
                enabled = enabled,
                message =
                    if (enabled) {
                        "Firewall enabled."
                    } else {
                        "Firewall disabled."
                    },
            )
        }

    /**
     * Enables or disables global lockdown mode. Lockdown blocks all apps except those explicitly
     * allowed during lockdown.
     *
     * @param enabled Whether lockdown mode should be active.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun setLockdownEnabled(
        enabled: Boolean,
    ): LockdownResult =
        withContext(Dispatchers.IO) {
            context.preferences().putBoolean("lockdown", enabled)
            ServiceSinkhole.reload("appfunction lockdown", context, false)
            Widgets.updateFirewall(context)
            LockdownResult(
                lockdown = enabled,
                message =
                    if (enabled) {
                        "Lockdown enabled."
                    } else {
                        "Lockdown disabled."
                    },
            )
        }

    /**
     * Searches installed apps that NetGuard can manage. Use the returned package name with
     * [getAppFirewallStatus] or [setAppInternetBlocked].
     *
     * @param query App display name or package name fragment. Null or blank returns the first
     *   manageable apps.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun searchApps(
        query: String?,
    ): List<AppSearchResult> =
        withContext(Dispatchers.IO) {
            val normalized = query?.trim()?.lowercase().orEmpty()
            val matches =
                rulesRepository
                    .loadAllRules()
                    .asSequence()
                    .filter { it.internet && it.installedPackage }
                    .filter { rule ->
                        if (normalized.isBlank()) {
                            true
                        } else {
                            val name = rule.name?.lowercase().orEmpty()
                            val packageName = rule.packageName?.lowercase().orEmpty()
                            name.contains(normalized) || packageName.contains(normalized)
                        }
                    }
                    .sortedBy { (it.name ?: it.packageName.orEmpty()).lowercase() }
                    .take(5)
                    .map { rule ->
                        AppSearchResult(
                            packageName = rule.packageName.orEmpty(),
                            displayName = rule.name ?: rule.packageName.orEmpty(),
                            wifiBlocked = rule.wifi_blocked,
                            mobileBlocked = rule.other_blocked,
                        )
                    }
                    .toList()

            if (matches.isEmpty()) {
                throw AppFunctionElementNotFoundException(
                    "No app found for query '${query.orEmpty()}'. Ask the user to clarify the app name."
                )
            }
            matches
        }

    /**
     * Returns the current Wi-Fi and mobile blocking state for a single app. Call [searchApps] first
     * when the package name is unknown.
     *
     * @param packageName The Android package name of the target app.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getAppFirewallStatus(
        packageName: String,
    ): AppFirewallStatus =
        withContext(Dispatchers.IO) {
            val rule = findRule(packageName)
            AppFirewallStatus(
                packageName = rule.packageName.orEmpty(),
                displayName = rule.name ?: rule.packageName.orEmpty(),
                wifiBlocked = rule.wifi_blocked,
                mobileBlocked = rule.other_blocked,
                lockdownAllowed = rule.lockdown,
            )
        }

    /**
     * Blocks or allows internet access for an app on Wi-Fi and/or mobile data. Call [searchApps]
     * first when the package name is unknown.
     *
     * @param packageName The Android package name of the target app.
     * @param network Which network type to change. Accepts "WIFI", "MOBILE", or "BOTH".
     * @param blocked Whether internet access should be blocked on the selected network(s).
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun setAppInternetBlocked(
        packageName: String,
        @AppFunctionStringValueConstraint(enumValues = ["WIFI", "MOBILE", "BOTH"]) network: String,
        blocked: Boolean,
    ): AppFirewallStatus =
        withContext(Dispatchers.IO) {
            val rules = rulesRepository.loadAllRules()
            val rule = findRule(packageName, rules)
            when (network) {
                "WIFI" -> rule.wifi_blocked = blocked
                "MOBILE" -> rule.other_blocked = blocked
                "BOTH" -> {
                    rule.wifi_blocked = blocked
                    rule.other_blocked = blocked
                }
                else ->
                    throw AppFunctionInvalidArgumentException(
                        "Network must be WIFI, MOBILE, or BOTH."
                    )
            }
            rulesRepository.persistNetworkRule(
                rule,
                rules,
                wifi = network != "MOBILE",
                mobile = network != "WIFI",
            )
            AppFirewallStatus(
                packageName = rule.packageName.orEmpty(),
                displayName = rule.name ?: rule.packageName.orEmpty(),
                wifiBlocked = rule.wifi_blocked,
                mobileBlocked = rule.other_blocked,
                lockdownAllowed = rule.lockdown,
            )
        }

    /**
     * Returns the most recent blocked connection attempts recorded by the firewall.
     *
     * @param limit Maximum number of entries to return. Defaults to 5.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getRecentBlockedConnections(
        limit: Int = 5,
    ): List<BlockedConnection> =
        withContext(Dispatchers.IO) {
            if (limit <= 0 || limit > 20) {
                throw AppFunctionInvalidArgumentException("Limit must be between 1 and 20.")
            }
            AndroidFirewallDatabase.getInstance(context)
                .dao
                .getLogs(udp = 1, tcp = 1, other = 1, allowed = 0, blocked = 1, limit = limit)
                .map { entry ->
                    BlockedConnection(
                        appName =
                            Util.getApplicationNames(entry.uid ?: -1, context).joinToString(", "),
                        destination =
                            entry.daddr?.let { address ->
                                val port = entry.dport
                                if (port != null && port > 0) "$address:$port" else address
                            },
                        hostName = entry.dname,
                        protocol = entry.protocol?.let(::protocolName) ?: "unknown",
                    )
                }
        }

    private fun findRule(
        packageName: String,
        rules: List<FirewallRule> = rulesRepository.loadAllRules(),
    ): FirewallRule {
        val normalized = packageName.trim()
        if (normalized.isBlank()) {
            throw AppFunctionInvalidArgumentException("Package name cannot be blank.")
        }
        return rules.firstOrNull {
            it.installedPackage && it.packageName.equals(normalized, ignoreCase = true)
        }
            ?: throw AppFunctionElementNotFoundException(
                "No manageable app found for package '$packageName'."
            )
    }

    private fun protocolName(protocol: Int): String =
        when (protocol) {
            6 -> "TCP"
            17 -> "UDP"
            else -> protocol.toString()
        }

    /** Current global firewall state. */
    @AppFunctionSerializable(isDescribedByKDoc = true)
    data class FirewallStatus(
        /** Whether the NetGuard VPN firewall is active. */
        val enabled: Boolean,
        /** Whether global lockdown mode is active. */
        val lockdown: Boolean,
        /** Number of apps with at least one blocked network path. */
        val blockedAppCount: Int,
    )

    /** Result of enabling or disabling the firewall. */
    @AppFunctionSerializable(isDescribedByKDoc = true)
    data class FirewallToggleResult(
        /** Whether the firewall is active after this operation. */
        val enabled: Boolean,
        /** Human-readable status for the caller. */
        val message: String,
        /** Intent to launch when user confirmation is required, such as VPN approval. */
        val confirmationIntent: PendingIntent? = null,
    )

    /** Result of enabling or disabling lockdown mode. */
    @AppFunctionSerializable(isDescribedByKDoc = true)
    data class LockdownResult(
        /** Whether lockdown mode is active after this operation. */
        val lockdown: Boolean,
        /** Human-readable status for the caller. */
        val message: String,
    )

    /** App entry returned by [searchApps]. */
    @AppFunctionSerializable(isDescribedByKDoc = true)
    data class AppSearchResult(
        /** Android package name used by other app functions. */
        val packageName: String,
        /** Human-readable app label. */
        val displayName: String,
        /** Whether Wi-Fi access is currently blocked. */
        val wifiBlocked: Boolean,
        /** Whether mobile data access is currently blocked. */
        val mobileBlocked: Boolean,
    )

    /** Per-app firewall state. */
    @AppFunctionSerializable(isDescribedByKDoc = true)
    data class AppFirewallStatus(
        /** Android package name of the app. */
        val packageName: String,
        /** Human-readable app label. */
        val displayName: String,
        /** Whether Wi-Fi access is blocked. */
        val wifiBlocked: Boolean,
        /** Whether mobile data access is blocked. */
        val mobileBlocked: Boolean,
        /** Whether the app is explicitly allowed during lockdown. */
        val lockdownAllowed: Boolean,
    )

    /** A blocked connection from the traffic log. */
    @AppFunctionSerializable(isDescribedByKDoc = true)
    data class BlockedConnection(
        /** Name of the app that attempted the connection. */
        val appName: String,
        /** Destination IP address and optional port. */
        val destination: String?,
        /** Resolved hostname when available. */
        val hostName: String?,
        /** Transport protocol, such as TCP or UDP. */
        val protocol: String,
    )
}
