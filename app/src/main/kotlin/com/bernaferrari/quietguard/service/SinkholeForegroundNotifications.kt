package com.bernaferrari.quietguard.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import com.bernaferrari.quietguard.ActivityMain
import com.bernaferrari.quietguard.Notifications
import com.bernaferrari.quietguard.PendingIntentCompat
import com.bernaferrari.quietguard.Util
import com.bernaferrari.quietguard.data.PreferencesRepository
import com.bernaferrari.quietguard.lockIcon
import com.bernaferrari.quietguard.securityIcon
import com.bernaferrari.quietguard.shared.R
import com.bernaferrari.quietguard.ui.theme.THEME_DEFAULT
import com.bernaferrari.quietguard.ui.theme.themePrimaryColor

/** Builds and remembers foreground-service notifications independently of VPN state changes. */
internal class SinkholeForegroundNotifications(
    private val context: Context,
    private val prefs: PreferencesRepository,
    private val isLockedDown: () -> Boolean,
    private val enforcingNotificationId: Int,
) {
    private var lastAllowed = UNKNOWN
    private var lastBlocked = UNKNOWN
    private var lastHosts = UNKNOWN

    fun enforcing(allowed: Int = UNKNOWN, blocked: Int = UNKNOWN, hosts: Int = UNKNOWN): Notification {
        val (allowedValue, blockedValue, hostsValue) = rememberCounts(allowed, blocked, hosts)
        return foregroundBuilder(
            icon = if (isLockedDown()) context.lockIcon() else context.securityIcon(),
        )
            .setContentTitle(context.getString(R.string.msg_started))
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(Notification.VISIBILITY_SECRET)
            .apply {
                if (allowedValue >= 0 || blockedValue >= 0 || hostsValue >= 0) {
                    setContentText(
                        if (Util.isPlayStoreInstall(context)) {
                            context.getString(R.string.msg_packages, allowedValue, blockedValue)
                        } else {
                            context.getString(R.string.msg_hosts, allowedValue, blockedValue, hostsValue)
                        },
                    )
                }
            }
            .build()
    }

    fun waiting(): Notification = foregroundBuilder(icon = context.securityIcon())
        .setContentTitle(context.getString(R.string.msg_waiting))
        .setCategory(Notification.CATEGORY_STATUS)
        .setVisibility(Notification.VISIBILITY_SECRET)
        .build()

    fun updateEnforcing(allowed: Int, total: Int, hosts: Int) {
        if (Util.canNotify(context)) {
            val notifications =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notifications.notify(enforcingNotificationId, enforcing(allowed, total - allowed, hosts))
        }
    }

    fun resetCounts() {
        lastAllowed = UNKNOWN
        lastBlocked = UNKNOWN
        lastHosts = UNKNOWN
    }

    private fun foregroundBuilder(icon: Icon): Notification.Builder {
        val main = Intent(context, ActivityMain::class.java)
        val pendingIntent =
            PendingIntentCompat.getActivity(context, 0, main, PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(context, Notifications.CHANNEL_FOREGROUND)
            .setSmallIcon(icon)
            .setContentIntent(pendingIntent)
            .setColor(themePrimaryColor(prefs.getString("theme", THEME_DEFAULT)))
            .setOngoing(true)
            .setAutoCancel(false)
    }

    private fun rememberCounts(allowed: Int, blocked: Int, hosts: Int): Triple<Int, Int, Int> {
        lastAllowed = allowed.takeIf { it >= 0 } ?: lastAllowed
        lastBlocked = blocked.takeIf { it >= 0 } ?: lastBlocked
        lastHosts = hosts.takeIf { it >= 0 } ?: lastHosts
        return Triple(lastAllowed, lastBlocked, lastHosts)
    }

    private companion object {
        const val UNKNOWN = -1
    }
}
