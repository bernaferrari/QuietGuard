package eu.faircode.netguard

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
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.IconCompat
import androidx.core.view.drawToBitmap
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.roundToInt

object MaterialIconsCompat {
    private const val ICON_SIZE_DP = 24

    private val cache = ConcurrentHashMap<String, Bitmap>()

    fun security(context: Context): IconCompat = icon(context, Icons.Default.Security)

    fun filterList(context: Context): IconCompat = icon(context, Icons.Default.FilterList)

    fun equalizer(context: Context): IconCompat = icon(context, Icons.Default.Equalizer)

    fun hourglass(context: Context): IconCompat = icon(context, Icons.Default.HourglassEmpty)

    fun cloudUpload(context: Context): IconCompat = icon(context, Icons.Default.CloudUpload)

    fun fileDownload(context: Context): IconCompat = icon(context, Icons.Default.FileDownload)

    fun lock(context: Context): IconCompat = icon(context, Icons.Default.Lock)

    fun error(context: Context): IconCompat = icon(context, Icons.Default.Error)

    fun wifi(context: Context, enabled: Boolean): IconCompat =
        icon(context, if (enabled) Icons.Default.Wifi else Icons.Default.WifiOff)

    fun cellular(context: Context, enabled: Boolean): IconCompat =
        icon(context, if (enabled) Icons.Default.SignalCellular4Bar else Icons.Default.SignalCellularOff)

    fun asTileIcon(context: Context, icon: IconCompat): Icon = icon.toIcon(context)

    private fun icon(context: Context, imageVector: ImageVector): IconCompat =
        IconCompat.createWithBitmap(
            cache.getOrPut(cacheKey(context, imageVector)) { materialBitmap(context, imageVector) },
        )

    private fun cacheKey(context: Context, imageVector: ImageVector): String {
        return imageVector.name + "@" + context.resources.displayMetrics.densityDpi
    }

    @Composable
    private fun IconComposable(imageVector: ImageVector) {
        Box(
            modifier =
                Modifier
                    .size(ICON_SIZE_DP.dp)
                    .fillMaxSize(),
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    private fun materialBitmap(context: Context, imageVector: ImageVector): Bitmap {
        val size = max(1, (ICON_SIZE_DP * context.resources.displayMetrics.density).roundToInt())
        val composeView =
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    IconComposable(imageVector)
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
}
