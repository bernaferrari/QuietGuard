package com.bernaferrari.quietguard.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.bernaferrari.quietguard.AndroidFirewallDatabase
import com.bernaferrari.quietguard.Rule
import com.bernaferrari.quietguard.data.db.ForwardEntity
import com.bernaferrari.quietguard.data.db.toAccessEntry
import com.bernaferrari.quietguard.data.db.toDnsEntry
import com.bernaferrari.quietguard.data.db.toForwardingEntry
import com.bernaferrari.quietguard.data.db.toLogEntry
import com.bernaferrari.quietguard.domain.FirewallRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext

private fun context(): Context = GlobalContext.get().get()

private fun localTimeFormatter() =
    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())

private fun dao() = AndroidFirewallDatabase.getInstance(context()).dao

actual suspend fun loadLogs(
    udp: Boolean,
    tcp: Boolean,
    other: Boolean,
    allowed: Boolean,
    blocked: Boolean,
    limit: Int,
): List<LogEntry> {
    val rows =
        dao().getLogs(udp.flag, tcp.flag, other.flag, allowed.flag, blocked.flag, limit.sqlLimit)
    val time = localTimeFormatter()
    return rows.map { it.toLogEntry(time::format) }
}

private val Int.sqlLimit: Int
    get() = if (this > 0) this else -1

private val Boolean.flag: Int
    get() = if (this) 1 else 0

actual suspend fun loadDnsEntries(): List<DnsEntry> = dao().getAllDns().map { it.toDnsEntry() }

actual suspend fun cleanupDns() {
    dao().cleanupDns(System.currentTimeMillis())
    NetGuardPlatform.firewall.reload("DNS cleanup", false)
}

actual suspend fun clearDns() {
    dao().deleteAllDns()
    NetGuardPlatform.firewall.reload("DNS clear", false)
}

actual suspend fun loadForwardingEntries(): List<ForwardingEntry> =
    dao().getAllForwards().map { it.toForwardingEntry() }

actual suspend fun addForwardingEntry(
    protocol: Int,
    dport: Int,
    raddr: String,
    rport: Int,
    ruid: Int,
) {
    dao()
        .insertForward(
            ForwardEntity(
                protocol = protocol,
                dport = dport,
                raddr = raddr,
                rport = rport,
                ruid = ruid,
            )
        )
    NetGuardPlatform.firewall.reload("forwarding", false)
}

actual suspend fun deleteForwardingEntry(entry: ForwardingEntry) {
    dao().deleteForward(entry.protocol, entry.dport)
    NetGuardPlatform.firewall.reload("forwarding", false)
}

actual fun loadAppDisplayInfo(uid: Int, fallbackLabel: String): AppDisplayInfo {
    val label =
        NetGuardPlatform.uiHelpers.getApplicationNames(uid).joinToString(", ").ifBlank {
            "UID $uid"
        }
    val packageName = NetGuardPlatform.uiHelpers.getPackageForUid(uid)
    return AppDisplayInfo(label = label.ifBlank { fallbackLabel }, packageName = packageName)
}

actual suspend fun loadAllRulesForPicker(): List<FirewallRule> =
    withContext(Dispatchers.IO) {
        Rule.getRules(true, context()).map { rule ->
            FirewallRule(uid = rule.uid, packageName = rule.packageName, name = rule.name)
        }
    }

actual fun registerUpdateCheckListener(
    onResult: (status: String?, version: String?) -> Unit
): () -> Unit {
    val ctx = context()
    val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action != NetGuardPlatform.firewall.updateCheckAction) return
                onResult(
                    intent.getStringExtra(NetGuardPlatform.firewall.extraUpdateCheckStatus),
                    intent.getStringExtra(NetGuardPlatform.firewall.extraUpdateCheckVersion),
                )
            }
        }
    ContextCompat.registerReceiver(
        ctx,
        receiver,
        IntentFilter(NetGuardPlatform.firewall.updateCheckAction),
        ContextCompat.RECEIVER_NOT_EXPORTED,
    )
    return {
        runCatching { ctx.unregisterReceiver(receiver) }
    }
}

actual fun exportDnsToFile(onComplete: (success: Boolean, error: String?) -> Unit) {
    onComplete(false, "Export not available")
}

actual suspend fun loadAccessEntries(uid: Int): List<AccessEntry> {
    val time = localTimeFormatter()
    return dao().getAccessForUid(uid, 10).map { it.toAccessEntry(time::format) }
}

actual suspend fun clearAccess(uid: Int) {
    dao().deleteAccessRules(uid)
}

actual suspend fun clearLogs() {
    dao().deleteLogs(-1)
}

actual fun observeLogs(
    udp: Boolean,
    tcp: Boolean,
    other: Boolean,
    allowed: Boolean,
    blocked: Boolean,
    limit: Int,
): Flow<List<LogEntry>> =
    dao()
        .observeLogs(udp.flag, tcp.flag, other.flag, allowed.flag, blocked.flag, limit.sqlLimit)
        .map { rows ->
            val time = localTimeFormatter()
            rows.map { it.toLogEntry(time::format) }
        }

actual fun observeDnsEntries(): Flow<List<DnsEntry>> =
    dao().observeAllDns().map { rows -> rows.map { it.toDnsEntry() } }

actual fun observeForwardingEntries(): Flow<List<ForwardingEntry>> =
    dao().observeAllForwards().map { rows -> rows.map { it.toForwardingEntry() } }

actual fun observeAccessEntries(uid: Int): Flow<List<AccessEntry>> =
    dao().observeAccessForUid(uid, 10).map { rows ->
        val time = localTimeFormatter()
        rows.map { it.toAccessEntry(time::format) }
    }
