package com.bernaferrari.quietguard.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.bernaferrari.quietguard.Util
import com.bernaferrari.quietguard.ui.theme.GraphGrayed
import com.bernaferrari.quietguard.ui.theme.GraphReceive
import com.bernaferrari.quietguard.ui.theme.GraphSend

internal data class TrafficGraph(val bitmap: Bitmap, val peak: Float)

internal fun renderTrafficGraph(
    context: Context,
    points: List<TrafficPoint>,
    samples: Long,
): TrafficGraph {
    val height = Util.dips2pixels(96, context)
    val width = Util.dips2pixels(96 * 5, context)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val peak = points.maxOfOrNull { maxOf(it.transmitPerSecond, it.receivePerSecond) } ?: 0f
    val newest = points.maxOfOrNull { it.timestampMs } ?: 0L
    val scale = peak.coerceAtLeast(1f)
    val transmit = Path()
    val receive = Path()

    for ((index, point) in points.withIndex()) {
        val x = width - width * (newest - point.timestampMs) / 1000f / samples
        val tx = height - height * point.transmitPerSecond / scale
        val rx = height - height * point.receivePerSecond / scale
        if (index == 0) {
            transmit.moveTo(x, tx)
            receive.moveTo(x, rx)
        } else {
            transmit.lineTo(x, tx)
            receive.lineTo(x, rx)
        }
    }

    val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = Util.dips2pixels(1, context).toFloat()
            color = GraphGrayed
        }
    val center = height / 2f
    canvas.drawLine(0f, center, width.toFloat(), center, paint)
    paint.strokeWidth = Util.dips2pixels(2, context).toFloat()
    paint.color = GraphSend
    canvas.drawPath(transmit, paint)
    paint.color = GraphReceive
    canvas.drawPath(receive, paint)
    return TrafficGraph(bitmap, peak)
}
