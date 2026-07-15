package com.bernaferari.renetguard.data

import com.bernaferari.renetguard.platform.AccessEntry
import com.bernaferari.renetguard.platform.AppDisplayInfo
import com.bernaferari.renetguard.platform.DnsEntry
import com.bernaferari.renetguard.platform.ForwardingEntry
import com.bernaferari.renetguard.platform.LogEntry
import com.bernaferari.renetguard.platform.addForwardingEntry as platformAddForwardingEntry
import com.bernaferari.renetguard.platform.cleanupDns as platformCleanupDns
import com.bernaferari.renetguard.platform.clearAccess as platformClearAccess
import com.bernaferari.renetguard.platform.clearDns as platformClearDns
import com.bernaferari.renetguard.platform.clearLogs as platformClearLogs
import com.bernaferari.renetguard.platform.deleteForwardingEntry as platformDeleteForwardingEntry
import com.bernaferari.renetguard.platform.loadAppDisplayInfo
import com.bernaferari.renetguard.platform.observeAccessEntries
import com.bernaferari.renetguard.platform.observeDnsEntries
import com.bernaferari.renetguard.platform.observeForwardingEntries
import com.bernaferari.renetguard.platform.observeLogs as platformObserveLogs
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

/**
 * UI-facing traffic/log/DNS/forwarding access via Koin.
 *
 * Screens inject this instead of calling platform expect/actual functions directly.
 * Platform implementations still do the work (Room Flow on wasm, DatabaseHelper on Android).
 */
@Single
class TrafficRepository {
    fun observeLogs(
        udp: Boolean,
        tcp: Boolean,
        other: Boolean,
        allowed: Boolean,
        blocked: Boolean,
        limit: Int,
    ): Flow<List<LogEntry>> = platformObserveLogs(udp, tcp, other, allowed, blocked, limit)

    fun observeDns(): Flow<List<DnsEntry>> = observeDnsEntries()

    fun observeForwarding(): Flow<List<ForwardingEntry>> = observeForwardingEntries()

    fun observeAccess(uid: Int): Flow<List<AccessEntry>> = observeAccessEntries(uid)

    fun appDisplay(uid: Int, fallbackLabel: String): AppDisplayInfo = loadAppDisplayInfo(uid, fallbackLabel)

    suspend fun cleanupDnsEntries() = platformCleanupDns()

    suspend fun clearAllDns() = platformClearDns()

    suspend fun clearAllLogs() = platformClearLogs()

    suspend fun clearAccessForUid(uid: Int) = platformClearAccess(uid)

    suspend fun addForward(
        protocol: Int,
        dport: Int,
        raddr: String,
        rport: Int,
        ruid: Int,
    ) = platformAddForwardingEntry(protocol, dport, raddr, rport, ruid)

    suspend fun deleteForward(entry: ForwardingEntry) = platformDeleteForwardingEntry(entry)
}
