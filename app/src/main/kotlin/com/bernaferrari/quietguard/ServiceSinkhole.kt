package com.bernaferrari.quietguard

import android.annotation.TargetApi
import android.app.AlarmManager
import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Log
import androidx.core.content.ContextCompat
import com.bernaferrari.quietguard.data.PreferenceKeys
import com.bernaferrari.quietguard.data.PreferencesRepository
import com.bernaferrari.quietguard.data.db.ForwardEntity
import com.bernaferrari.quietguard.data.preferences
import com.bernaferrari.quietguard.netguard.NativePcapConfig
import com.bernaferrari.quietguard.netguard.NativeSocks5Config
import com.bernaferrari.quietguard.netguard.capabilities
import com.bernaferrari.quietguard.service.*
import com.bernaferrari.quietguard.shared.R
import java.io.File
import java.io.IOException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.UnknownHostException
import java.util.Date
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.locks.ReentrantReadWriteLock

class ServiceSinkhole : VpnService() {
    @Volatile private var destroying = false
    private val updateExecutor = Executors.newSingleThreadExecutor()

    private var registeredUser = false
    private var registeredIdleState = false
    private var registeredApState = false
    private var registeredPackageChanged = false

    private var registeredInteractiveState = false

    private var state = State.none
    private var userForeground = true
    private var lastConnected = false
    private var lastMetered = true
    private var lastInteractive = false

    private var removePrefsListener: (() -> Unit)? = null

    private val prefs: PreferencesRepository
        get() = applicationContext.preferences()

    private val notifications by lazy { SinkholeNotifications(this, prefs) }
    private val trafficNotifications by lazy { TrafficNotificationPresenter(this, prefs) }
    private val networkMonitor by lazy {
        SinkholeNetworkMonitor(this, prefs) { reason -> reload(reason, this, false) }
    }
    private val telephonyMonitor by lazy {
        SinkholeTelephonyMonitor(
            context = this,
            isFirewallEnabled = { prefs.getBoolean("enabled", false) },
            usesMeteredGenerationRules = {
                prefs.getBoolean("unmetered_2g", false) ||
                    prefs.getBoolean("unmetered_3g", false) ||
                    prefs.getBoolean("unmetered_4g", false)
            },
            onReload = { reason -> reload(reason, this, false) },
            onStart = { start("call state", this) },
            onTemporaryStop = { stop("call state", this, true) },
        )
    }
    private val foregroundNotifications by lazy {
        SinkholeForegroundNotifications(
            context = this,
            prefs = prefs,
            isLockedDown = { isLockedDown(lastMetered) },
            enforcingNotificationId = NOTIFY_ENFORCING,
        )
    }
    private val domainLists by lazy { SinkholeDomainLists(this, prefs) }

    private var lastBuilder: TrackingVpnBuilder? = null
    private var vpn: ParcelFileDescriptor? = null
    private var temporarilyStopped = false

    private val allowedUids = HashSet<Int>()
    private val knownUids = HashSet<Int>()
    private val mapUidIPFilters = HashMap<IpRuleKey, MutableMap<InetAddress, IpRule>>()
    private val mapForward = HashMap<ForwardKey, ForwardEntity>()
    private val mapNotify = HashMap<Int, Boolean>()
    private val lock = ReentrantReadWriteLock(true)

    private inline fun <T> withPolicyRead(block: () -> T): T {
        lock.readLock().lock()
        return try {
            block()
        } finally {
            lock.readLock().unlock()
        }
    }

    private inline fun <T> withPolicyWrite(block: () -> T): T {
        lock.writeLock().lock()
        return try {
            block()
        } finally {
            lock.writeLock().unlock()
        }
    }

    @Volatile private lateinit var commandLooper: Looper

    @Volatile private lateinit var logLooper: Looper

    @Volatile private lateinit var statsLooper: Looper

    @Volatile private lateinit var commandHandler: SinkholeCommandHandler

    @Volatile private lateinit var logHandler: SinkholeLogHandler

    @Volatile private lateinit var statsHandler: SinkholeStatsHandler
    private var statsWhenMs: Long = 0
    private val trafficStatistics = TrafficStatistics()

    private val executor: ExecutorService = Executors.newCachedThreadPool()

    private lateinit var nativeEngine: NativeVpnEngineController

    private val nativeCallbacks by lazy {
        NativeVpnCallbacks(
            uidFor = ::getUidQ,
            addressAllowed = ::isAddressAllowed,
            domainBlocked = ::isDomainBlocked,
            packetLogger = ::logPacket,
            dnsRecorder = ::dnsResolved,
            socketProtector = ::protect,
            usageRecorder = ::accountUsage,
            exitReporter = { nativeExit(it) },
        )
    }

    private fun handleCommandIntent(intent: Intent) {
        val cmd = getCommandExtra(intent) ?: return
        val reason = intent.getStringExtra(EXTRA_REASON)
        Log.i(
            TAG,
            "Executing intent=$intent command=$cmd reason=$reason" +
                " vpn=" +
                (vpn != null) +
                " user=" +
                (Process.myUid() / 100000),
        )

        if (cmd != Command.stop && !userForeground) {
            Log.i(TAG, "Command $cmd ignored for background user")
            return
        }

        if (cmd == Command.stop) {
            temporarilyStopped = intent.getBooleanExtra(EXTRA_TEMPORARY, false)
        } else if (cmd == Command.start) {
            temporarilyStopped = false
        } else if (cmd == Command.reload && temporarilyStopped) {
            Log.i(TAG, "Command $cmd ignored because of temporary stop")
            return
        }

        if (prefs.getBoolean(PreferenceKeys.SCREEN_ON)) {
            if (!registeredInteractiveState) {
                Log.i(TAG, "Starting listening for interactive state changes")
                lastInteractive = Util.isInteractive(this@ServiceSinkhole)
                val ifInteractive = IntentFilter()
                ifInteractive.addAction(Intent.ACTION_SCREEN_ON)
                ifInteractive.addAction(Intent.ACTION_SCREEN_OFF)
                ifInteractive.addAction(ACTION_SCREEN_OFF_DELAYED)
                ContextCompat.registerReceiver(
                    this@ServiceSinkhole,
                    interactiveStateReceiver,
                    ifInteractive,
                    ContextCompat.RECEIVER_NOT_EXPORTED,
                )
                registeredInteractiveState = true
            }
        } else {
            if (registeredInteractiveState) {
                Log.i(TAG, "Stopping listening for interactive state changes")
                unregisterReceiver(interactiveStateReceiver)
                registeredInteractiveState = false
                lastInteractive = false
            }
        }

        val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        if (prefs.getBoolean("disable_on_call", false)) {
            if (tm != null) {
                Log.i(TAG, "Starting listening for call states")
                telephonyMonitor.setCallStateListening(tm, enabled = true)
            }
        } else {
            if (tm != null) {
                Log.i(TAG, "Stopping listening for call states")
                telephonyMonitor.setCallStateListening(tm, enabled = false)
            }
        }

        if (cmd == Command.start || cmd == Command.reload || cmd == Command.stop) {
            val watchdog = prefs.getString("watchdog", "0")?.toIntOrNull() ?: 0
            val enabled = prefs.getBoolean("enabled", false)
            WorkScheduler.scheduleWatchdog(
                this@ServiceSinkhole,
                watchdog,
                enabled && cmd != Command.stop,
            )
        }

        try {
            when (cmd) {
                Command.run -> Unit
                Command.start -> start()
                Command.reload -> reload(intent.getBooleanExtra(EXTRA_INTERACTIVE, false))
                Command.stop -> stop(temporarilyStopped)
                Command.stats -> {
                    statsHandler.restart()
                }

                Command.householding -> householding(intent)
                Command.watchdog -> watchdog(intent)
                Command.updatecheck -> scheduleUpdateCheck(reportResult = true)
                else -> Log.e(TAG, "Unknown command=$cmd")
            }

            if (cmd == Command.start || cmd == Command.reload) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val filter = prefs.getBoolean("filter", false)
                    if (filter && isLockdownEnabled()) {
                        showLockdownNotification()
                    } else {
                        removeLockdownNotification()
                    }
                }
            }

            if (cmd == Command.start || cmd == Command.reload || cmd == Command.stop) {
                val ruleset = Intent(ActivityMain.ACTION_RULES_CHANGED).setPackage(packageName)
                ruleset.putExtra(
                    ActivityMain.EXTRA_CONNECTED,
                    cmd != Command.stop && lastConnected,
                )
                ruleset.putExtra(ActivityMain.EXTRA_METERED, cmd != Command.stop && lastMetered)
                sendBroadcast(ruleset)

                Widgets.updateFirewall(this@ServiceSinkhole)
            }

            if (
                !commandHandler.hasMessages(Command.start.ordinal) &&
                    !commandHandler.hasMessages(Command.reload.ordinal) &&
                    !prefs.getBoolean("enabled", false) &&
                    !prefs.getBoolean("show_stats", false)
            ) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
        } catch (ex: Throwable) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))

            if (cmd == Command.start || cmd == Command.reload) {
                if (VpnService.prepare(this@ServiceSinkhole) == null) {
                    Log.w(TAG, "VPN prepared connected=$lastConnected")
                    if (lastConnected && ex !is StartFailedException) {
                        if (!Util.isPlayStoreInstall(this@ServiceSinkhole)) {
                            showErrorNotification(ex.toString())
                        }
                    }
                } else {
                    showErrorNotification(ex.toString())

                    if (ex !is StartFailedException) {
                        prefs.putBoolean("enabled", false)
                        Widgets.updateFirewall(this@ServiceSinkhole)
                    }
                }
            } else {
                showErrorNotification(ex.toString())
            }
        }
    }

    private fun start() {
        if (vpn == null) {
            if (state != State.none) {
                Log.d(TAG, "Stop foreground state=$state")
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
            startForeground(NOTIFY_ENFORCING, foregroundNotifications.enforcing())
            state = State.enforcing
            Log.d(TAG, "Start foreground state=$state")

            val listRule = Rule.getRules(true, this@ServiceSinkhole)
            val listAllowed = getAllowedRules(listRule)

            lastBuilder = getBuilder(listAllowed, listRule)
            vpn = startVPN(lastBuilder!!)
            if (vpn == null) {
                throw StartFailedException(getString((R.string.msg_start_failed)))
            }

            startNative(vpn!!, listAllowed, listRule)

            removeWarningNotifications()
            foregroundNotifications.updateEnforcing(
                listAllowed.size,
                listRule.size,
                domainLists.blockedCount(),
            )
        }
    }

    private fun reload(interactive: Boolean) {
        val listRule = Rule.getRules(true, this@ServiceSinkhole)

        if (interactive) {
            var process = false
            for (rule in listRule) {
                val blocked = if (lastMetered) rule.other_blocked else rule.wifi_blocked
                val screen = if (lastMetered) rule.screen_other else rule.screen_wifi
                if (blocked && screen) {
                    process = true
                    break
                }
            }
            if (!process) {
                Log.i(TAG, "No changed rules on interactive state change")
                return
            }
        }

        if (state != State.enforcing) {
            if (state != State.none) {
                Log.d(TAG, "Stop foreground state=$state")
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
            startForeground(NOTIFY_ENFORCING, foregroundNotifications.enforcing())
            state = State.enforcing
            Log.d(TAG, "Start foreground state=$state")
        }

        val listAllowed = getAllowedRules(listRule)
        val builder = getBuilder(listAllowed, listRule)

        if (vpn != null && prefs.getBoolean("filter", false) && builder == lastBuilder) {
            Log.i(TAG, "Native restart")
            stopNative()
        } else {
            lastBuilder = builder
            vpn?.let {
                stopNative()
                stopVPN(it)
            }
            vpn = startVPN(builder)
        }

        if (vpn == null) {
            throw StartFailedException(getString((R.string.msg_start_failed)))
        }

        startNative(vpn!!, listAllowed, listRule)

        removeWarningNotifications()
        foregroundNotifications.updateEnforcing(
            listAllowed.size,
            listRule.size,
            domainLists.blockedCount(),
        )
    }

    private fun stop(temporary: Boolean) {
        if (vpn != null) {
            stopNative()
            stopVPN(vpn!!)
            vpn = null
            unprepare()
        }
        if (state == State.enforcing && !temporary) {
            Log.d(TAG, "Stop foreground state=$state")
            foregroundNotifications.resetCounts()

            stopForeground(STOP_FOREGROUND_REMOVE)

            if (prefs.getBoolean("show_stats", false)) {
                startForeground(NOTIFY_WAITING, foregroundNotifications.waiting())
                state = State.waiting
                Log.d(TAG, "Start foreground state=$state")
            } else {
                state = State.none
                stopSelf()
            }
        }
    }

    private fun householding(intent: Intent) {
        val retentionDays =
            (prefs.getString("log_retention_days", "3")?.toIntOrNull() ?: 3).coerceIn(0, 365)
        if (retentionDays > 0) {
            val cutoffTime = Date().time - retentionDays * 24L * 3600L * 1000L
            AndroidFirewallDatabase.getInstance(this@ServiceSinkhole).cleanupLog(cutoffTime)
        } else {
            Log.i(TAG, "Log cleanup disabled by preference")
        }

        AndroidFirewallDatabase.getInstance(this@ServiceSinkhole).cleanupDns()

        if (
            !Util.isPlayStoreInstall(this@ServiceSinkhole) &&
                prefs.getBoolean(PreferenceKeys.UPDATE_CHECK, PreferenceKeys.UPDATE_CHECK_DEFAULT)
        ) {
            scheduleUpdateCheck(reportResult = false)
        }
    }

    private fun watchdog(intent: Intent) {
        if (vpn == null) {
            if (prefs.getBoolean("enabled", false)) {
                Log.e(TAG, "Service was killed")
                start()
            }
        }
    }

    private fun checkUpdateResult(result: UpdateCheckResult) {
        val resultIntent = Intent(ACTION_UPDATE_CHECK_RESULT).setPackage(packageName)
        resultIntent.putExtra(EXTRA_UPDATE_CHECK_STATUS, result.status.name)
        result.availableVersion?.let { resultIntent.putExtra(EXTRA_UPDATE_CHECK_VERSION, it) }
        sendBroadcast(resultIntent)
    }

    private fun scheduleUpdateCheck(reportResult: Boolean) {
        updateExecutor.execute {
            val result =
                ReleaseUpdateChecker.check(
                    apiUrl = BuildConfig.GITHUB_LATEST_API,
                    currentVersion = Util.getSelfVersionName(this),
                )
            Handler(mainLooper).post {
                if (destroying) return@post
                if (result.status == UpdateCheckStatus.available) {
                    notifications.showUpdate(
                        requireNotNull(result.assetName),
                        requireNotNull(result.releaseUrl),
                    )
                }
                if (reportResult) checkUpdateResult(result)
            }
        }
    }

    private data class ForwardKey(val protocol: Int, val port: Int)

    private class StartFailedException(msg: String) : IllegalStateException(msg)

    private fun onStatsStarted() {
        statsWhenMs = Date().time
        trafficStatistics.reset()
    }

    private fun onStatsStopped() {
        if (state == State.stats) {
            Log.d(TAG, "Stop foreground state=$state")
            stopForeground(STOP_FOREGROUND_REMOVE)
            state = State.none
        } else {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFY_TRAFFIC)
        }
    }

    private fun renderTrafficStats(): Long? {
        if (destroying) return null

        val frequency =
            (prefs.getString("stats_frequency", "1000")?.toLongOrNull() ?: 1000).coerceIn(
                MIN_STATS_FREQUENCY_MS,
                MAX_STATS_FREQUENCY_MS,
            )
        val samples =
            (prefs.getString("stats_samples", "90")?.toLongOrNull() ?: 90).coerceIn(
                MIN_STATS_SAMPLES,
                MAX_STATS_SAMPLES,
            )
        val filter = prefs.getBoolean("filter", false)
        val showTop = prefs.getBoolean("show_top", false)

        val ct = SystemClock.elapsedRealtime()

        var ttx = TrafficStats.getTotalTxBytes().coerceAtLeast(0)
        var trx = TrafficStats.getTotalRxBytes().coerceAtLeast(0)
        if (filter) {
            ttx =
                (ttx - TrafficStats.getUidTxBytes(Process.myUid()).coerceAtLeast(0)).coerceAtLeast(
                    0
                )
            trx =
                (trx - TrafficStats.getUidRxBytes(Process.myUid()).coerceAtLeast(0)).coerceAtLeast(
                    0
                )
        }
        val rates = trafficStatistics.sample(ct, ttx, trx, samples)
        val txsec = rates.transmitPerSecond
        val rxsec = rates.receivePerSecond

        var topText = ""
        if (showTop) {
            val uidBytes =
                packageManager
                    .getInstalledApplications(0)
                    .asSequence()
                    .map { it.uid }
                    .filter { it != Process.myUid() }
                    .distinct()
                    .associateWith { uid ->
                        TrafficStats.getUidTxBytes(uid).coerceAtLeast(0) +
                            TrafficStats.getUidRxBytes(uid).coerceAtLeast(0)
                    }
            if (!trafficStatistics.hasUidBaseline()) {
                trafficStatistics.initializeUidBytes(uidBytes)
            } else {

                val sb = StringBuilder()
                var i = 0
                for ((uid, speed) in trafficStatistics.uidRates(uidBytes)) {
                    if (i++ >= 3) break
                    if (speed < 1000 * 1000) {
                        sb.append(getString(R.string.msg_kbsec, speed / 1000))
                    } else {
                        sb.append(getString(R.string.msg_mbsec, speed / 1000 / 1000))
                    }
                    sb.append(' ')
                    val apps = Util.getApplicationNames(uid, this@ServiceSinkhole)
                    sb.append(if (apps.isNotEmpty()) apps[0] else "?")
                    sb.append("\r\n")
                }
                if (sb.isNotEmpty()) {
                    sb.setLength(sb.length - 2)
                    topText = sb.toString()
                }
            }
        } else {
            trafficStatistics.clearUidBaseline()
        }

        val graph = renderTrafficGraph(this, trafficStatistics.points(), samples)

        val txText =
            if (txsec < 1000 * 1000) {
                getString(R.string.msg_kbsec, txsec / 1000)
            } else {
                getString(R.string.msg_mbsec, txsec / 1000 / 1000)
            }
        val rxText =
            if (rxsec < 1000 * 1000) {
                getString(R.string.msg_kbsec, rxsec / 1000)
            } else {
                getString(R.string.msg_mbsec, rxsec / 1000 / 1000)
            }
        val maxText =
            if (graph.peak < 1000 * 1000) {
                getString(R.string.msg_kbsec, graph.peak / 2 / 1000)
            } else {
                getString(R.string.msg_mbsec, graph.peak / 2 / 1000 / 1000)
            }
        val statsSummary =
            getString(
                R.string.notify_traffic_summary,
                txText,
                rxText,
                maxText,
            )
        val debugText =
            if (BuildConfig.DEBUG) {
                val count = nativeEngine.stats()
                getString(
                    R.string.notify_traffic_debug,
                    count.icmpSessions,
                    count.udpSessions,
                    count.tcpSessions,
                    count.openFileDescriptors,
                    count.fileDescriptorLimit,
                )
            } else {
                ""
            }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val extraLines = buildList {
            if (topText.isNotBlank()) {
                addAll(topText.split("\r\n"))
            }
            if (debugText.isNotBlank()) {
                add(debugText)
            }
        }
        val notification =
            trafficNotifications.build(
                TrafficNotificationContent(statsWhenMs, graph.bitmap, statsSummary, extraLines)
            )

        if (state == State.none || state == State.waiting) {
            if (state != State.none) {
                Log.d(TAG, "Stop foreground state=$state")
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
            startForeground(NOTIFY_TRAFFIC, notification)
            state = State.stats
            Log.d(TAG, "Start foreground state=$state")
        } else {
            if (Util.canNotify(this@ServiceSinkhole)) {
                notificationManager.notify(NOTIFY_TRAFFIC, notification)
            }
        }
        return frequency
    }

    private fun startVPN(builder: TrackingVpnBuilder): ParcelFileDescriptor? {
        return try {
            val pfd = builder.establish()

            pfd
        } catch (ex: SecurityException) {
            throw ex
        } catch (ex: Throwable) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            null
        }
    }

    private fun getBuilder(listAllowed: List<Rule>, listRule: List<Rule>): TrackingVpnBuilder {
        val subnet = prefs.getBoolean("subnet", false)
        val tethering = prefs.getBoolean("tethering", false)
        val lan = prefs.getBoolean("lan", false)
        val ip6 = prefs.getBoolean("ip6", true)
        val filter = prefs.getBoolean("filter", false)
        val system = prefs.getBoolean("manage_system", false)

        val builder = TrackingVpnBuilder()
        builder.setSession(getString(R.string.app_name))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(Util.isMeteredNetwork(this))
        }

        val vpn4 = prefs.getString("vpn4", "10.1.10.1") ?: "10.1.10.1"
        Log.i(TAG, "Using VPN4=$vpn4")
        builder.addAddress(vpn4, 32)
        if (ip6) {
            val vpn6 =
                prefs.getString("vpn6", "fd00:1:fd00:1:fd00:1:fd00:1")
                    ?: "fd00:1:fd00:1:fd00:1:fd00:1"
            Log.i(TAG, "Using VPN6=$vpn6")
            builder.addAddress(vpn6, 128)
        }

        if (filter) {
            for (dns in getDns(this@ServiceSinkhole)) {
                if (ip6 || dns is Inet4Address) {
                    Log.i(TAG, "Using DNS=$dns")
                    builder.addDnsServer(dns)
                }
            }
        }

        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val active = cm.activeNetwork
            val props = if (active == null) null else cm.getLinkProperties(active)
            val domain = props?.domains
            if (domain != null) {
                Log.i(TAG, "Using search domain=$domain")
                builder.addSearchDomain(domain)
            }
        } catch (ex: Throwable) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
        }

        if (subnet) {
            val listExclude = ArrayList<IPUtil.CIDR>()
            listExclude.add(IPUtil.CIDR("127.0.0.0", 8))

            if (tethering && !lan) {
                listExclude.add(IPUtil.CIDR("192.168.42.0", 23))
                listExclude.add(IPUtil.CIDR("192.168.44.0", 24))
                listExclude.add(IPUtil.CIDR("192.168.49.0", 24))

                try {
                    val nis = NetworkInterface.getNetworkInterfaces()
                    if (nis != null) {
                        while (nis.hasMoreElements()) {
                            val ni = nis.nextElement()
                            if (
                                ni != null &&
                                    !ni.isLoopback &&
                                    ni.isUp &&
                                    ni.name != null &&
                                    ni.name.startsWith("ap_br_wlan")
                            ) {
                                val ias = ni.interfaceAddresses
                                if (ias != null) {
                                    for (ia in ias) {
                                        if (ia.address is Inet4Address) {
                                            val host = ia.address.hostAddress
                                            if (host != null) {
                                                listExclude.add(IPUtil.CIDR(host, 24))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (ex: Throwable) {
                    Log.e(TAG, ex.toString())
                }
            }

            if (lan) {
                listExclude.add(IPUtil.CIDR("10.0.0.0", 8))
                listExclude.add(IPUtil.CIDR("172.16.0.0", 12))
                listExclude.add(IPUtil.CIDR("192.168.0.0", 16))
            }

            if (!filter) {
                for (dns in getDns(this@ServiceSinkhole)) {
                    if (dns is Inet4Address) {
                        val host = dns.hostAddress
                        if (host != null) {
                            listExclude.add(IPUtil.CIDR(host, 32))
                        }
                    }
                }

                val dnsSpecifier = Util.getPrivateDnsSpecifier(this@ServiceSinkhole)
                if (!dnsSpecifier.isNullOrEmpty()) {
                    try {
                        Log.i(TAG, "Resolving private dns=$dnsSpecifier")
                        for (pdns in InetAddress.getAllByName(dnsSpecifier)) {
                            if (pdns is Inet4Address) {
                                val host = pdns.hostAddress
                                if (host != null) {
                                    listExclude.add(IPUtil.CIDR(host, 32))
                                }
                            }
                        }
                    } catch (ex: Throwable) {
                        Log.e(TAG, ex.toString())
                    }
                }
            }

            listExclude.addAll(carrierRouteExclusions(resources.configuration))

            listExclude.add(IPUtil.CIDR("224.0.0.0", 3))

            listExclude.sort()

            try {
                var start = InetAddress.getByName("0.0.0.0")
                for (exclude in listExclude) {
                    val excludeStart = exclude.getStart() ?: continue
                    val excludeEnd = exclude.getEnd() ?: continue
                    Log.i(
                        TAG,
                        "Exclude " + excludeStart.hostAddress + "..." + excludeEnd.hostAddress,
                    )
                    val before = IPUtil.minus1(excludeStart) ?: continue
                    for (include in IPUtil.toCIDR(start, before)) {
                        val address = include.address ?: continue
                        try {
                            builder.addRoute(address, include.prefix)
                        } catch (ex: Throwable) {
                            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                        }
                    }
                    start = IPUtil.plus1(excludeEnd) ?: start
                }
                val end = if (lan) "255.255.255.254" else "255.255.255.255"
                for (include in IPUtil.toCIDR("224.0.0.0", end)) {
                    val address = include.address ?: continue
                    try {
                        builder.addRoute(address, include.prefix)
                    } catch (ex: Throwable) {
                        Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                    }
                }
            } catch (ex: UnknownHostException) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }
        } else {
            builder.addRoute("0.0.0.0", 0)
        }

        Log.i(TAG, "IPv6=$ip6")
        if (ip6) {
            builder.addRoute("2000::", 3)
        }

        val mtu = capabilities().mtu.toInt()
        Log.i(TAG, "MTU=$mtu")
        builder.setMtu(mtu)

        if (lastConnected && !filter) {
            val mapDisallowed = HashMap<String, Rule>()
            for (rule in listRule) {
                rule.packageName?.let { mapDisallowed[it] = rule }
            }
            for (rule in listAllowed) {
                rule.packageName?.let { mapDisallowed.remove(it) }
            }
            for (packageName in mapDisallowed.keys) {
                try {
                    builder.addAllowedApplication(packageName)
                    Log.i(TAG, "Sinkhole $packageName")
                } catch (ex: PackageManager.NameNotFoundException) {
                    Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                }
            }
            if (mapDisallowed.isEmpty()) {
                try {
                    builder.addAllowedApplication(packageName)
                } catch (ex: PackageManager.NameNotFoundException) {
                    Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                }
            }
        } else if (filter) {
            try {
                builder.addDisallowedApplication(packageName)
            } catch (ex: PackageManager.NameNotFoundException) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }
            for (rule in listRule) {
                if (!rule.apply || (!system && rule.system)) {
                    try {
                        Log.i(TAG, "Not routing " + rule.packageName)
                        rule.packageName?.let { builder.addDisallowedApplication(it) }
                    } catch (ex: PackageManager.NameNotFoundException) {
                        Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                    }
                }
            }
        }

        val configure = Intent(this, ActivityMain::class.java)
        val pi =
            PendingIntentCompat.getActivity(this, 0, configure, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setConfigureIntent(pi)

        return builder
    }

    private fun startNative(
        vpn: ParcelFileDescriptor,
        listAllowed: List<Rule>,
        listRule: List<Rule>,
    ) {
        val log = prefs.getBoolean("log", false)
        val filter = prefs.getBoolean("filter", false)

        Log.i(TAG, "Start native log=$log filter=$filter")

        if (filter) {
            prepareUidAllowed(listAllowed, listRule)
            prepareHostsBlocked()
            prepareMalwareList()
            prepareUidIPFilters(null)
            prepareForwarding()
        } else {
            withPolicyWrite {
                allowedUids.clear()
                knownUids.clear()
                domainLists.clear()
                mapUidIPFilters.clear()
                mapForward.clear()
            }
        }

        if (log) {
            prepareNotify(listRule)
        } else {
            withPolicyWrite { mapNotify.clear() }
        }

        if (log || filter) {
            val rcode = prefs.getString("rcode", "3")?.toIntOrNull() ?: 3
            if (prefs.getBoolean("socks5_enabled", false)) {
                nativeEngine.configureSocks5(
                    NativeSocks5Config(
                        prefs.getString("socks5_addr", "") ?: "",
                        prefs.getString("socks5_port", "0")?.toIntOrNull() ?: 0,
                        prefs.getString("socks5_username", "") ?: "",
                        prefs.getString("socks5_password", "") ?: "",
                    )
                )
            } else {
                nativeEngine.configureSocks5(null)
            }

            Log.i(TAG, "Starting UniFFI tunnel engine")
            nativeEngine.start(
                vpn.fd,
                withPolicyRead { ForwardKey(17, 53) in mapForward },
                rcode,
            )
            Log.i(TAG, "Started tunnel thread")
        }
    }

    private fun stopNative() {
        Log.i(TAG, "Stop native")

        nativeEngine.stop()
        Log.i(TAG, "Stopped tunnel thread")
    }

    private fun unprepare() {
        withPolicyWrite {
            allowedUids.clear()
            knownUids.clear()
            mapUidIPFilters.clear()
            mapForward.clear()
            mapNotify.clear()
        }
        domainLists.clear()
    }

    private fun prepareUidAllowed(listAllowed: List<Rule>, listRule: List<Rule>) {
        withPolicyWrite {
            allowedUids.clear()
            allowedUids.addAll(listAllowed.mapTo(HashSet()) { it.uid })
            knownUids.clear()
            knownUids.addAll(listRule.mapTo(HashSet()) { it.uid })
        }
    }

    private fun prepareHostsBlocked() = domainLists.refreshHosts()

    private fun prepareMalwareList() = domainLists.refreshMalware()

    private fun prepareUidIPFilters(dname: String?) {
        withPolicyWrite {
            if (dname == null) {
                mapUidIPFilters.clear()
            }

            val rows = AndroidFirewallDatabase.getInstance(this@ServiceSinkhole).getAccessDns(dname)
            for (row in rows) {
                val uid = row.uid
                val version = row.version
                val protocol = row.protocol
                val daddr = row.daddr
                val dresource = row.resource
                val dport = row.dport
                val block = row.block > 0
                val time = row.time ?: System.currentTimeMillis()
                val ttl = row.ttl ?: 7 * 24 * 3600 * 1000L

                if (isLockedDown(lastMetered)) {
                    val pkg = packageManager.getPackagesForUid(uid)
                    if (pkg != null && pkg.isNotEmpty()) {
                        if (
                            !prefs.getBoolean(
                                PreferencesRepository.namespaced("lockdown", pkg[0]),
                                false,
                            )
                        ) {
                            continue
                        }
                    }
                }

                val key = IpRuleKey(version, protocol, dport, uid)
                if (!mapUidIPFilters.containsKey(key)) {
                    mapUidIPFilters[key] = HashMap()
                }

                try {
                    val name = if (dresource == null) daddr else dresource
                    if (Util.isNumericAddress(name)) {
                        val iname = InetAddress.getByName(name)
                        if (version == 4 && iname !is Inet4Address) {
                            continue
                        }
                        if (version == 6 && iname !is Inet6Address) {
                            continue
                        }

                        val exists = mapUidIPFilters[key]?.containsKey(iname) == true
                        val currentRule = mapUidIPFilters[key]?.get(iname)
                        if (!exists || currentRule?.isBlocked() == false) {
                            val rule = IpRule(key, "$name/$iname", block, time, ttl)
                            mapUidIPFilters[key]?.put(iname, rule)
                            if (exists) {
                                Log.w(TAG, "Address conflict $key $daddr/$dresource")
                            }
                        } else if (exists) {
                            currentRule?.refresh(time, ttl)
                            if (dname != null && ttl > 60 * 1000L) {
                                Log.w(TAG, "Address updated $key $daddr/$dresource")
                            }
                        } else {
                            if (dname != null) {
                                Log.i(TAG, "Ignored $key $daddr/$dresource=$block")
                            }
                        }
                    } else {
                        Log.w(TAG, "Address not numeric $name")
                    }
                } catch (ex: UnknownHostException) {
                    Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                }
            }
        }
    }

    private fun prepareForwarding() {
        withPolicyWrite {
            mapForward.clear()
            if (prefs.getBoolean("filter", false)) {
                for (forward in
                    AndroidFirewallDatabase.getInstance(this@ServiceSinkhole).getForwarding()) {
                    mapForward[ForwardKey(forward.protocol, forward.dport)] = forward
                    Log.i(TAG, "Forward $forward")
                }
            }
        }
    }

    private fun prepareNotify(listRule: List<Rule>) {
        val notify = prefs.getBoolean("notify_access", false)
        val system = prefs.getBoolean("manage_system", false)

        withPolicyWrite {
            mapNotify.clear()
            for (rule in listRule) {
                mapNotify[rule.uid] = notify && rule.notify && (system || !rule.system)
            }
        }
    }

    private fun isLockedDown(metered: Boolean): Boolean {
        var lockdown = prefs.getBoolean("lockdown", false)
        val lockdownWifi = prefs.getBoolean("lockdown_wifi", true)
        val lockdownOther = prefs.getBoolean("lockdown_other", true)
        if (metered) {
            if (!lockdownOther) lockdown = false
        } else {
            if (!lockdownWifi) lockdown = false
        }
        return lockdown
    }

    private fun getAllowedRules(listRule: List<Rule>): List<Rule> {
        val listAllowed = ArrayList<Rule>()

        val wifi = Util.isWifiActive(this)
        var metered = Util.isMeteredNetwork(this)
        val useMetered = prefs.getBoolean("use_metered", false)
        val ssidHomes = prefs.getStringSet("wifi_homes", emptySet()).toMutableSet()
        val ssidNetwork = Util.getWifiSSID(this)
        val generation = Util.getNetworkGeneration(this)
        val unmetered2g = prefs.getBoolean("unmetered_2g", false)
        val unmetered3g = prefs.getBoolean("unmetered_3g", false)
        val unmetered4g = prefs.getBoolean("unmetered_4g", false)
        var roaming = Util.isRoaming(this@ServiceSinkhole)
        val national = prefs.getBoolean("national_roaming", false)
        val eu = prefs.getBoolean("eu_roaming", false)
        val tethering = prefs.getBoolean("tethering", false)
        val filter = prefs.getBoolean("filter", false)

        lastConnected = Util.isConnected(this@ServiceSinkhole)

        val orgMetered = metered
        val orgRoaming = roaming

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            ssidHomes.clear()
        }

        if (wifi && !useMetered) {
            metered = false
        }
        if (
            wifi &&
                ssidHomes.isNotEmpty() &&
                !(ssidHomes.contains(ssidNetwork) || ssidHomes.contains('"' + ssidNetwork + '"'))
        ) {
            metered = true
            Log.i(TAG, "!@home=$ssidNetwork homes=" + TextUtils.join(",", ssidHomes))
        }
        if (unmetered2g && "2G" == generation) metered = false
        if (unmetered3g && "3G" == generation) metered = false
        if (unmetered4g && "4G" == generation) metered = false
        lastMetered = metered

        val lockdown = isLockedDown(lastMetered)

        if (roaming && eu) roaming = !Util.isEU(this)
        if (roaming && national) roaming = !Util.isNational(this)

        Log.i(
            TAG,
            "Get allowed" +
                " connected=" +
                lastConnected +
                " wifi=" +
                wifi +
                " home=" +
                TextUtils.join(",", ssidHomes) +
                " network=" +
                ssidNetwork +
                " metered=" +
                metered +
                "/" +
                orgMetered +
                " generation=" +
                generation +
                " roaming=" +
                roaming +
                "/" +
                orgRoaming +
                " interactive=" +
                lastInteractive +
                " tethering=" +
                tethering +
                " filter=" +
                filter +
                " lockdown=" +
                lockdown,
        )

        if (lastConnected) {
            for (rule in listRule) {
                val blocked = if (metered) rule.other_blocked else rule.wifi_blocked
                val screen = if (metered) rule.screen_other else rule.screen_wifi
                if (
                    (!blocked || (screen && lastInteractive)) &&
                        (!metered || !(rule.roaming && roaming)) &&
                        (!lockdown || rule.lockdown)
                ) {
                    listAllowed.add(rule)
                }
            }
        }

        Log.i(TAG, "Allowed ${listAllowed.size} of ${listRule.size}")
        return listAllowed
    }

    private fun stopVPN(pfd: ParcelFileDescriptor) {
        Log.i(TAG, "Stopping")
        try {
            pfd.close()
        } catch (ex: IOException) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
        }
    }

    private fun nativeExit(reason: String?) {
        Log.w(TAG, "Native exit reason=$reason")
        if (reason != null) {
            showErrorNotification(reason)

            prefs.putBoolean("enabled", false)
            Widgets.updateFirewall(this)
        }
    }

    private fun logPacket(packet: Packet) {
        val connection = if (lastConnected) if (lastMetered) 2 else 1 else 0
        logHandler.queue(packet, connection, lastInteractive)
    }

    private fun dnsResolved(rr: ResourceRecord) {
        if (AndroidFirewallDatabase.getInstance(this@ServiceSinkhole).insertDns(rr)) {
            Log.i(TAG, "New IP $rr")
            prepareUidIPFilters(rr.QName)
        }
        if (rr.uid > 0 && !TextUtils.isEmpty(rr.AName)) {
            val malware = domainLists.isMalware(requireNotNull(rr.AName))

            if (malware) {
                val notified = prefs.getBoolean("malware.${rr.uid}", false)
                if (!notified) {
                    prefs.putBoolean("malware.${rr.uid}", true)
                    notifications.showNewApplication(rr.uid, true)
                }
            }
        }
    }

    private fun isDomainBlocked(name: String): Boolean {
        return domainLists.isBlocked(name)
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private fun getUidQ(
        version: Int,
        protocol: Int,
        saddr: String,
        sport: Int,
        daddr: String,
        dport: Int,
    ): Int {
        if (protocol != 6 && protocol != 17) {
            return Process.INVALID_UID
        }

        val cm =
            getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager?
                ?: return Process.INVALID_UID

        val local = InetSocketAddress(saddr, sport)
        val remote = InetSocketAddress(daddr, dport)

        Log.i(TAG, "Get uid local=$local remote=$remote")
        val uid = cm.getConnectionOwnerUid(protocol, local, remote)
        Log.i(TAG, "Get uid=$uid")
        return uid
    }

    private fun isSupported(protocol: Int): Boolean {
        return protocol == 1 || protocol == 58 || protocol == 6 || protocol == 17
    }

    private fun isAddressAllowed(packet: Packet): Allowed? {
        val allowed = withPolicyRead {
            packet.allowed = false
            if (prefs.getBoolean("filter", false)) {
                if (packet.protocol == 17 && !prefs.getBoolean(PreferenceKeys.FILTER_UDP)) {
                    packet.allowed = true
                    Log.i(TAG, "Allowing UDP $packet")
                } else if (
                    (packet.uid < 2000 || BuildConfig.PLAY_STORE_RELEASE) &&
                        packet.uid !in knownUids &&
                        isSupported(packet.protocol)
                ) {
                    packet.allowed = true
                    Log.w(TAG, "Allowing unknown system $packet")
                } else if (packet.uid == Process.myUid()) {
                    packet.allowed = true
                    Log.w(TAG, "Allowing self $packet")
                } else {
                    var filtered = false
                    val key = IpRuleKey(packet.version, packet.protocol, packet.dport, packet.uid)
                    if (mapUidIPFilters.containsKey(key)) {
                        try {
                            val daddr = packet.daddr
                            if (daddr == null) {
                                return@withPolicyRead null
                            }
                            val iaddr = InetAddress.getByName(daddr)
                            val map = mapUidIPFilters[key]
                            if (map != null && map.containsKey(iaddr)) {
                                val rule = map[iaddr]
                                if (rule != null && rule.isExpired()) {
                                    Log.i(TAG, "DNS expired $packet rule $rule")
                                } else if (rule != null) {
                                    filtered = true
                                    packet.allowed = !rule.isBlocked()
                                    Log.i(
                                        TAG,
                                        "Filtering $packet allowed=${packet.allowed} rule $rule",
                                    )
                                }
                            }
                        } catch (ex: UnknownHostException) {
                            Log.w(TAG, "Allowed $ex\n" + Log.getStackTraceString(ex))
                        }
                    }

                    if (!filtered) {
                        if (packet.uid in allowedUids) {
                            packet.allowed = true
                        } else {
                            Log.w(TAG, "No rules for $packet")
                        }
                    }
                }
            }

            val redirect =
                if (packet.allowed) {
                    val forward = mapForward[ForwardKey(packet.protocol, packet.dport)]
                    if (forward == null || forward.ruid == packet.uid) {
                        Allowed()
                    } else {
                        packet.data = "> ${forward.raddr}/${forward.rport}"
                        Allowed(forward.raddr, forward.rport)
                    }
                } else {
                    null
                }

            redirect
        }

        if (prefs.getBoolean("log", false)) {
            if (packet.protocol != 6 || packet.flags != "") {
                if (packet.uid != Process.myUid()) {
                    logPacket(packet)
                }
            }
        }

        return allowed
    }

    private fun accountUsage(usage: Usage) {
        logHandler.account(usage)
    }

    private val interactiveStateReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.i(TAG, "Received $intent")
                Util.logExtras(intent)

                if (destroying || executor.isShutdown) return
                try {
                    executor.execute {
                        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        val i = Intent(ACTION_SCREEN_OFF_DELAYED)
                        i.setPackage(context.packageName)
                        val pi =
                            PendingIntentCompat.getBroadcast(
                                context,
                                0,
                                i,
                                PendingIntent.FLAG_UPDATE_CURRENT,
                            )
                        am.cancel(pi)

                        try {
                            val delay = prefs.getString("screen_delay", "0")?.toIntOrNull() ?: 0
                            val interactive = Intent.ACTION_SCREEN_ON == intent.action

                            if (interactive || delay == 0) {
                                lastInteractive = interactive
                                reload("interactive state changed", this@ServiceSinkhole, true)
                            } else {
                                if (ACTION_SCREEN_OFF_DELAYED == intent.action) {
                                    lastInteractive = interactive
                                    reload("interactive state changed", this@ServiceSinkhole, true)
                                } else {
                                    am.setAndAllowWhileIdle(
                                        AlarmManager.RTC_WAKEUP,
                                        Date().time + delay * 60 * 1000L,
                                        pi,
                                    )
                                }
                            }

                            if (Util.isInteractive(this@ServiceSinkhole)) statsHandler.start()
                            else statsHandler.stop()
                        } catch (ex: Throwable) {
                            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))

                            am.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                Date().time + 15 * 1000L,
                                pi,
                            )
                        }
                    }
                } catch (_: RejectedExecutionException) {
                    Log.i(TAG, "Ignoring interactive change after executor shutdown")
                }
            }
        }

    private val userReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.i(TAG, "Received $intent")
                Util.logExtras(intent)

                userForeground = Intent.ACTION_USER_FOREGROUND == intent.action
                Log.i(TAG, "User foreground=$userForeground user=" + (Process.myUid() / 100000))

                if (userForeground) {
                    if (prefs.getBoolean("enabled", false) && !destroying) {
                        try {
                            executor.execute {
                                try {
                                    Thread.sleep(USER_FOREGROUND_DELAY_MS)
                                } catch (_: InterruptedException) {
                                    Thread.currentThread().interrupt()
                                    return@execute
                                }
                                if (!destroying && prefs.getBoolean("enabled", false)) {
                                    start("foreground", this@ServiceSinkhole)
                                }
                            }
                        } catch (_: RejectedExecutionException) {
                            Log.i(TAG, "Ignoring user foreground after executor shutdown")
                        }
                    }
                } else {
                    stop("background", this@ServiceSinkhole, true)
                }
            }
        }

    private val idleStateReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.i(TAG, "Received $intent")
                Util.logExtras(intent)

                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                Log.i(TAG, "device idle=" + pm.isDeviceIdleMode)

                if (!pm.isDeviceIdleMode) {
                    reload("idle state changed", this@ServiceSinkhole, false)
                }
            }
        }

    private val apStateReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.i(TAG, "Received $intent")
                Util.logExtras(intent)
                reload("AP state changed", this@ServiceSinkhole, false)
            }
        }

    // Built on first registration, after the service has a Context; preferences are read per
    // broadcast. Reading them while the service object is constructed throws, because
    // applicationContext is still null then.
    private val packageChangedReceiver by lazy {
        SinkholePackageChangeReceiver(
            preferences = { prefs },
            onNewApplication = { uid -> notifications.showNewApplication(uid, false) },
            onReload = { reason, context -> reload(reason, context, false) },
        )
    }

    override fun onCreate() {
        destroying = false
        Log.i(
            TAG,
            "Create version=" + Util.getSelfVersionName(this) + "/" + Util.getSelfVersionCode(this),
        )
        startForeground(NOTIFY_WAITING, foregroundNotifications.waiting())

        nativeEngine = NativeVpnEngineController(Build.VERSION.SDK_INT, nativeCallbacks)
        Log.i(TAG, "Created UniFFI native engine")
        val pcap = prefs.getBoolean("pcap", false)
        setPcap(pcap, this, nativeEngine)

        removePrefsListener = prefs.addListener { key ->
            if (!destroying) onPreferenceChanged(key)
        }

        Util.setTheme(this)
        super.onCreate()

        val commandThread =
            HandlerThread(
                getString(R.string.app_name) + " command",
                Process.THREAD_PRIORITY_FOREGROUND,
            )
        val logThread =
            HandlerThread(getString(R.string.app_name) + " log", Process.THREAD_PRIORITY_BACKGROUND)
        val statsThread =
            HandlerThread(
                getString(R.string.app_name) + " stats",
                Process.THREAD_PRIORITY_BACKGROUND,
            )
        commandThread.start()
        logThread.start()
        statsThread.start()

        commandLooper = commandThread.looper
        logLooper = logThread.looper
        statsLooper = statsThread.looper

        commandHandler =
            SinkholeCommandHandler(
                looper = commandLooper,
                commandCode = { intent -> getCommandExtra(intent)?.ordinal },
                handleCommand = { intent ->
                    if (!destroying)
                        synchronized(this) { if (!destroying) handleCommandIntent(intent) }
                },
                reportQueueSize = { size ->
                    sendBroadcast(
                        Intent(ActivityMain.ACTION_QUEUE_CHANGED)
                            .setPackage(packageName)
                            .putExtra(ActivityMain.EXTRA_SIZE, size)
                    )
                },
                releaseWakeLock = {
                    val wakeLock = getLock(this)
                    if (wakeLock.isHeld) wakeLock.release() else Log.w(TAG, "Wakelock under-locked")
                },
            )
        logHandler =
            SinkholeLogHandler(
                looper = logLooper,
                context = applicationContext,
                prefs = prefs,
                shouldShowAccessNotification = { uid ->
                    withPolicyRead { mapNotify[uid] != false }
                },
                showAccessNotification = ::showAccessNotification,
            )
        statsHandler =
            SinkholeStatsHandler(
                looper = statsLooper,
                isEnabled = { !destroying && prefs.getBoolean("show_stats", false) },
                onStarted = ::onStatsStarted,
                onStopped = ::onStatsStopped,
                onRefresh = ::renderTrafficStats,
            )

        val ifUser = IntentFilter()
        ifUser.addAction(Intent.ACTION_USER_BACKGROUND)
        ifUser.addAction(Intent.ACTION_USER_FOREGROUND)
        ContextCompat.registerReceiver(
            this,
            userReceiver,
            ifUser,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        registeredUser = true

        val ifIdle = IntentFilter()
        ifIdle.addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
        ContextCompat.registerReceiver(
            this,
            idleStateReceiver,
            ifIdle,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        registeredIdleState = true

        val ifAp = IntentFilter()
        ifAp.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED")
        ContextCompat.registerReceiver(
            this,
            apStateReceiver,
            ifAp,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        registeredApState = true

        val ifPackage = IntentFilter()
        ifPackage.addAction(Intent.ACTION_PACKAGE_ADDED)
        ifPackage.addAction(Intent.ACTION_PACKAGE_REMOVED)
        ifPackage.addDataScheme("package")
        ContextCompat.registerReceiver(
            this,
            packageChangedReceiver,
            ifPackage,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        registeredPackageChanged = true

        try {
            networkMonitor.start()
        } catch (ex: RuntimeException) {
            Log.w(TAG, "Could not register network callbacks", ex)
        }
        listenConnectivityChanges()

        WorkScheduler.scheduleHousekeeping(this)
    }

    private fun listenConnectivityChanges() {
        Log.i(TAG, "Starting listening to service state changes")
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        if (tm != null) {
            telephonyMonitor.startDataConnectionListener(tm)
        }
    }

    private fun getCommandExtra(intent: Intent): Command? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_COMMAND, Command::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_COMMAND) as? Command
        }
    }

    private fun onPreferenceChanged(name: String?) {
        if ("theme" == name) {
            Log.i(TAG, "Theme changed")
            Util.setTheme(this)
            if (state != State.none) {
                Log.d(TAG, "Stop foreground state=$state")
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
            if (state == State.enforcing) {
                startForeground(NOTIFY_ENFORCING, foregroundNotifications.enforcing())
            } else if (state != State.none) {
                startForeground(NOTIFY_WAITING, foregroundNotifications.waiting())
            }
            Log.d(TAG, "Start foreground state=$state")
        }
        if (name == "watchdog" || name == "enabled") {
            val watchdog = prefs.getString("watchdog", "0")?.toIntOrNull() ?: 0
            val enabled = prefs.getBoolean("enabled", false)
            WorkScheduler.scheduleWatchdog(this, watchdog, enabled)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (state == State.enforcing) {
            startForeground(NOTIFY_ENFORCING, foregroundNotifications.enforcing())
        } else {
            startForeground(NOTIFY_WAITING, foregroundNotifications.waiting())
        }

        Log.i(TAG, "Received $intent")
        Util.logExtras(intent)

        var actualIntent = intent

        if (
            actualIntent != null &&
                actualIntent.hasExtra(EXTRA_COMMAND) &&
                getCommandExtra(actualIntent) == Command.set
        ) {
            set(actualIntent)
            return START_STICKY
        }

        getLock(this).acquire()

        val enabled = prefs.getBoolean("enabled", false)

        if (actualIntent == null) {
            Log.i(TAG, "Restart")
            actualIntent = Intent(this, ServiceSinkhole::class.java)
            actualIntent.putExtra(EXTRA_COMMAND, if (enabled) Command.start else Command.stop)
        }

        if (ACTION_HOUSE_HOLDING == actualIntent.action) {
            actualIntent.putExtra(EXTRA_COMMAND, Command.householding)
        }
        if (ACTION_WATCHDOG == actualIntent.action) {
            actualIntent.putExtra(EXTRA_COMMAND, Command.watchdog)
        }

        var cmd = getCommandExtra(actualIntent)
        if (cmd == null) {
            actualIntent.putExtra(EXTRA_COMMAND, if (enabled) Command.start else Command.stop)
            cmd = getCommandExtra(actualIntent) ?: Command.stop
        }
        val reason = actualIntent.getStringExtra(EXTRA_REASON)
        Log.i(
            TAG,
            "Start intent=$actualIntent command=$cmd reason=$reason" +
                " vpn=" +
                (vpn != null) +
                " user=" +
                (Process.myUid() / 100000),
        )

        if (!commandHandler.queue(actualIntent)) {
            getLock(this).release()
        }

        return START_STICKY
    }

    private fun set(intent: Intent) {
        val uid = intent.getIntExtra(EXTRA_UID, 0)
        val network = intent.getStringExtra(EXTRA_NETWORK) ?: return
        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: return
        if (network != "wifi" && network != "other") return
        val blocked = intent.getBooleanExtra(EXTRA_BLOCKED, false)
        Log.i(TAG, "Set $pkg $network=$blocked")

        val defaultWifi = prefs.getBoolean(PreferenceKeys.WHITELIST_WIFI)
        val defaultOther = prefs.getBoolean(PreferenceKeys.WHITELIST_OTHER)

        val key = PreferencesRepository.namespaced(network, pkg)
        if (blocked == (if ("wifi" == network) defaultWifi else defaultOther)) {
            prefs.remove(key)
        } else {
            prefs.putBoolean(key, blocked)
        }

        reload("notification", this@ServiceSinkhole, false)

        notifications.showNewApplication(uid, false)

        val ruleset = Intent(ActivityMain.ACTION_RULES_CHANGED).setPackage(packageName)
        sendBroadcast(ruleset)
    }

    override fun onRevoke() {
        Log.i(TAG, "Revoke")

        prefs.putBoolean("enabled", false)

        showDisabledNotification()
        Widgets.updateFirewall(this)

        super.onRevoke()
    }

    override fun onDestroy() {
        destroying = true
        synchronized(this) {
            updateExecutor.shutdownNow()
            Log.i(TAG, "Destroy")
            commandLooper.quit()
            logLooper.quit()
            statsLooper.quit()

            for (command in Command.values()) {
                commandHandler.removeMessages(command.ordinal)
            }
            executor.shutdownNow()
            releaseLock(this)

            if (registeredInteractiveState) {
                unregisterReceiver(interactiveStateReceiver)
                registeredInteractiveState = false
            }
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            if (registeredUser) {
                unregisterReceiver(userReceiver)
                registeredUser = false
            }
            if (registeredIdleState) {
                unregisterReceiver(idleStateReceiver)
                registeredIdleState = false
            }
            if (registeredApState) {
                unregisterReceiver(apStateReceiver)
                registeredApState = false
            }
            if (registeredPackageChanged) {
                unregisterReceiver(packageChangedReceiver)
                registeredPackageChanged = false
            }

            networkMonitor.stop()

            telephonyMonitor.stop(tm)

            try {
                if (vpn != null) {
                    stopNative()
                    stopVPN(vpn!!)
                    vpn = null
                    unprepare()
                }
            } catch (ex: Throwable) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }

            Log.i(TAG, "Destroy UniFFI native engine")
            nativeEngine.close()

            removePrefsListener?.invoke()
            removePrefsListener = null
        }

        super.onDestroy()
    }

    private fun showDisabledNotification() {
        notifications.showDisabled()
    }

    private fun showLockdownNotification() {
        notifications.showLockdown()
    }

    private fun removeLockdownNotification() {
        notifications.removeLockdown()
    }

    private fun showErrorNotification(message: String) {
        notifications.showError(message)
    }

    private fun showAccessNotification(uid: Int) {
        notifications.showAccess(uid)
    }

    private fun removeWarningNotifications() {
        notifications.removeWarnings()
    }

    /** Records the effective VPN configuration so equivalent reloads can reuse the tunnel. */
    private inner class TrackingVpnBuilder : VpnService.Builder() {
        private val fingerprint =
            VpnConfigurationFingerprint(
                getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            )

        override fun setMtu(mtu: Int): VpnService.Builder {
            fingerprint.setMtu(mtu)
            super.setMtu(mtu)
            return this
        }

        override fun addAddress(address: String, prefixLength: Int): TrackingVpnBuilder {
            fingerprint.addAddress(address, prefixLength)
            super.addAddress(address, prefixLength)
            return this
        }

        override fun addRoute(address: String, prefixLength: Int): TrackingVpnBuilder {
            fingerprint.addRoute(address, prefixLength)
            super.addRoute(address, prefixLength)
            return this
        }

        override fun addRoute(address: InetAddress, prefixLength: Int): TrackingVpnBuilder {
            val host = address.hostAddress ?: return this
            fingerprint.addRoute(host, prefixLength)
            super.addRoute(address, prefixLength)
            return this
        }

        override fun addDnsServer(address: InetAddress): TrackingVpnBuilder {
            fingerprint.addDnsServer(address)
            super.addDnsServer(address)
            return this
        }

        @Throws(PackageManager.NameNotFoundException::class)
        override fun addAllowedApplication(packageName: String): VpnService.Builder {
            fingerprint.addAllowedApplication(packageName)
            return super.addAllowedApplication(packageName)
        }

        @Throws(PackageManager.NameNotFoundException::class)
        override fun addDisallowedApplication(packageName: String): TrackingVpnBuilder {
            fingerprint.addDisallowedApplication(packageName)
            super.addDisallowedApplication(packageName)
            return this
        }

        override fun equals(other: Any?): Boolean =
            other is TrackingVpnBuilder && fingerprint == other.fingerprint

        override fun hashCode(): Int = fingerprint.hashCode()
    }

    companion object {
        private const val TAG = "NetGuard.Service"

        private const val NOTIFY_ENFORCING = 1
        private const val NOTIFY_WAITING = 2
        private const val NOTIFY_TRAFFIC = 7
        const val NOTIFY_EXTERNAL = 9
        const val NOTIFY_DOWNLOAD = 10

        const val EXTRA_COMMAND = "Command"
        private const val EXTRA_REASON = "Reason"
        const val EXTRA_NETWORK = "Network"
        const val EXTRA_UID = "UID"
        const val EXTRA_PACKAGE = "Package"
        const val EXTRA_BLOCKED = "Blocked"
        const val EXTRA_INTERACTIVE = "Interactive"
        const val EXTRA_TEMPORARY = "Temporary"
        const val EXTRA_UPDATE_CHECK_STATUS = "UpdateCheckStatus"
        const val EXTRA_UPDATE_CHECK_VERSION = "UpdateCheckVersion"

        private const val MIN_STATS_FREQUENCY_MS = 250L
        private const val MAX_STATS_FREQUENCY_MS = 60_000L
        private const val MIN_STATS_SAMPLES = 1L
        private const val MAX_STATS_SAMPLES = 3_600L
        private const val USER_FOREGROUND_DELAY_MS = 3_000L

        @Volatile private var wlInstance: PowerManager.WakeLock? = null

        const val ACTION_HOUSE_HOLDING = "com.bernaferrari.quietguard.HOUSE_HOLDING"
        private const val ACTION_SCREEN_OFF_DELAYED =
            "com.bernaferrari.quietguard.SCREEN_OFF_DELAYED"
        const val ACTION_WATCHDOG = "com.bernaferrari.quietguard.WATCHDOG"
        const val ACTION_UPDATE_CHECK_RESULT = "com.bernaferrari.quietguard.UPDATE_CHECK_RESULT"

        private const val MIN_PCAP_RECORD_SIZE = 1
        private const val MAX_PCAP_RECORD_SIZE = 10_000
        private const val MIN_PCAP_FILE_SIZE_MIB = 1
        private const val MAX_PCAP_FILE_SIZE_MIB = 1_024

        private fun setPcap(enabled: Boolean, context: Context, engine: NativeVpnEngineController) {
            val prefs = context.preferences()
            var recordSize = 64
            try {
                var r = prefs.getString("pcap_record_size", null)
                if (TextUtils.isEmpty(r)) r = "64"
                recordSize =
                    (r?.toIntOrNull() ?: 64).coerceIn(MIN_PCAP_RECORD_SIZE, MAX_PCAP_RECORD_SIZE)
            } catch (ex: Throwable) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }

            var fileSize = 2 * 1024 * 1024
            try {
                var f = prefs.getString("pcap_file_size", null)
                if (TextUtils.isEmpty(f)) f = "2"
                val fileSizeMib =
                    (f?.toIntOrNull() ?: 2).coerceIn(MIN_PCAP_FILE_SIZE_MIB, MAX_PCAP_FILE_SIZE_MIB)
                fileSize = fileSizeMib * 1024 * 1024
            } catch (ex: Throwable) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }

            val pcap =
                if (enabled)
                    File(
                        context.getDir("data", Context.MODE_PRIVATE),
                        "netguard.pcap",
                    )
                else null
            try {
                engine.configurePcap(
                    NativePcapConfig(pcap?.absolutePath, recordSize, fileSize.toLong())
                )
            } catch (ex: Throwable) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }
        }

        @Synchronized
        private fun getLock(context: Context): PowerManager.WakeLock {
            if (wlInstance == null) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                wlInstance =
                    pm.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        context.getString(R.string.app_name) + " wakelock",
                    )
                wlInstance?.setReferenceCounted(true)
            }
            return wlInstance!!
        }

        @Synchronized
        private fun releaseLock(context: Context) {
            if (wlInstance != null) {
                while (wlInstance?.isHeld == true) {
                    wlInstance?.release()
                }
                wlInstance = null
            }
        }

        @JvmStatic
        fun getDns(context: Context): List<InetAddress> = SinkholeDnsResolver.resolve(context)

        @JvmStatic
        fun run(reason: String, context: Context) = sendCommand(context, Command.run, reason)

        @JvmStatic
        fun start(reason: String, context: Context) = sendCommand(context, Command.start, reason)

        @JvmStatic
        fun reload(reason: String, context: Context, interactive: Boolean) {
            if (context.preferences().getBoolean(PreferenceKeys.ENABLED)) {
                sendCommand(context, Command.reload, reason) {
                    putExtra(EXTRA_INTERACTIVE, interactive)
                }
            }
        }

        @JvmStatic
        fun stop(reason: String, context: Context, vpnonly: Boolean) =
            sendCommand(context, Command.stop, reason) { putExtra(EXTRA_TEMPORARY, vpnonly) }

        @JvmStatic
        fun reloadStats(reason: String, context: Context) =
            sendCommand(context, Command.stats, reason)

        @JvmStatic
        fun checkForUpdateNow(reason: String, context: Context) =
            sendCommand(context, Command.updatecheck, reason)

        private fun sendCommand(
            context: Context,
            command: Command,
            reason: String,
            extras: Intent.() -> Unit = {},
        ) {
            val intent =
                Intent(context, ServiceSinkhole::class.java).apply {
                    putExtra(EXTRA_COMMAND, command)
                    putExtra(EXTRA_REASON, reason)
                    extras()
                }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (failure: IllegalStateException) {
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        failure is ForegroundServiceStartNotAllowedException
                ) {
                    try {
                        context.startService(intent)
                    } catch (fallbackFailure: IllegalStateException) {
                        Log.w(TAG, "Could not start service for $command", fallbackFailure)
                    }
                } else {
                    Log.w(TAG, "Could not start foreground service for $command", failure)
                }
            } catch (failure: SecurityException) {
                Log.w(TAG, "Could not start foreground service for $command", failure)
            }
        }
    }

    private enum class State {
        none,
        waiting,
        enforcing,
        stats,
    }

    enum class Command {
        run,
        start,
        reload,
        stop,
        stats,
        set,
        householding,
        watchdog,
        updatecheck,
    }
}
