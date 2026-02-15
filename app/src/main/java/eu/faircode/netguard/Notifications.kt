package eu.faircode.netguard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.drawable.Icon
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.SignalCellularOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon as ComposeIcon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.view.drawToBitmap
import kotlin.math.max
import kotlin.math.roundToInt
import java.util.concurrent.ConcurrentHashMap

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

private const val NOTIFICATION_ICON_SIZE_DP = 24
private val materialNotificationIconCache = ConcurrentHashMap<String, Bitmap>()

fun Context.securityIcon(): Icon = notificationIcon(Icons.Default.Security)

fun Context.filterListIcon(): Icon = notificationIcon(Icons.Default.FilterList)

fun Context.equalizerIcon(): Icon = notificationIcon(Icons.Default.Equalizer)

fun Context.hourglassIcon(): Icon = notificationIcon(Icons.Default.HourglassEmpty)

fun Context.cloudUploadIcon(): Icon = notificationIcon(Icons.Default.CloudUpload)

fun Context.fileDownloadIcon(): Icon = notificationIcon(Icons.Default.FileDownload)

fun Context.lockIcon(): Icon = notificationIcon(Icons.Default.Lock)

fun Context.errorIcon(): Icon = notificationIcon(Icons.Default.Error)

fun Context.wifiIcon(enabled: Boolean): Icon = notificationIcon(if (enabled) Icons.Default.Wifi else Icons.Default.WifiOff)

fun Context.cellularIcon(enabled: Boolean): Icon =
    notificationIcon(if (enabled) Icons.Default.SignalCellular4Bar else Icons.Default.SignalCellularOff)

private fun Context.notificationIcon(imageVector: ImageVector): Icon {
    val key = imageVector.name + "@" + resources.displayMetrics.densityDpi
    val bitmap = materialNotificationIconCache.getOrPut(key) {
        materialBitmap(imageVector)
    }
    return Icon.createWithBitmap(bitmap)
}

@Composable
private fun NotificationIconComposable(imageVector: ImageVector) {
    Box(
        modifier =
            Modifier
                .size(NOTIFICATION_ICON_SIZE_DP.dp)
                .fillMaxSize(),
    ) {
        ComposeIcon(
            imageVector = imageVector,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun Context.materialBitmap(imageVector: ImageVector): Bitmap {
    val size = max(1, (NOTIFICATION_ICON_SIZE_DP * resources.displayMetrics.density).roundToInt())
    val composeView =
        ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                NotificationIconComposable(imageVector)
            }
        }

    val measureSpec = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)
    composeView.measure(measureSpec, measureSpec)
    composeView.layout(0, 0, size, size)

    return try {
        composeView.drawToBitmap(config = Config.ARGB_8888)
    } catch (_: Throwable) {
        Bitmap.createBitmap(size, size, Config.ARGB_8888)
    } finally {
        composeView.disposeComposition()
    }
}
