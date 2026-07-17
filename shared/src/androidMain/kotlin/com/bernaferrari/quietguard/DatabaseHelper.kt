package com.bernaferrari.quietguard

import android.content.Context
import android.database.Cursor
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import com.bernaferrari.quietguard.data.db.AppEntity
import com.bernaferrari.quietguard.data.db.DbCursors
import com.bernaferrari.quietguard.data.db.LogEntity
import com.bernaferrari.quietguard.data.db.NetGuardDatabase
import com.bernaferrari.quietguard.data.db.createNetGuardDatabase
import com.bernaferrari.quietguard.data.preferences
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

class DatabaseHelper private constructor(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val database: NetGuardDatabase = createNetGuardDatabase(appContext)
    private val dao = database.netGuardDao()
    private val dbContext: CoroutineContext =
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private inline fun <T> dbCall(crossinline block: suspend () -> T): T =
        runBlocking(dbContext) { block() }

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
        notifyLogChanged()
    }

    fun clearLog(uid: Int) {
        dbCall { dao.deleteLogs(uid) }
        notifyLogChanged()
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

    fun getLog(
        udp: Boolean,
        tcp: Boolean,
        other: Boolean,
        allowed: Boolean,
        blocked: Boolean,
        limit: Int = 0,
    ): Cursor =
        dbCall {
            val rows =
                if (limit > 0) {
                    dao.getLogLimited(
                        udp = bool(udp),
                        tcp = bool(tcp),
                        other = bool(other),
                        allowed = bool(allowed),
                        blocked = bool(blocked),
                        limit = limit,
                    )
                } else {
                    dao.getLogUnlimited(
                        udp = bool(udp),
                        tcp = bool(tcp),
                        other = bool(other),
                        allowed = bool(allowed),
                        blocked = bool(blocked),
                    )
                }
            DbCursors.logs(rows)
        }

    fun searchLog(find: String): Cursor =
        dbCall { DbCursors.logs(dao.searchLogs(find)) }

    fun updateAccess(packet: Packet, dname: String?, block: Int): Boolean {
        val inserted = dbCall { dao.upsertAccess(packet, dname, block) }
        notifyAccessChanged()
        return inserted
    }

    fun updateUsage(usage: Usage, dname: String?) {
        dbCall { dao.updateUsageEntry(usage, dname) }
        notifyAccessChanged()
    }

    fun setAccess(id: Long, block: Int) {
        dbCall { dao.setAccessBlock(id, block) }
        notifyAccessChanged()
    }

    fun clearAccess() {
        dbCall { dao.deleteAllAccess() }
        notifyAccessChanged()
    }

    fun clearAccess(uid: Int, keeprules: Boolean) {
        dbCall {
            if (keeprules) {
                dao.deleteAccessRules(uid)
            } else {
                dao.deleteAccessForUid(uid)
            }
        }
        notifyAccessChanged()
    }

    fun resetUsage(uid: Int) {
        dbCall { dao.resetUsage(uid) }
        notifyAccessChanged()
    }

    fun getAccess(uid: Int): Cursor =
        dbCall { DbCursors.accessWithCount(dao.getAccessForUid(uid)) }

    fun getAccess(): Cursor =
        dbCall { DbCursors.access(dao.getBlockedAccess()) }

    fun getAccessUnset(uid: Int, limit: Int, since: Long): Cursor =
        dbCall { DbCursors.accessUnset(dao.getAccessUnset(uid, limit, since)) }

    fun getHostCount(uid: Int, usecache: Boolean): Long {
        if (usecache) {
            synchronized(mapUidHosts) {
                mapUidHosts[uid]?.let { return it }
            }
        }
        val hosts = dbCall { dao.countHosts(uid) }
        synchronized(mapUidHosts) {
            mapUidHosts[uid] = hosts
        }
        return hosts
    }

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
                ),
            )
        }
        notifyDnsChanged()
        return changed
    }

    fun cleanupDns() {
        dbCall {
            dao.cleanupDns(Date().time)
            Log.i(TAG, "Cleanup DNS")
        }
        notifyDnsChanged()
    }

    fun clearDns() {
        dbCall { dao.deleteAllDns() }
        notifyDnsChanged()
    }

    fun getQName(uid: Int, ip: String): String? =
        dbCall { dao.getQName(uid, ip) }

    fun getAlternateQNames(qname: String): Cursor =
        dbCall { DbCursors.alternateQNames(dao.getAlternateQNames(qname)) }

    fun getDns(): Cursor =
        dbCall { DbCursors.dns(dao.getAllDns()) }

    fun getAccessDns(dname: String?): Cursor =
        dbCall { DbCursors.accessDns(dao.getAccessDns(Date().time, dname)) }

    fun addForward(protocol: Int, dport: Int, raddr: String, rport: Int, ruid: Int) {
        dbCall {
            dao.insertForward(
                com.bernaferrari.quietguard.data.db.ForwardEntity(
                    protocol = protocol,
                    dport = dport,
                    raddr = raddr,
                    rport = rport,
                    ruid = ruid,
                ),
            )
        }
        notifyForwardChanged()
    }

    fun deleteForward() {
        dbCall { dao.deleteAllForwards() }
        notifyForwardChanged()
    }

    fun deleteForward(protocol: Int, dport: Int) {
        dbCall { dao.deleteForward(protocol, dport) }
        notifyForwardChanged()
    }

    fun getForwarding(): Cursor =
        dbCall { DbCursors.forwards(dao.getAllForwards()) }

    fun addApp(
        packageName: String,
        label: String?,
        system: Boolean,
        internet: Boolean,
        enabled: Boolean,
    ) {
        dbCall {
            dao.insertApp(
                AppEntity(
                    packageName = packageName,
                    label = label,
                    system = if (system) 1 else 0,
                    internet = if (internet) 1 else 0,
                    enabled = if (enabled) 1 else 0,
                ),
            )
        }
    }

    fun getApp(packageName: String): Cursor =
        dbCall { DbCursors.app(dao.getApp(packageName)) }

    fun clearApps() {
        dbCall { dao.deleteAllApps() }
    }

    fun addLogChangedListener(listener: LogChangedListener) {
        logChangedListeners.add(listener)
    }

    fun removeLogChangedListener(listener: LogChangedListener) {
        logChangedListeners.remove(listener)
    }

    fun addAccessChangedListener(listener: AccessChangedListener) {
        accessChangedListeners.add(listener)
    }

    fun removeAccessChangedListener(listener: AccessChangedListener) {
        accessChangedListeners.remove(listener)
    }

    fun addForwardChangedListener(listener: ForwardChangedListener) {
        forwardChangedListeners.add(listener)
    }

    fun removeForwardChangedListener(listener: ForwardChangedListener) {
        forwardChangedListeners.remove(listener)
    }

    fun addDnsChangedListener(listener: DnsChangedListener) {
        dnsChangedListeners.add(listener)
    }

    fun removeDnsChangedListener(listener: DnsChangedListener) {
        dnsChangedListeners.remove(listener)
    }

    private fun notifyLogChanged() {
        val msg = handler.obtainMessage()
        msg.what = MSG_LOG
        handler.sendMessage(msg)
    }

    private fun notifyAccessChanged() {
        val msg = handler.obtainMessage()
        msg.what = MSG_ACCESS
        handler.sendMessage(msg)
    }

    private fun notifyForwardChanged() {
        val msg = handler.obtainMessage()
        msg.what = MSG_FORWARD
        handler.sendMessage(msg)
    }

    private fun notifyDnsChanged() {
        val msg = handler.obtainMessage()
        msg.what = MSG_DNS
        handler.sendMessage(msg)
    }

    companion object {
        private const val TAG = "NetGuard.Database"

        private val logChangedListeners = mutableListOf<LogChangedListener>()
        private val accessChangedListeners = mutableListOf<AccessChangedListener>()
        private val forwardChangedListeners = mutableListOf<ForwardChangedListener>()
        private val dnsChangedListeners = mutableListOf<DnsChangedListener>()

        private val handlerThread: HandlerThread = HandlerThread("DatabaseHelper").apply { start() }
        private val handler: Handler =
            Handler(handlerThread.looper) { msg ->
                handleChangedNotification(msg)
                true
            }

        private val mapUidHosts = mutableMapOf<Int, Long>()

        private const val MSG_LOG = 1
        private const val MSG_ACCESS = 2
        private const val MSG_FORWARD = 3
        private const val MSG_DNS = 4

        private const val SYN_SNI_DELAY = 5000L

        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            if (instance == null) {
                instance = DatabaseHelper(context.applicationContext)
            }
            return instance!!
        }

        fun clearCache() {
            synchronized(mapUidHosts) {
                mapUidHosts.clear()
            }
        }

        private fun bool(value: Boolean): Int = if (value) 1 else 0

        private fun handleChangedNotification(msg: Message) {
            try {
                Thread.sleep(1000)
                if (handler.hasMessages(msg.what)) {
                    handler.removeMessages(msg.what)
                }
            } catch (_: InterruptedException) {
            }

            when (msg.what) {
                MSG_LOG -> {
                    for (listener in logChangedListeners) {
                        try {
                            listener.onChanged()
                        } catch (ex: Throwable) {
                            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                        }
                    }
                }

                MSG_ACCESS -> {
                    for (listener in accessChangedListeners) {
                        try {
                            listener.onChanged()
                        } catch (ex: Throwable) {
                            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                        }
                    }
                }

                MSG_FORWARD -> {
                    for (listener in forwardChangedListeners) {
                        try {
                            listener.onChanged()
                        } catch (ex: Throwable) {
                            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                        }
                    }
                }

                MSG_DNS -> {
                    for (listener in dnsChangedListeners) {
                        try {
                            listener.onChanged()
                        } catch (ex: Throwable) {
                            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                        }
                    }
                }
            }
        }
    }

    fun interface LogChangedListener {
        fun onChanged()
    }

    fun interface AccessChangedListener {
        fun onChanged()
    }

    fun interface ForwardChangedListener {
        fun onChanged()
    }

    fun interface DnsChangedListener {
        fun onChanged()
    }
}
