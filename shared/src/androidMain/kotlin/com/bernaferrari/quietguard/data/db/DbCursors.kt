package com.bernaferrari.quietguard.data.db

import android.database.Cursor
import android.database.MatrixCursor

internal object DbCursors {
    fun logs(rows: List<LogEntity>): Cursor {
        val cursor =
            MatrixCursor(
                arrayOf(
                    "_id",
                    "ID",
                    "time",
                    "version",
                    "protocol",
                    "flags",
                    "saddr",
                    "sport",
                    "daddr",
                    "dport",
                    "dname",
                    "uid",
                    "data",
                    "allowed",
                    "connection",
                    "interactive",
                ),
            )
        rows.forEach { row ->
            cursor.addRow(
                arrayOf<Any?>(
                    row.id,
                    row.id,
                    row.time,
                    row.version,
                    row.protocol,
                    row.flags,
                    row.saddr,
                    row.sport,
                    row.daddr,
                    row.dport,
                    row.dname,
                    row.uid,
                    row.data,
                    row.allowed,
                    row.connection,
                    row.interactive,
                ),
            )
        }
        return cursor
    }

    fun accessWithCount(rows: List<AccessWithCountRow>): Cursor {
        val cursor =
            MatrixCursor(
                arrayOf(
                    "_id",
                    "ID",
                    "uid",
                    "version",
                    "protocol",
                    "daddr",
                    "dport",
                    "time",
                    "allowed",
                    "block",
                    "sent",
                    "received",
                    "connections",
                    "count",
                ),
            )
        rows.forEach { row ->
            cursor.addRow(
                arrayOf<Any?>(
                    row.id,
                    row.id,
                    row.uid,
                    row.version,
                    row.protocol,
                    row.daddr,
                    row.dport,
                    row.time,
                    row.allowed,
                    row.block,
                    row.sent,
                    row.received,
                    row.connections,
                    row.count,
                ),
            )
        }
        return cursor
    }

    fun access(rows: List<AccessEntity>): Cursor {
        val cursor =
            MatrixCursor(
                arrayOf(
                    "ID",
                    "uid",
                    "version",
                    "protocol",
                    "daddr",
                    "dport",
                    "time",
                    "allowed",
                    "block",
                    "sent",
                    "received",
                    "connections",
                ),
            )
        rows.forEach { row ->
            cursor.addRow(
                arrayOf<Any?>(
                    row.id,
                    row.uid,
                    row.version,
                    row.protocol,
                    row.daddr,
                    row.dport,
                    row.time,
                    row.allowed,
                    row.block,
                    row.sent,
                    row.received,
                    row.connections,
                ),
            )
        }
        return cursor
    }

    fun accessUnset(rows: List<AccessUnsetRow>): Cursor {
        val cursor = MatrixCursor(arrayOf("time", "daddr", "allowed"))
        rows.forEach { row ->
            cursor.addRow(arrayOf<Any?>(row.time, row.daddr, row.allowed))
        }
        return cursor
    }

    fun dns(rows: List<DnsEntity>): Cursor {
        val cursor = MatrixCursor(arrayOf("_id", "ID", "time", "qname", "aname", "resource", "ttl", "uid"))
        rows.forEach { row ->
            cursor.addRow(
                arrayOf<Any?>(
                    row.id,
                    row.id,
                    row.time,
                    row.qname,
                    row.aname,
                    row.resource,
                    row.ttl,
                    row.uid,
                ),
            )
        }
        return cursor
    }

    fun alternateQNames(rows: List<String>): Cursor {
        val cursor = MatrixCursor(arrayOf("qname"))
        rows.forEach { qname -> cursor.addRow(arrayOf<Any?>(qname)) }
        return cursor
    }

    fun accessDns(rows: List<AccessDnsRow>): Cursor {
        val cursor =
            MatrixCursor(arrayOf("uid", "version", "protocol", "daddr", "resource", "dport", "block", "time", "ttl"))
        rows.forEach { row ->
            cursor.addRow(
                arrayOf<Any?>(
                    row.uid,
                    row.version,
                    row.protocol,
                    row.daddr,
                    row.resource,
                    row.dport,
                    row.block,
                    row.time,
                    row.ttl,
                ),
            )
        }
        return cursor
    }

    fun forwards(rows: List<ForwardEntity>): Cursor {
        val cursor = MatrixCursor(arrayOf("_id", "ID", "protocol", "dport", "raddr", "rport", "ruid"))
        rows.forEach { row ->
            cursor.addRow(
                arrayOf<Any?>(
                    row.id,
                    row.id,
                    row.protocol,
                    row.dport,
                    row.raddr,
                    row.rport,
                    row.ruid,
                ),
            )
        }
        return cursor
    }

    fun app(entity: AppEntity?): Cursor {
        val cursor = MatrixCursor(arrayOf("ID", "package", "label", "system", "internet", "enabled"))
        if (entity != null) {
            cursor.addRow(
                arrayOf<Any?>(
                    entity.id,
                    entity.packageName,
                    entity.label,
                    entity.system,
                    entity.internet,
                    entity.enabled,
                ),
            )
        }
        return cursor
    }
}