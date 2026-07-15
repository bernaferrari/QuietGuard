package com.bernaferari.renetguard.data.db

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import com.bernaferari.renetguard.Packet
import com.bernaferari.renetguard.Usage
import kotlinx.coroutines.flow.Flow

@Dao
interface NetGuardDao {
    /** Emits whenever the log table is invalidated (insert/delete/update). */
    @Query("SELECT COUNT(*) FROM log")
    fun observeLogTable(): Flow<Long>

    /** Emits whenever the forward table is invalidated. */
    @Query("SELECT COUNT(*) FROM forward")
    fun observeForwardTable(): Flow<Long>

    /** Emits whenever the dns table is invalidated. */
    @Query("SELECT COUNT(*) FROM dns")
    fun observeDnsTable(): Flow<Long>

    /** Emits whenever the access table is invalidated. */
    @Query("SELECT COUNT(*) FROM access")
    fun observeAccessTable(): Flow<Long>

    @Insert
    suspend fun insertLog(entity: LogEntity): Long

    @Query(
        """
        DELETE FROM log
        WHERE time > :afterTime
          AND protocol = :protocol
          AND version = :version
          AND flags = :flags
          AND daddr = :daddr
          AND dport = :dport
          AND uid = :uid
        """,
    )
    suspend fun deleteSynSniLogs(
        afterTime: Long,
        protocol: Int,
        version: Int,
        flags: String,
        daddr: String,
        dport: Int,
        uid: Int,
    ): Int

    @Transaction
    suspend fun insertLogEntry(
        entity: LogEntity,
        deleteSynSni: Boolean,
        synSniAfterTime: Long,
    ) {
        if (deleteSynSni) {
            deleteSynSniLogs(
                afterTime = synSniAfterTime,
                protocol = requireNotNull(entity.protocol),
                version = requireNotNull(entity.version),
                flags = "S",
                daddr = requireNotNull(entity.daddr),
                dport = requireNotNull(entity.dport),
                uid = requireNotNull(entity.uid),
            )
        }
        insertLog(entity)
    }

    @Query("DELETE FROM log WHERE :uid < 0 OR uid = :uid")
    suspend fun deleteLogs(uid: Int)

    @Query("DELETE FROM log WHERE time < :time")
    suspend fun deleteLogsBefore(time: Long): Int

    @Query(
        """
        SELECT ID, time, version, protocol, flags, saddr, sport, daddr, dport, dname, uid, data, allowed, connection, interactive
        FROM log
        WHERE (0 = 1
            OR (:udp = 1 AND protocol = 17)
            OR (:tcp = 1 AND protocol = 6)
            OR (:other = 1 AND (protocol IS NULL OR (protocol <> 6 AND protocol <> 17))))
          AND (0 = 1 OR (:allowed = 1 AND allowed = 1) OR (:blocked = 1 AND allowed = 0))
        ORDER BY time DESC
        """,
    )
    suspend fun getLogUnlimited(
        udp: Int,
        tcp: Int,
        other: Int,
        allowed: Int,
        blocked: Int,
    ): List<LogEntity>

    @Query(
        """
        SELECT ID, time, version, protocol, flags, saddr, sport, daddr, dport, dname, uid, data, allowed, connection, interactive
        FROM log
        WHERE (0 = 1
            OR (:udp = 1 AND protocol = 17)
            OR (:tcp = 1 AND protocol = 6)
            OR (:other = 1 AND (protocol IS NULL OR (protocol <> 6 AND protocol <> 17))))
          AND (0 = 1 OR (:allowed = 1 AND allowed = 1) OR (:blocked = 1 AND allowed = 0))
        ORDER BY time DESC
        LIMIT :limit
        """,
    )
    suspend fun getLogLimited(
        udp: Int,
        tcp: Int,
        other: Int,
        allowed: Int,
        blocked: Int,
        limit: Int,
    ): List<LogEntity>

    @Query(
        """
        SELECT ID, time, version, protocol, flags, saddr, sport, daddr, dport, dname, uid, data, allowed, connection, interactive
        FROM log
        WHERE daddr LIKE '%' || :find || '%'
           OR dname LIKE '%' || :find || '%'
           OR CAST(dport AS TEXT) = :find
           OR CAST(uid AS TEXT) = :find
        ORDER BY time DESC
        """,
    )
    suspend fun searchLogs(find: String): List<LogEntity>

    @Query(
        """
        SELECT * FROM access
        WHERE uid = :uid AND version = :version AND protocol = :protocol AND daddr = :daddr AND dport = :dport
        LIMIT 1
        """,
    )
    suspend fun getAccessByKey(
        uid: Int,
        version: Int,
        protocol: Int,
        daddr: String,
        dport: Int,
    ): AccessEntity?

    @Update
    suspend fun updateAccessEntity(entity: AccessEntity): Int

    @Insert
    suspend fun insertAccess(entity: AccessEntity): Long

    @Transaction
    suspend fun upsertAccess(
        packet: Packet,
        dname: String?,
        block: Int,
    ): Boolean {
        val keyDaddr = dname ?: packet.daddr ?: return false
        val existing =
            getAccessByKey(
                uid = packet.uid,
                version = packet.version,
                protocol = packet.protocol,
                daddr = keyDaddr,
                dport = packet.dport,
            )
        val allowed = if (packet.allowed) 1 else 0
        if (existing != null) {
            updateAccessEntity(
                existing.copy(
                    time = packet.time,
                    allowed = allowed,
                    block = if (block >= 0) block else existing.block,
                ),
            )
            return false
        }
        insertAccess(
            AccessEntity(
                uid = packet.uid,
                version = packet.version,
                protocol = packet.protocol,
                daddr = keyDaddr,
                dport = packet.dport,
                time = packet.time,
                allowed = allowed,
                block = block,
                sent = null,
                received = null,
                connections = null,
            ),
        )
        return true
    }

    @Transaction
    suspend fun updateUsageEntry(usage: Usage, dname: String?) {
        val keyDaddr = dname ?: usage.DAddr ?: return
        val existing =
            getAccessByKey(
                uid = usage.Uid,
                version = usage.Version,
                protocol = usage.Protocol,
                daddr = keyDaddr,
                dport = usage.DPort,
            ) ?: return
        updateAccessEntity(
            existing.copy(
                sent = (existing.sent ?: 0L) + usage.Sent,
                received = (existing.received ?: 0L) + usage.Received,
                connections = (existing.connections ?: 0) + 1,
            ),
        )
    }

    @Query("UPDATE access SET block = :block, allowed = -1 WHERE ID = :id")
    suspend fun setAccessBlock(id: Long, block: Int): Int

    @Query("DELETE FROM access")
    suspend fun deleteAllAccess()

    @Query("DELETE FROM access WHERE uid = :uid AND block < 0")
    suspend fun deleteAccessRules(uid: Int)

    @Query("DELETE FROM access WHERE uid = :uid")
    suspend fun deleteAccessForUid(uid: Int)

    @Query("UPDATE access SET sent = NULL, received = NULL, connections = NULL WHERE :uid < 0 OR uid = :uid")
    suspend fun resetUsage(uid: Int)

    @Query(
        """
        SELECT a.ID, a.uid, a.version, a.protocol, a.daddr, a.dport, a.time, a.allowed, a.block, a.sent, a.received, a.connections,
               (SELECT COUNT(DISTINCT d.qname)
                FROM dns d
                WHERE d.resource IN (SELECT d1.resource FROM dns d1 WHERE d1.qname = a.daddr)) AS count
        FROM access a
        WHERE a.uid = :uid
        ORDER BY a.time DESC
        LIMIT 250
        """,
    )
    suspend fun getAccessForUid(uid: Int): List<AccessWithCountRow>

    @Query("SELECT * FROM access WHERE block >= 0 ORDER BY uid")
    suspend fun getBlockedAccess(): List<AccessEntity>

    @Query(
        """
        SELECT MAX(time) AS time, daddr, allowed
        FROM access
        WHERE uid = :uid AND block < 0 AND time >= :since
        GROUP BY daddr, allowed
        ORDER BY time DESC
        LIMIT :limit
        """,
    )
    suspend fun getAccessUnset(uid: Int, limit: Int, since: Long): List<AccessUnsetRow>

    @Query("SELECT COUNT(*) FROM access WHERE block >= 0 AND uid = :uid")
    suspend fun countHosts(uid: Int): Long

    @Query(
        """
        SELECT * FROM dns
        WHERE qname = :qname AND aname = :aname AND resource = :resource
        LIMIT 1
        """,
    )
    suspend fun getDnsRecord(qname: String, aname: String, resource: String): DnsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDns(entity: DnsEntity): Long

    @Update
    suspend fun updateDns(entity: DnsEntity): Int

    @Transaction
    suspend fun upsertDns(entity: DnsEntity): Boolean {
        val existing = getDnsRecord(entity.qname, entity.aname, entity.resource)
        return if (existing == null) {
            insertDns(entity) >= 0
        } else {
            updateDns(entity.copy(id = existing.id)) == 1
        }
    }

    @Query("DELETE FROM dns WHERE time + ttl < :now")
    suspend fun cleanupDns(now: Long): Int

    @Query("DELETE FROM dns")
    suspend fun deleteAllDns()

    @Query(
        """
        SELECT d.qname
        FROM dns AS d
        WHERE d.resource = :resource
        ORDER BY (d.uid = :uid) DESC, d.qname
        LIMIT 1
        """,
    )
    suspend fun getQName(uid: Int, resource: String): String?

    @Query(
        """
        SELECT DISTINCT d2.qname AS qname
        FROM dns d1
        JOIN dns d2 ON d2.resource = d1.resource AND d2.ID <> d1.ID
        WHERE d1.qname = :qname
        ORDER BY d2.qname
        """,
    )
    suspend fun getAlternateQNames(qname: String): List<String>

    @Query("SELECT ID, time, qname, aname, resource, ttl, uid FROM dns ORDER BY resource, qname")
    suspend fun getAllDns(): List<DnsEntity>

    @Query(
        """
        SELECT a.uid, a.version, a.protocol, a.daddr, d.resource, a.dport, a.block, d.time, d.ttl
        FROM access AS a
        LEFT JOIN dns AS d ON d.qname = a.daddr
        WHERE a.block >= 0
          AND (d.time IS NULL OR d.time + d.ttl >= :now)
          AND (:dname IS NULL OR a.daddr = :dname)
        """,
    )
    suspend fun getAccessDns(now: Long, dname: String?): List<AccessDnsRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForward(entity: ForwardEntity): Long

    @Query("DELETE FROM forward")
    suspend fun deleteAllForwards()

    @Query("DELETE FROM forward WHERE protocol = :protocol AND dport = :dport")
    suspend fun deleteForward(protocol: Int, dport: Int)

    @Query("SELECT ID, protocol, dport, raddr, rport, ruid FROM forward ORDER BY dport")
    suspend fun getAllForwards(): List<ForwardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(entity: AppEntity): Long

    @Query("SELECT * FROM app WHERE package = :packageName LIMIT 1")
    suspend fun getApp(packageName: String): AppEntity?

    @Query("DELETE FROM app")
    suspend fun deleteAllApps()
}

data class AccessWithCountRow(
    @ColumnInfo(name = "ID")
    val id: Long,
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
    val count: Long,
)