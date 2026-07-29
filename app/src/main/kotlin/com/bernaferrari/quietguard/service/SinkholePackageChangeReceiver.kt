package com.bernaferrari.quietguard.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.bernaferrari.quietguard.ActivityPro
import com.bernaferrari.quietguard.DatabaseHelper
import com.bernaferrari.quietguard.IAB
import com.bernaferrari.quietguard.Rule
import com.bernaferrari.quietguard.Util
import com.bernaferrari.quietguard.data.PreferencesRepository

/** Keeps package installation/removal cleanup separate from VPN lifecycle orchestration. */
internal class SinkholePackageChangeReceiver(
    private val prefs: PreferencesRepository,
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
        Rule.clearCache(context)
        if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false) &&
            IAB.isPurchased(ActivityPro.SKU_NOTIFY, context) &&
            prefs.getBoolean("install", true)
        ) {
            onNewApplication(intent.getIntExtra(Intent.EXTRA_UID, -1))
        }
        onReload("package added", context)
    }

    private fun handlePackageRemoved(context: Context, intent: Intent) {
        Rule.clearCache(context)
        if (intent.getBooleanExtra(Intent.EXTRA_DATA_REMOVED, false)) {
            removePackageData(context, intent)
        }
        onReload("package deleted", context)
    }

    private fun removePackageData(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart.orEmpty()
        Log.i(TAG, "Deleting settings package=$packageName")
        PACKAGE_PREFERENCE_NAMES.forEach { name ->
            prefs.remove(PreferencesRepository.namespaced(name, packageName))
        }

        val uid = intent.getIntExtra(Intent.EXTRA_UID, 0)
        if (uid > 0) {
            DatabaseHelper.getInstance(context).apply {
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
        val PACKAGE_PREFERENCE_NAMES = listOf(
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
