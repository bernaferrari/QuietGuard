package com.bernaferrari.quietguard.platform

import com.bernaferrari.quietguard.data.db.ForwardEntity
import com.bernaferrari.quietguard.data.db.WasmNetGuardDatabase
import com.bernaferrari.quietguard.data.db.toAccessEntry
import com.bernaferrari.quietguard.data.db.toDnsEntry
import com.bernaferrari.quietguard.data.db.toForwardingEntry
import com.bernaferrari.quietguard.data.db.toLogEntry
import com.bernaferrari.quietguard.demo.wasmDemoApps
import com.bernaferrari.quietguard.domain.FirewallRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private suspend fun dao() =
    WasmNetGuardDatabase.dao.also {
        WasmNetGuardDatabase.ensureDemoDataSeeded()
    }

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
    return rows.map { it.toLogEntry() }
}

actual fun observeLogs(
    udp: Boolean,
    tcp: Boolean,
    other: Boolean,
    allowed: Boolean,
    blocked: Boolean,
    limit: Int,
): Flow<List<LogEntry>> = flow {
    dao()
        .observeLogs(udp.flag, tcp.flag, other.flag, allowed.flag, blocked.flag, limit.sqlLimit)
        .collect { rows -> emit(rows.map { it.toLogEntry() }) }
}

actual suspend fun loadDnsEntries(): List<DnsEntry> = dao().getAllDns().map { it.toDnsEntry() }

actual suspend fun cleanupDns() {
    dao().cleanupDns(currentTimeMillis())
}

actual suspend fun clearDns() {
    dao().deleteAllDns()
}

actual suspend fun loadForwardingEntries(): List<ForwardingEntry> =
    dao().getAllForwards().map { it.toForwardingEntry() }

actual fun observeDnsEntries(): Flow<List<DnsEntry>> = flow {
    dao().observeAllDns().collect { rows -> emit(rows.map { it.toDnsEntry() }) }
}

actual fun observeForwardingEntries(): Flow<List<ForwardingEntry>> = flow {
    dao().observeAllForwards().collect { rows -> emit(rows.map { it.toForwardingEntry() }) }
}

actual fun observeAccessEntries(uid: Int): Flow<List<AccessEntry>> = flow {
    dao().observeAccessForUid(uid, 250).collect { rows -> emit(rows.map { it.toAccessEntry() }) }
}

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
}

actual suspend fun deleteForwardingEntry(entry: ForwardingEntry) {
    dao().deleteForward(entry.protocol, entry.dport)
}

actual fun loadAppDisplayInfo(uid: Int, fallbackLabel: String): AppDisplayInfo =
    wasmDemoApps[uid] ?: AppDisplayInfo(label = fallbackLabel, packageName = null)

actual suspend fun loadAllRulesForPicker(): List<FirewallRule> = wasmDemoApps.map { (uid, info) ->
    FirewallRule(
        uid = uid,
        packageName = info.packageName,
        name = info.label,
    )
}

actual fun registerUpdateCheckListener(
    onResult: (status: String?, version: String?) -> Unit
): () -> Unit = {}

actual fun exportDnsToFile(onComplete: (success: Boolean, error: String?) -> Unit) {
    onComplete(false, null)
}

actual suspend fun loadAccessEntries(uid: Int): List<AccessEntry> =
    dao().getAccessForUid(uid, 250).map { it.toAccessEntry() }

actual suspend fun clearAccess(uid: Int) {
    dao().deleteAccessForUid(uid)
}

actual suspend fun clearLogs() {
    dao().deleteLogs(-1)
}

private val Boolean.flag: Int
    get() = if (this) 1 else 0

private val Int.sqlLimit: Int
    get() = if (this > 0) this else -1
