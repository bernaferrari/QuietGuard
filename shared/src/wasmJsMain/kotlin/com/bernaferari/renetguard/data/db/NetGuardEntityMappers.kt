package com.bernaferari.renetguard.data.db

import com.bernaferari.renetguard.platform.AccessEntry
import com.bernaferari.renetguard.platform.DnsEntry
import com.bernaferari.renetguard.platform.ForwardingEntry
import com.bernaferari.renetguard.platform.LogEntry
import com.bernaferari.renetguard.platform.NetGuardPlatform

internal fun LogEntity.toLogEntry(): LogEntry {
    val protocolValue = protocol ?: -1
    return LogEntry(
        id = id,
        time = time,
        timeText = formatTimeHHmmss(time),
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

internal fun AccessWithCountRow.toAccessEntry(): AccessEntry =
    AccessEntry(
        time = time,
        timeText = formatTimeHHmm(time),
        daddr = daddr,
        dport = dport,
        allowed = allowed ?: -1,
    )

private fun formatTimeHHmmss(epochMs: Long): String {
    val totalSeconds = epochMs / 1000
    val seconds = totalSeconds % 60
    val totalMinutes = totalSeconds / 60
    val minutes = totalMinutes % 60
    val hours = (totalMinutes / 60) % 24
    return "${hours.twoDigits()}:${minutes.twoDigits()}:${seconds.twoDigits()}"
}

private fun formatTimeHHmm(epochMs: Long): String {
    val totalSeconds = epochMs / 1000
    val totalMinutes = totalSeconds / 60
    val minutes = totalMinutes % 60
    val hours = (totalMinutes / 60) % 24
    return "${hours.twoDigits()}:${minutes.twoDigits()}"
}

private fun Long.twoDigits(): String = toString().padStart(2, '0')
