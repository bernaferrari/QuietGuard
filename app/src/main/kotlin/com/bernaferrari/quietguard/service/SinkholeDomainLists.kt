package com.bernaferrari.quietguard.service

import android.content.Context
import android.util.Log
import com.bernaferrari.quietguard.data.PreferencesRepository
import java.io.File

/** Thread-safe cache for the hosts and malware domain lists used by the packet policy. */
internal class SinkholeDomainLists(
    private val context: Context,
    private val prefs: PreferencesRepository,
) {
    private val blockedHosts = HashSet<String>()
    private val malwareHosts = HashSet<String>()
    private var lastHostsModified = 0L
    private var lastMalwareModified = 0L

    @Synchronized
    fun refreshHosts() = reload(
        enabled = prefs.getBoolean("filter", false) && prefs.getBoolean("use_hosts", false),
        file = File(context.filesDir, HOSTS_FILE),
        target = blockedHosts,
        previousModified = lastHostsModified,
        setModified = { lastHostsModified = it },
        includeTestDomain = true,
        description = "Hosts",
    )

    @Synchronized
    fun refreshMalware() = reload(
        enabled = prefs.getBoolean("filter", false) && prefs.getBoolean("malware", false),
        file = File(context.filesDir, MALWARE_FILE),
        target = malwareHosts,
        previousModified = lastMalwareModified,
        setModified = { lastMalwareModified = it },
        includeTestDomain = false,
        description = "Malware",
    )

    @Synchronized
    fun isBlocked(domain: String): Boolean = domain in blockedHosts

    @Synchronized
    fun isMalware(domain: String): Boolean = domain in malwareHosts

    @Synchronized
    fun blockedCount(): Int = blockedHosts.size

    @Synchronized
    fun clear() {
        blockedHosts.clear()
        malwareHosts.clear()
    }

    private fun reload(
        enabled: Boolean,
        file: File,
        target: HashSet<String>,
        previousModified: Long,
        setModified: (Long) -> Unit,
        includeTestDomain: Boolean,
        description: String,
    ) {
        if (!enabled || !file.exists() || !file.canRead()) {
            Log.i(TAG, "$description file use=$enabled exists=${file.exists()}")
            target.clear()
            return
        }
        val modified = file.lastModified()
        if (modified == previousModified && target.isNotEmpty()) {
            Log.i(TAG, "$description file unchanged")
            return
        }
        val domains = DomainListLoader.load(file, includeTestDomain) { line ->
            Log.i(TAG, "Invalid ${description.lowercase()} file line: $line")
        } ?: run {
            Log.e(TAG, "Unable to read ${description.lowercase()} file")
            return
        }
        target.clear()
        target.addAll(domains)
        if (includeTestDomain) target.add(TEST_DOMAIN)
        setModified(modified)
        Log.i(TAG, "${domains.size} ${description.lowercase()} read")
    }

    private companion object {
        const val TAG = "NetGuard.Service"
        const val HOSTS_FILE = "hosts.txt"
        const val MALWARE_FILE = "malware.txt"
        const val TEST_DOMAIN = "test.netguard.me"
    }
}
