package com.bernaferrari.quietguard.data.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "log",
    indices = [
        Index(value = ["time"]),
        Index(value = ["daddr"]),
        Index(value = ["dname"]),
        Index(value = ["dport"]),
        Index(value = ["uid"]),
    ],
)
data class LogEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ID")
    val id: Long = 0,
    val time: Long,
    val version: Int?,
    val protocol: Int?,
    val flags: String?,
    val saddr: String?,
    val sport: Int?,
    val daddr: String?,
    val dport: Int?,
    val dname: String?,
    val uid: Int?,
    val data: String?,
    val allowed: Int?,
    val connection: Int?,
    val interactive: Int?,
)

@Entity(
    tableName = "access",
    indices = [
        Index(value = ["uid", "version", "protocol", "daddr", "dport"], unique = true),
        Index(value = ["daddr"]),
        Index(value = ["block"]),
    ],
)
data class AccessEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ID")
    val id: Long = 0,
    val uid: Int,
    val version: Int,
    val protocol: Int,
    val daddr: String,
    val dport: Int,
    val time: Long,
    val allowed: Int?,
    val block: Int,
    val sent: Long?,
    val received: Long?,
    val connections: Int?,
)

@Entity(
    tableName = "dns",
    indices = [
        Index(value = ["qname", "aname", "resource"], unique = true),
        Index(value = ["resource"]),
    ],
)
data class DnsEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ID")
    val id: Long = 0,
    val time: Long,
    val qname: String,
    val aname: String,
    val resource: String,
    val ttl: Long?,
    val uid: Int?,
)

@Entity(
    tableName = "forward",
    indices = [Index(value = ["protocol", "dport"], unique = true)],
)
data class ForwardEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ID")
    val id: Long = 0,
    val protocol: Int,
    val dport: Int,
    val raddr: String,
    val rport: Int,
    val ruid: Int,
)

@Entity(
    tableName = "app",
    indices = [Index(value = ["package"], unique = true)],
)
data class AppEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ID")
    val id: Long = 0,
    @ColumnInfo(name = "package")
    val packageName: String,
    val label: String?,
    val system: Int,
    val internet: Int,
    val enabled: Int,
)

data class AccessUnsetRow(
    val time: Long,
    val daddr: String,
    val allowed: Int?,
)

data class AccessDnsRow(
    val uid: Int,
    val version: Int,
    val protocol: Int,
    val daddr: String,
    val resource: String?,
    val dport: Int,
    val block: Int,
    val time: Long?,
    val ttl: Long?,
)