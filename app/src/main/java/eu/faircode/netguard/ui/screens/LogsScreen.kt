package eu.faircode.netguard.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.faircode.netguard.ActivityPro
import eu.faircode.netguard.DatabaseHelper
import eu.faircode.netguard.IAB
import eu.faircode.netguard.R
import eu.faircode.netguard.Util
import eu.faircode.netguard.data.Prefs
import java.text.SimpleDateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking

@Composable
fun LogsScreen() {
    val context = LocalContext.current
    val hasLog = remember { IAB.isPurchased(ActivityPro.SKU_LOG, context) }
    var entries by remember { mutableStateOf<List<LogEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    val logListener =
        remember {
            DatabaseHelper.LogChangedListener {
                val udp = Prefs.getBoolean("proto_udp", true)
                val tcp = Prefs.getBoolean("proto_tcp", true)
                val other = Prefs.getBoolean("proto_other", true)
                val allowed = Prefs.getBoolean("traffic_allowed", true)
                val blocked = Prefs.getBoolean("traffic_blocked", true)
                entries = runBlockingLogs(context, udp, tcp, other, allowed, blocked)
                isLoading = false
            }
        }

    DisposableEffect(hasLog) {
        if (!hasLog) {
            return@DisposableEffect onDispose { }
        }
        DatabaseHelper.getInstance(context).addLogChangedListener(logListener)
        onDispose {
            DatabaseHelper.getInstance(context).removeLogChangedListener(logListener)
        }
    }

    LaunchedEffect(refreshKey) {
        if (!hasLog) return@LaunchedEffect
        isLoading = true
        val udp = Prefs.getBoolean("proto_udp", true)
        val tcp = Prefs.getBoolean("proto_tcp", true)
        val other = Prefs.getBoolean("proto_other", true)
        val allowed = Prefs.getBoolean("traffic_allowed", true)
        val blocked = Prefs.getBoolean("traffic_blocked", true)
        entries = loadLogs(context, udp, tcp, other, allowed, blocked)
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!hasLog) {
            Text(
                text = stringResource(R.string.title_pro),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.msg_log_disabled),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(
                onClick = { context.startActivity(Intent(context, ActivityPro::class.java)) },
            ) {
                Text(text = stringResource(R.string.title_pro))
            }
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.menu_log),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.home_logs_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(onClick = { refreshKey += 1 }) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.menu_refresh))
            }
        }

        if (isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(entries, key = { "${it.time}_${it.uid}_${it.daddr}_${it.dport}" }) { entry ->
                val allowedColor =
                    if (entry.allowed > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = entry.timeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = entry.protocolLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "${entry.daddr}:${entry.dport}",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (entry.allowed >= 0) allowedColor else MaterialTheme.colorScheme.onSurface,
                        )
                        entry.dname?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (entry.uid > 0) {
                            Text(
                                text = Util.getApplicationNames(entry.uid, context).joinToString(", "),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class LogEntry(
    val time: Long,
    val timeText: String,
    val protocolLabel: String,
    val daddr: String,
    val dport: Int,
    val dname: String?,
    val uid: Int,
    val allowed: Int,
)

private suspend fun loadLogs(
    context: android.content.Context,
    udp: Boolean,
    tcp: Boolean,
    other: Boolean,
    allowed: Boolean,
    blocked: Boolean,
): List<LogEntry> =
    withContext(Dispatchers.IO) {
        val result = mutableListOf<LogEntry>()
        DatabaseHelper.getInstance(context).getLog(udp, tcp, other, allowed, blocked).use { cursor ->
            val colTime = cursor.getColumnIndex("time")
            val colProtocol = cursor.getColumnIndex("protocol")
            val colDAddr = cursor.getColumnIndex("daddr")
            val colDPort = cursor.getColumnIndex("dport")
            val colDName = cursor.getColumnIndex("dname")
            val colUid = cursor.getColumnIndex("uid")
            val colAllowed = cursor.getColumnIndex("allowed")
            val timeFormat = SimpleDateFormat("HH:mm:ss")
            while (cursor.moveToNext()) {
                val time = cursor.getLong(colTime)
                val protocol = if (cursor.isNull(colProtocol)) -1 else cursor.getInt(colProtocol)
                val daddr = cursor.getString(colDAddr)
                val dport = if (cursor.isNull(colDPort)) -1 else cursor.getInt(colDPort)
                val dname = if (cursor.isNull(colDName)) null else cursor.getString(colDName)
                val uid = if (cursor.isNull(colUid)) -1 else cursor.getInt(colUid)
                val allow = if (cursor.isNull(colAllowed)) -1 else cursor.getInt(colAllowed)
                result.add(
                    LogEntry(
                        time = time,
                        timeText = timeFormat.format(time),
                        protocolLabel = Util.getProtocolName(protocol, 0, false),
                        daddr = daddr,
                        dport = dport,
                        dname = dname,
                        uid = uid,
                        allowed = allow,
                    ),
                )
            }
        }
        result
    }

private fun runBlockingLogs(
    context: android.content.Context,
    udp: Boolean,
    tcp: Boolean,
    other: Boolean,
    allowed: Boolean,
    blocked: Boolean,
): List<LogEntry> {
    return runCatching {
        runBlocking {
            loadLogs(context, udp, tcp, other, allowed, blocked)
        }
    }.getOrDefault(emptyList())
}
