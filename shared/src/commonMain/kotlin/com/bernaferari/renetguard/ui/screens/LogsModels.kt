package com.bernaferari.renetguard.ui.screens

import com.bernaferari.renetguard.data.PreferencesRepository

const val LOGS_UI_MAX_ROWS = 2000

enum class LogsOutcomeFilter {
    All,
    Allowed,
    Blocked,
}

enum class LogsProtocolFilter {
    All,
    Udp,
    Tcp,
    Other,
}

enum class LogsGroupMode {
    Timeline,
    ByApp,
}

data class LogQueryFlags(
    val udp: Boolean,
    val tcp: Boolean,
    val other: Boolean,
    val allowed: Boolean,
    val blocked: Boolean,
)

fun defaultOutcomeFilterFromPrefs(prefs: PreferencesRepository): LogsOutcomeFilter {
    val allowed = prefs.getBoolean("traffic_allowed", true)
    val blocked = prefs.getBoolean("traffic_blocked", true)
    return when {
        allowed && !blocked -> LogsOutcomeFilter.Allowed
        !allowed && blocked -> LogsOutcomeFilter.Blocked
        else -> LogsOutcomeFilter.All
    }
}

fun defaultProtocolFilterFromPrefs(prefs: PreferencesRepository): LogsProtocolFilter {
    val udp = prefs.getBoolean("proto_udp", true)
    val tcp = prefs.getBoolean("proto_tcp", true)
    val other = prefs.getBoolean("proto_other", true)
    return when {
        udp && !tcp && !other -> LogsProtocolFilter.Udp
        !udp && tcp && !other -> LogsProtocolFilter.Tcp
        !udp && !tcp && other -> LogsProtocolFilter.Other
        else -> LogsProtocolFilter.All
    }
}

fun buildLogQueryFlags(
    protocolFilter: LogsProtocolFilter,
    outcomeFilter: LogsOutcomeFilter,
): LogQueryFlags {
    val protocolFlags =
        when (protocolFilter) {
            LogsProtocolFilter.All -> Triple(true, true, true)
            LogsProtocolFilter.Udp -> Triple(true, false, false)
            LogsProtocolFilter.Tcp -> Triple(false, true, false)
            LogsProtocolFilter.Other -> Triple(false, false, true)
        }
    val outcomeFlags =
        when (outcomeFilter) {
            LogsOutcomeFilter.All -> Pair(true, true)
            LogsOutcomeFilter.Allowed -> Pair(true, false)
            LogsOutcomeFilter.Blocked -> Pair(false, true)
        }
    return LogQueryFlags(
        udp = protocolFlags.first,
        tcp = protocolFlags.second,
        other = protocolFlags.third,
        allowed = outcomeFlags.first,
        blocked = outcomeFlags.second,
    )
}
