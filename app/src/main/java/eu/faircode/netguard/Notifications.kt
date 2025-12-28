package eu.faircode.netguard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object Notifications {
    const val CHANNEL_FOREGROUND = "foreground"
    const val CHANNEL_NOTIFY = "notify"
    const val CHANNEL_ACCESS = "access"
    const val CHANNEL_MALWARE = "malware"

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val foreground = NotificationChannel(
            CHANNEL_FOREGROUND,
            context.getString(R.string.channel_foreground),
            NotificationManager.IMPORTANCE_MIN,
        )
        foreground.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT)
        nm.createNotificationChannel(foreground)

        val notify = NotificationChannel(
            CHANNEL_NOTIFY,
            context.getString(R.string.channel_notify),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        notify.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT)
        notify.setBypassDnd(true)
        nm.createNotificationChannel(notify)

        val access = NotificationChannel(
            CHANNEL_ACCESS,
            context.getString(R.string.channel_access),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        access.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT)
        access.setBypassDnd(true)
        nm.createNotificationChannel(access)

        val malware = NotificationChannel(
            CHANNEL_MALWARE,
            context.getString(R.string.setting_malware),
            NotificationManager.IMPORTANCE_HIGH,
        )
        malware.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT)
        malware.setBypassDnd(true)
        nm.createNotificationChannel(malware)
    }
}
