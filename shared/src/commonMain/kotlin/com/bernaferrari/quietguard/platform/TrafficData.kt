package com.bernaferrari.quietguard.platform

import com.bernaferrari.quietguard.domain.FirewallRule
import kotlinx.coroutines.flow.Flow

data class LogEntry(
    val id: Long,
    val time: Long,
    val timeText: String,
    val protocolLabel: String,
    val daddr: String,
    val dport: Int,
    val dname: String?,
    val uid: Int,
    val allowed: Int,
)

data class DnsEntry(
    val time: Long,
    val qname: String,
    val aname: String,
    val resource: String,
    val ttl: Int,
    val uid: Int,
)

data class ForwardingEntry(
    val protocol: Int,
    val dport: Int,
    val raddr: String,
    val rport: Int,
    val ruid: Int,
)

data class AppDisplayInfo(
    val label: String,
    val packageName: String?,
)

expect suspend fun loadLogs(
    udp: Boolean,
    tcp: Boolean,
    other: Boolean,
    allowed: Boolean,
    blocked: Boolean,
    limit: Int,
): List<LogEntry>

/** Reactive logs stream; re-emits when the log table changes or filters are applied by the caller. */
expect fun observeLogs(
    udp: Boolean,
    tcp: Boolean,
    other: Boolean,
    allowed: Boolean,
    blocked: Boolean,
    limit: Int,
): Flow<List<LogEntry>>

@Deprecated("Prefer observeLogs().collectAsState()", ReplaceWith("observeLogs(udp, tcp, other, allowed, blocked, limit)"))
expect fun observeLogChanges(onChanged: () -> Unit): () -> Unit

expect suspend fun loadDnsEntries(): List<DnsEntry>

expect fun observeDnsEntries(): Flow<List<DnsEntry>>

expect suspend fun cleanupDns()

expect suspend fun clearDns()

expect suspend fun loadForwardingEntries(): List<ForwardingEntry>

expect fun observeForwardingEntries(): Flow<List<ForwardingEntry>>

@Deprecated("Prefer observeForwardingEntries().collectAsState()", ReplaceWith("observeForwardingEntries()"))
expect fun observeForwardingChanges(onChanged: () -> Unit): () -> Unit

expect suspend fun addForwardingEntry(
    protocol: Int,
    dport: Int,
    raddr: String,
    rport: Int,
    ruid: Int,
)

expect suspend fun deleteForwardingEntry(entry: ForwardingEntry)

expect fun loadAppDisplayInfo(uid: Int, fallbackLabel: String): AppDisplayInfo

expect suspend fun loadAllRulesForPicker(): List<FirewallRule>

expect fun registerUpdateCheckListener(onResult: (status: String?, version: String?) -> Unit): () -> Unit

expect fun exportDnsToFile(onComplete: (success: Boolean, error: String?) -> Unit)

expect fun importHostsFromFile(onComplete: (success: Boolean) -> Unit)

data class AccessEntry(
    val time: Long,
    val timeText: String,
    val daddr: String,
    val dport: Int,
    val allowed: Int,
)

expect suspend fun loadAccessEntries(uid: Int): List<AccessEntry>

expect fun observeAccessEntries(uid: Int): Flow<List<AccessEntry>>

expect suspend fun clearAccess(uid: Int)

expect suspend fun clearLogs()