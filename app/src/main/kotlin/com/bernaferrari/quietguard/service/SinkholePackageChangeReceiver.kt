package com.bernaferrari.quietguard.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.bernaferrari.quietguard.AndroidFirewallDatabase
import com.bernaferrari.quietguard.Rule
import com.bernaferrari.quietguard.Util
import com.bernaferrari.quietguard.data.PreferencesRepository

/**
 * Keeps package installation/removal cleanup separate from VPN lifecycle orchestration.
 *
 * Preferences are read only when a broadcast arrives: the service creates this receiver before it
 * has a Context, so touching preferences at construction time would crash.
 */
internal class SinkholePackageChangeReceiver(
    private val preferences: () -> PreferencesRepository,
    private val onNewApplication: (uid: Int) -> Unit,
    private val onReload: (reason: String, context: Context) -> Unit,
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "Received $intent")
        Util.logExtras(intent)

        try {
            when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED -> handlePackageAdded(context, intent)
                Intent.ACTION_PACKAGE_REMOVED -> handlePackageRemoved(context, intent)
            }
        } catch (ex: Throwable) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
        }
    }

    private fun handlePackageAdded(context: Context, intent: Intent) {
        Rule.clearCache()
        if (
            !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false) &&
                preferences().getBoolean("install", true)
        ) {
            onNewApplication(intent.getIntExtra(Intent.EXTRA_UID, -1))
        }
        onReload("package added", context)
    }

    private fun handlePackageRemoved(context: Context, intent: Intent) {
        Rule.clearCache()
        if (intent.getBooleanExtra(Intent.EXTRA_DATA_REMOVED, false)) {
            removePackageData(context, intent)
        }
        onReload("package deleted", context)
    }

    private fun removePackageData(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart.orEmpty()
        Log.i(TAG, "Deleting settings package=$packageName")
        val prefs = preferences()
        PACKAGE_PREFERENCE_NAMES.forEach { name ->
            prefs.remove(PreferencesRepository.namespaced(name, packageName))
        }

        val uid = intent.getIntExtra(Intent.EXTRA_UID, 0)
        if (uid > 0) {
            AndroidFirewallDatabase.getInstance(context).apply {
                clearLog(uid)
                clearAccess(uid, false)
            }
            val notifications =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notifications.cancel(uid)
            notifications.cancel(uid + NOTIFICATION_OTHER_OFFSET)
        }
    }

    private companion object {
        const val TAG = "NetGuard.Service"
        const val NOTIFICATION_OTHER_OFFSET = 10_000
        val PACKAGE_PREFERENCE_NAMES =
            listOf(
                "wifi",
                "other",
                "screen_wifi",
                "screen_other",
                "roaming",
                "lockdown",
                "apply",
                "notify",
            )
    }
}
