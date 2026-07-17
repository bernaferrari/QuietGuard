package com.bernaferrari.quietguard.data.db

internal suspend fun seedWebDemoDataIfEmpty(dao: NetGuardDao) {
    if (dao.getLogLimited(udp = 1, tcp = 1, other = 1, allowed = 1, blocked = 1, limit = 1).isNotEmpty()) {
        return
    }

    demoLogs().forEach { log ->
        dao.insertLog(log)
    }

    listOf(
        dns(1_700_000_000_000L, "google.com", "google.com", "A", 300_000, 10101),
        dns(1_700_000_050_000L, "gmail.com", "gmail.com", "A", 300_000, 10102),
        dns(1_700_000_100_000L, "spotify.com", "spotify.com", "A", 300_000, 10103),
        dns(1_700_000_150_000L, "cdninstagram.com", "cdninstagram.com", "A", 300_000, 10104),
        dns(1_700_000_200_000L, "whatsapp.net", "whatsapp.net", "A", 300_000, 10105),
    ).forEach { dao.insertDns(it) }

    dao.insertForward(
        ForwardEntity(protocol = 6, dport = 8080, raddr = "192.168.1.10", rport = 80, ruid = 10101),
    )

    listOf(
        AccessEntity(
            uid = 10101,
            version = 4,
            protocol = 6,
            daddr = "142.250.80.46",
            dport = 443,
            time = 1_700_000_000_000L,
            allowed = 0,
            block = -1,
            sent = null,
            received = null,
            connections = null,
        ),
        AccessEntity(
            uid = 10101,
            version = 4,
            protocol = 17,
            daddr = "8.8.8.8",
            dport = 53,
            time = 1_700_000_100_000L,
            allowed = 1,
            block = -1,
            sent = null,
            received = null,
            connections = null,
        ),
    ).forEach { dao.insertAccess(it) }
}

private fun demoLogs(): List<LogEntity> =
    listOf(
        log(1_700_000_000_000L, 6, "142.250.80.46", 443, "google.com", 10101, 0),
        log(1_700_000_050_000L, 6, "142.251.40.78", 443, "gmail.com", 10102, 0),
        log(1_700_000_100_000L, 17, "8.8.8.8", 53, null, 10103, 1),
        log(1_700_000_150_000L, 6, "31.13.64.35", 443, "facebook.com", 10108, 0),
        log(1_700_000_200_000L, 6, "151.101.1.140", 443, "reddit.com", 10113, 1),
        log(1_700_000_250_000L, 17, "1.1.1.1", 53, null, 10105, 1),
        log(1_700_000_300_000L, 6, "104.16.132.229", 443, "discord.com", 10112, 0),
        log(1_700_000_350_000L, 6, "149.154.167.50", 443, "telegram.org", 10114, 1),
        log(1_700_000_400_000L, 6, "13.107.42.14", 443, "tiktokcdn.com", 10115, 0),
        log(1_700_000_450_000L, 6, "52.84.0.44", 443, "slack.com", 10116, 1),
        log(1_700_000_500_000L, 6, "13.107.42.14", 443, "linkedin.com", 10117, 0),
        log(1_700_000_550_000L, 6, "104.18.32.45", 443, "signal.org", 10118, 1),
    )

private fun dns(
    time: Long,
    qname: String,
    aname: String,
    resource: String,
    ttl: Long,
    uid: Int,
): DnsEntity =
    DnsEntity(
        time = time,
        qname = qname,
        aname = aname,
        resource = resource,
        ttl = ttl,
        uid = uid,
    )

private fun log(
    time: Long,
    protocol: Int,
    daddr: String,
    dport: Int,
    dname: String?,
    uid: Int,
    allowed: Int,
): LogEntity =
    LogEntity(
        time = time,
        version = 4,
        protocol = protocol,
        flags = null,
        saddr = null,
        sport = null,
        daddr = daddr,
        dport = dport,
        dname = dname,
        uid = uid,
        data = null,
        allowed = allowed,
        connection = 0,
        interactive = 0,
    )