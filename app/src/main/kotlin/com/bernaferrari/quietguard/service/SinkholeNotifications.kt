package com.bernaferrari.quietguard.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.util.Log
import com.bernaferrari.quietguard.*
import com.bernaferrari.quietguard.data.PreferenceKeys
import com.bernaferrari.quietguard.data.PreferencesRepository
import com.bernaferrari.quietguard.shared.R
import com.bernaferrari.quietguard.ui.theme.THEME_DEFAULT
import com.bernaferrari.quietguard.ui.theme.themeOffColor
import com.bernaferrari.quietguard.ui.theme.themeOnColor
import com.bernaferrari.quietguard.ui.theme.themePrimaryColor
import java.net.InetAddress
import java.net.UnknownHostException
import java.text.SimpleDateFormat

/** Warning/status notifications that do not depend on live VPN session state. */
internal class SinkholeNotifications(
    private val context: Context,
    private val prefs: PreferencesRepository,
) {
    private val manager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun showDisabled() =
        showText(
            id = DISABLED,
            title = context.getString(R.string.app_name),
            text = context.getString(R.string.msg_revoked),
            intent = Intent(context, ActivityMain::class.java),
        )

    fun showLockdown() =
        showText(
            id = LOCKDOWN,
            title = context.getString(R.string.app_name),
            text = context.getString(R.string.msg_always_on_lockdown),
            intent = Intent(Settings.ACTION_VPN_SETTINGS),
        )

    fun removeLockdown() {
        manager.cancel(LOCKDOWN)
    }

    fun showError(message: String) =
        showText(
            id = ERROR,
            title = context.getString(R.string.app_name),
            text = context.getString(R.string.msg_error, message),
            intent = Intent(context, ActivityMain::class.java),
            summary = message,
        )

    fun showUpdate(name: String, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val pendingIntent =
            PendingIntentCompat.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val notification =
            Notification.Builder(context, Notifications.CHANNEL_NOTIFY)
                .setSmallIcon(context.securityIcon())
                .setContentTitle(name)
                .setContentText(context.getString(R.string.msg_update))
                .setContentIntent(pendingIntent)
                .setColor(themePrimaryColor(prefs.getString("theme", THEME_DEFAULT)))
                .setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(Notification.VISIBILITY_SECRET)
                .setAutoCancel(true)
                .build()
        if (Util.canNotify(context)) manager.notify(UPDATE, notification)
    }

    fun removeWarnings() {
        manager.cancel(DISABLED)
        manager.cancel(ERROR)
    }

    fun showNewApplication(uid: Int, malware: Boolean) {
        if (uid < 0 || uid == Process.myUid()) {
            return
        }

        try {
            val names = Util.getApplicationNames(uid, context)
            if (names.isEmpty()) {
                return
            }
            val name = TextUtils.join(", ", names)

            val pm = context.packageManager
            val packages = pm.getPackagesForUid(uid)
            if (packages == null || packages.isEmpty()) {
                throw PackageManager.NameNotFoundException(uid.toString())
            }
            val internet = Util.hasInternet(uid, context)

            val main = Intent(context, ActivityMain::class.java)
            val pi =
                PendingIntentCompat.getActivity(
                    context,
                    uid,
                    main,
                    PendingIntent.FLAG_UPDATE_CURRENT,
                )

            val notificationColor =
                themePrimaryColor(prefs.getString("theme", THEME_DEFAULT))
            val builder =
                Notification.Builder(
                    context,
                    if (malware) Notifications.CHANNEL_MALWARE else Notifications.CHANNEL_NOTIFY,
                )
            builder
                .setSmallIcon(context.securityIcon())
                .setContentIntent(pi)
                .setColor(notificationColor)
                .setAutoCancel(true)

            if (malware) {
                builder
                    .setContentTitle(name)
                    .setContentText(context.getString(R.string.msg_malware, name))
            } else {
                builder
                    .setContentTitle(name)
                    .setContentText(context.getString(R.string.msg_installed_n))
            }

            builder
                .setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(Notification.VISIBILITY_SECRET)

            val packageName = packages[0]
            val wifi =
                prefs.getBoolean(
                    PreferencesRepository.namespaced("wifi", packageName),
                    prefs.getBoolean(PreferenceKeys.WHITELIST_WIFI),
                )
            val other =
                prefs.getBoolean(
                    PreferencesRepository.namespaced("other", packageName),
                    prefs.getBoolean(PreferenceKeys.WHITELIST_OTHER),
                )

            val riWifi = Intent(context, ServiceSinkhole::class.java)
            riWifi.putExtra(ServiceSinkhole.EXTRA_COMMAND, ServiceSinkhole.Command.set)
            riWifi.putExtra(ServiceSinkhole.EXTRA_NETWORK, "wifi")
            riWifi.putExtra(ServiceSinkhole.EXTRA_UID, uid)
            riWifi.putExtra(ServiceSinkhole.EXTRA_PACKAGE, packageName)
            riWifi.putExtra(ServiceSinkhole.EXTRA_BLOCKED, !wifi)

            val piWifi =
                PendingIntentCompat.getService(
                    context,
                    uid,
                    riWifi,
                    PendingIntent.FLAG_UPDATE_CURRENT,
                )
            val wAction =
                Notification.Action.Builder(
                        if (wifi) context.wifiIcon(true) else context.wifiIcon(false),
                        context.getString(
                            if (wifi) R.string.title_allow_wifi else R.string.title_block_wifi
                        ),
                        piWifi,
                    )
                    .build()
            builder.addAction(wAction)

            val riOther = Intent(context, ServiceSinkhole::class.java)
            riOther.putExtra(ServiceSinkhole.EXTRA_COMMAND, ServiceSinkhole.Command.set)
            riOther.putExtra(ServiceSinkhole.EXTRA_NETWORK, "other")
            riOther.putExtra(ServiceSinkhole.EXTRA_UID, uid)
            riOther.putExtra(ServiceSinkhole.EXTRA_PACKAGE, packageName)
            riOther.putExtra(ServiceSinkhole.EXTRA_BLOCKED, !other)
            val piOther =
                PendingIntentCompat.getService(
                    context,
                    uid + 10000,
                    riOther,
                    PendingIntent.FLAG_UPDATE_CURRENT,
                )
            val oAction =
                Notification.Action.Builder(
                        if (other) context.cellularIcon(true) else context.cellularIcon(false),
                        context.getString(
                            if (other) R.string.title_allow_other else R.string.title_block_other
                        ),
                        piOther,
                    )
                    .build()
            builder.addAction(oAction)

            if (!internet) {
                builder.setStyle(
                    Notification.BigTextStyle()
                        .bigText(context.getString(R.string.msg_installed_n))
                        .setSummaryText(context.getString(R.string.title_internet))
                )
            }
            if (Util.canNotify(context)) manager.notify(uid, builder.build())
        } catch (ex: PackageManager.NameNotFoundException) {
            Log.e("QuietGuard.Notifications", "Package no longer available for UID $uid", ex)
        }
    }

    fun showAccess(uid: Int) {
        val applications = Util.getApplicationNames(uid, context)
        if (applications.isEmpty()) return
        val name = applications.joinToString()
        val intent =
            Intent(context, ActivityMain::class.java)
                .putExtra(ActivityMain.EXTRA_SEARCH, uid.toString())
        val pendingIntent =
            PendingIntentCompat.getActivity(
                context,
                uid + 10_000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val theme = prefs.getString("theme", THEME_DEFAULT)
        val allowedColor = themeOnColor(theme)
        val deniedColor = themeOffColor(theme)
        val style = Notification.InboxStyle().addLine(context.getString(R.string.msg_access_n))
        val packageManager = context.packageManager
        val installedAt =
            packageManager.getPackagesForUid(uid)?.firstOrNull()?.let { packageName ->
                runCatching { packageManager.getPackageInfo(packageName, 0).firstInstallTime }
                    .getOrDefault(0)
            } ?: 0
        val dateFormat = SimpleDateFormat("dd HH:mm")
        for (entry in
            AndroidFirewallDatabase.getInstance(context).getAccessUnset(uid, 7, installedAt)) {
            val address = resolveHostName(entry.daddr)
            val line = "${dateFormat.format(entry.time)} $address"
            val decision = entry.allowed ?: 0
            style.addLine(
                if (decision < 0) line
                else
                    SpannableString(line).apply {
                        val start = indexOf(address).coerceAtLeast(0)
                        setSpan(
                            ForegroundColorSpan(if (decision > 0) allowedColor else deniedColor),
                            start,
                            start + address.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                    }
            )
        }
        val notification =
            Notification.Builder(context, Notifications.CHANNEL_ACCESS)
                .setSmallIcon(context.cloudUploadIcon())
                .setGroup("AccessAttempt")
                .setContentTitle(name)
                .setContentText(context.getString(R.string.msg_access_n))
                .setContentIntent(pendingIntent)
                .setColor(deniedColor)
                .setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(Notification.VISIBILITY_SECRET)
                .setAutoCancel(true)
                .setStyle(style)
                .build()
        if (Util.canNotify(context)) manager.notify(uid + 10_000, notification)
    }

    private fun resolveHostName(address: String): String {
        if (!Util.isNumericAddress(address)) return address
        return try {
            InetAddress.getByName(address).hostName
        } catch (_: UnknownHostException) {
            address
        }
    }

    private fun showText(
        id: Int,
        title: String,
        text: String,
        intent: Intent,
        summary: String? = null,
    ) {
        val pendingIntent =
            PendingIntentCompat.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val style = Notification.BigTextStyle().bigText(text)
        if (summary != null) style.setSummaryText(summary)
        val notification =
            Notification.Builder(context, Notifications.CHANNEL_NOTIFY)
                .setSmallIcon(context.errorIcon())
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setColor(themeOffColor(prefs.getString("theme", THEME_DEFAULT)))
                .setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(Notification.VISIBILITY_SECRET)
                .setAutoCancel(true)
                .setStyle(style)
                .build()
        if (Util.canNotify(context)) manager.notify(id, notification)
    }

    private companion object {
        const val DISABLED = 3
        const val LOCKDOWN = 4
        const val ERROR = 6
        const val UPDATE = 8
    }
}
