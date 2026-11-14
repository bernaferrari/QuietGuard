package com.bernaferrari.quietguard

import android.content.Context
import android.util.Log
import com.bernaferrari.quietguard.data.db.LogEntity
import com.bernaferrari.quietguard.data.db.NetGuardDatabase
import com.bernaferrari.quietguard.data.db.createNetGuardDatabase
import com.bernaferrari.quietguard.data.preferences
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.runBlocking

class AndroidFirewallDatabase private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val database: NetGuardDatabase = createNetGuardDatabase(appContext)
    val dao = database.netGuardDao()

    private inline fun <T> dbCall(crossinline block: suspend () -> T): T = runBlocking { block() }

    fun insertLog(packet: Packet, dname: String?, connection: Int, interactive: Boolean) {
        val deleteSynSni =
            packet.protocol == 6 &&
                packet.daddr != null &&
                packet.dport > 0 &&
                packet.uid > 0 &&
                packet.data == "sni"
        if (deleteSynSni) {
            Log.i(TAG, "Deleting SYN/SNI packet=$packet dname=$dname")
        }
        dbCall {
            dao.insertLogEntry(
                entity =
                    LogEntity(
                        time = packet.time,
                        version = packet.version,
                        protocol = if (packet.protocol < 0) null else packet.protocol,
                        flags = packet.flags,
                        saddr = packet.saddr,
                        sport = if (packet.sport < 0) null else packet.sport,
                        daddr = packet.daddr,
                        dport = if (packet.dport < 0) null else packet.dport,
                        dname = dname,
                        data = packet.data,
                        uid = if (packet.uid < 0) null else packet.uid,
                        allowed = if (packet.allowed) 1 else 0,
                        connection = connection,
                        interactive = if (interactive) 1 else 0,
                    ),
                deleteSynSni = deleteSynSni,
                synSniAfterTime = packet.time - SYN_SNI_DELAY,
            )
        }
    }

    fun clearLog(uid: Int) {
        dbCall { dao.deleteLogs(uid) }
    }

    fun cleanupLog(time: Long) {
        dbCall {
            val rows = dao.deleteLogsBefore(time)
            Log.i(
                TAG,
                "Cleanup log before=${SimpleDateFormat.getDateTimeInstance().format(Date(time))} rows=$rows",
            )
        }
    }

    fun updateAccess(packet: Packet, dname: String?, block: Int): Boolean {
        val inserted = dbCall { dao.upsertAccess(packet, dname, block) }
        return inserted
    }

    fun updateUsage(usage: Usage, dname: String?) {
        dbCall { dao.updateUsageEntry(usage, dname) }
    }

    fun clearAccess() {
        dbCall { dao.deleteAllAccess() }
    }

    fun clearAccess(uid: Int, keeprules: Boolean) {
        dbCall {
            if (keeprules) {
                dao.deleteAccessRules(uid)
            } else {
                dao.deleteAccessForUid(uid)
            }
        }
    }

    fun getAccessUnset(uid: Int, limit: Int, since: Long) = dbCall {
        dao.getAccessUnset(uid, limit, since)
    }

    fun getHostCounts() = dbCall { dao.getHostCounts() }.associate { it.uid to it.count }

    fun insertDns(rr: ResourceRecord): Boolean {
        val qname = rr.QName ?: return false
        val aname = rr.AName ?: return false
        val resource = rr.Resource ?: return false
        val min = appContext.preferences().getString("ttl", "259200")?.toIntOrNull() ?: 259200
        var ttl = rr.TTL
        if (ttl < min) {
            ttl = min
        }
        val changed = dbCall {
            dao.upsertDns(
                com.bernaferrari.quietguard.data.db.DnsEntity(
                    time = rr.Time,
                    qname = qname,
                    aname = aname,
                    resource = resource,
                    ttl = ttl * 1000L,
                    uid = rr.uid,
                )
            )
        }
        return changed
    }

    fun cleanupDns() {
        dbCall {
            dao.cleanupDns(Date().time)
            Log.i(TAG, "Cleanup DNS")
        }
    }

    fun clearDns() {
        dbCall { dao.deleteAllDns() }
    }

    fun getQName(uid: Int, ip: String): String? = dbCall { dao.getQName(uid, ip) }

    fun getAccessDns(dname: String?) = dbCall {
        dao.getAccessDns(System.currentTimeMillis(), dname)
    }

    fun addForward(protocol: Int, dport: Int, raddr: String, rport: Int, ruid: Int) {
        dbCall {
            dao.insertForward(
                com.bernaferrari.quietguard.data.db.ForwardEntity(
                    protocol = protocol,
                    dport = dport,
                    raddr = raddr,
                    rport = rport,
                    ruid = ruid,
                )
            )
        }
    }

    fun deleteForward(protocol: Int, dport: Int) {
        dbCall { dao.deleteForward(protocol, dport) }
    }

    fun getForwarding() = dbCall { dao.getAllForwards() }

    companion object {
        private const val TAG = "QuietGuard.Database"
        private const val SYN_SNI_DELAY = 5000L
        private var instance: AndroidFirewallDatabase? = null

        @Synchronized
        fun getInstance(context: Context): AndroidFirewallDatabase =
            instance ?: AndroidFirewallDatabase(context.applicationContext).also { instance = it }
    }
}
