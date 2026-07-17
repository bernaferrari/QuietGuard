package com.bernaferrari.quietguard.data

import com.bernaferrari.quietguard.platform.AccessEntry
import com.bernaferrari.quietguard.platform.AppDisplayInfo
import com.bernaferrari.quietguard.platform.DnsEntry
import com.bernaferrari.quietguard.platform.ForwardingEntry
import com.bernaferrari.quietguard.platform.LogEntry
import com.bernaferrari.quietguard.platform.addForwardingEntry as platformAddForwardingEntry
import com.bernaferrari.quietguard.platform.cleanupDns as platformCleanupDns
import com.bernaferrari.quietguard.platform.clearAccess as platformClearAccess
import com.bernaferrari.quietguard.platform.clearDns as platformClearDns
import com.bernaferrari.quietguard.platform.clearLogs as platformClearLogs
import com.bernaferrari.quietguard.platform.deleteForwardingEntry as platformDeleteForwardingEntry
import com.bernaferrari.quietguard.platform.loadAppDisplayInfo
import com.bernaferrari.quietguard.platform.observeAccessEntries
import com.bernaferrari.quietguard.platform.observeDnsEntries
import com.bernaferrari.quietguard.platform.observeForwardingEntries
import com.bernaferrari.quietguard.platform.observeLogs as platformObserveLogs
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
