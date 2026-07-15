package com.bernaferari.renetguard.platform

import com.bernaferari.renetguard.data.db.ForwardEntity
import com.bernaferari.renetguard.data.db.WasmNetGuardDatabase
import com.bernaferari.renetguard.data.db.observeRoomInvalidations
import com.bernaferari.renetguard.data.db.toAccessEntry
import com.bernaferari.renetguard.data.db.toDnsEntry
import com.bernaferari.renetguard.data.db.toForwardingEntry
import com.bernaferari.renetguard.data.db.toLogEntry
import com.bernaferari.renetguard.demo.wasmDemoApps
import com.bernaferari.renetguard.domain.FirewallRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

private suspend fun dao() = WasmNetGuardDatabase.dao.also {
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
    val d = dao()
    val rows =
        if (limit > 0) {
            d.getLogLimited(
                udp = boolInt(udp),
                tcp = boolInt(tcp),
                other = boolInt(other),
                allowed = boolInt(allowed),
                blocked = boolInt(blocked),
                limit = limit,
            )
        } else {
            d.getLogUnlimited(
                udp = boolInt(udp),
                tcp = boolInt(tcp),
                other = boolInt(other),
                allowed = boolInt(allowed),
                blocked = boolInt(blocked),
            )
        }
    return rows.map { it.toLogEntry() }
}

actual fun observeLogs(
    udp: Boolean,
    tcp: Boolean,
    other: Boolean,
    allowed: Boolean,
    blocked: Boolean,
    limit: Int,
): Flow<List<LogEntry>> =
    flow {
        WasmNetGuardDatabase.ensureDemoDataSeeded()
        val d = WasmNetGuardDatabase.dao
        d.observeLogTable()
            .map {
                if (limit > 0) {
                    d.getLogLimited(
                        udp = boolInt(udp),
                        tcp = boolInt(tcp),
                        other = boolInt(other),
                        allowed = boolInt(allowed),
                        blocked = boolInt(blocked),
                        limit = limit,
                    )
                } else {
                    d.getLogUnlimited(
                        udp = boolInt(udp),
                        tcp = boolInt(tcp),
                        other = boolInt(other),
                        allowed = boolInt(allowed),
                        blocked = boolInt(blocked),
                    )
                }.map { it.toLogEntry() }
            }
            .collect { emit(it) }
    }

actual fun observeLogChanges(onChanged: () -> Unit): () -> Unit =
    observeRoomInvalidations(WasmNetGuardDatabase.dao.observeLogTable(), onChanged)

actual suspend fun loadDnsEntries(): List<DnsEntry> =
    dao().getAllDns().map { it.toDnsEntry() }

actual suspend fun cleanupDns() {
    dao().cleanupDns(currentTimeMillis())
}

actual suspend fun clearDns() {
    dao().deleteAllDns()
}

actual suspend fun loadForwardingEntries(): List<ForwardingEntry> =
    dao().getAllForwards().map { it.toForwardingEntry() }

actual fun observeDnsEntries(): Flow<List<DnsEntry>> =
    flow {
        WasmNetGuardDatabase.ensureDemoDataSeeded()
        val d = WasmNetGuardDatabase.dao
        d.observeDnsTable()
            .map { d.getAllDns().map { it.toDnsEntry() } }
            .collect { emit(it) }
    }

actual fun observeForwardingEntries(): Flow<List<ForwardingEntry>> =
    flow {
        WasmNetGuardDatabase.ensureDemoDataSeeded()
        val d = WasmNetGuardDatabase.dao
        d.observeForwardTable()
            .map { d.getAllForwards().map { it.toForwardingEntry() } }
            .collect { emit(it) }
    }

actual fun observeForwardingChanges(onChanged: () -> Unit): () -> Unit =
    observeRoomInvalidations(WasmNetGuardDatabase.dao.observeForwardTable(), onChanged)

actual fun observeAccessEntries(uid: Int): Flow<List<AccessEntry>> =
    flow {
        WasmNetGuardDatabase.ensureDemoDataSeeded()
        val d = WasmNetGuardDatabase.dao
        d.observeAccessTable()
            .map { d.getAccessForUid(uid).map { it.toAccessEntry() } }
            .collect { emit(it) }
    }

actual suspend fun addForwardingEntry(
    protocol: Int,
    dport: Int,
    raddr: String,
    rport: Int,
    ruid: Int,
) {
    dao().insertForward(
        ForwardEntity(protocol = protocol, dport = dport, raddr = raddr, rport = rport, ruid = ruid),
    )
}

actual suspend fun deleteForwardingEntry(entry: ForwardingEntry) {
    dao().deleteForward(entry.protocol, entry.dport)
}

actual fun loadAppDisplayInfo(uid: Int, fallbackLabel: String): AppDisplayInfo =
    wasmDemoApps[uid] ?: AppDisplayInfo(label = fallbackLabel, packageName = null)

actual suspend fun loadAllRulesForPicker(): List<FirewallRule> =
    wasmDemoApps.map { (uid, info) ->
        FirewallRule(
            uid = uid,
            packageName = info.packageName,
            name = info.label,
        )
    }

actual fun registerUpdateCheckListener(onResult: (status: String?, version: String?) -> Unit): () -> Unit =
    { }

actual fun exportDnsToFile(onComplete: (success: Boolean, error: String?) -> Unit) {
    onComplete(false, null)
}

actual fun importHostsFromFile(onComplete: (success: Boolean) -> Unit) {
    onComplete(false)
}

actual suspend fun loadAccessEntries(uid: Int): List<AccessEntry> =
    dao().getAccessForUid(uid).map { it.toAccessEntry() }

actual suspend fun clearAccess(uid: Int) {
    dao().deleteAccessForUid(uid)
}

actual suspend fun clearLogs() {
    dao().deleteLogs(-1)
}

private fun boolInt(value: Boolean): Int = if (value) 1 else 0
