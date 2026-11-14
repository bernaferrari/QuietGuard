package com.bernaferrari.quietguard.data.db

import com.bernaferrari.quietguard.platform.AccessEntry
import com.bernaferrari.quietguard.platform.DnsEntry
import com.bernaferrari.quietguard.platform.ForwardingEntry
import com.bernaferrari.quietguard.platform.LogEntry
import com.bernaferrari.quietguard.platform.NetGuardPlatform

internal fun LogEntity.toLogEntry(formatTime: (Long) -> String = ::formatTimeHHmm): LogEntry {
    val protocolValue = protocol ?: -1
    return LogEntry(
        id = id,
        time = time,
        timeText = formatTime(time),
        protocolLabel = NetGuardPlatform.uiHelpers.getProtocolName(protocolValue, 0, false),
        daddr = daddr.orEmpty(),
        dport = dport ?: -1,
        dname = dname,
        uid = uid ?: -1,
        allowed = allowed ?: -1,
    )
}

internal fun DnsEntity.toDnsEntry(): DnsEntry =
    DnsEntry(
        time = time,
        qname = qname,
        aname = aname,
        resource = resource,
        ttl = ttl?.toInt() ?: 0,
        uid = uid ?: -1,
    )

internal fun ForwardEntity.toForwardingEntry(): ForwardingEntry =
    ForwardingEntry(
        protocol = protocol,
        dport = dport,
        raddr = raddr,
        rport = rport,
        ruid = ruid,
    )

internal fun AccessWithCountRow.toAccessEntry(
    formatTime: (Long) -> String = ::formatTimeHHmm
): AccessEntry =
    AccessEntry(
        time = time,
        timeText = formatTime(time),
        daddr = daddr,
        dport = dport,
        allowed = allowed ?: -1,
    )

private fun formatTimeHHmm(epochMs: Long): String {
    val totalSeconds = epochMs / 1000
    val totalMinutes = totalSeconds / 60
    val minutes = totalMinutes % 60
    val hours = (totalMinutes / 60) % 24
    return "${hours.twoDigits()}:${minutes.twoDigits()}"
}

private fun Long.twoDigits(): String = toString().padStart(2, '0')
