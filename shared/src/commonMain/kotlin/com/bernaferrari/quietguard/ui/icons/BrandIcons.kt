package com.bernaferrari.quietguard.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Brand marks that are not part of Material Symbols. */
object BrandIcons {
    val X: MaterialIcon get() = MaterialIcon(xLogo)
}

// X mark from Simple Icons (CC0-1.0), normalized to its native 24 × 24 viewport.
// https://simpleicons.org/?q=x
private val xLogo: ImageVector by lazy {
    ImageVector.Builder(
        name = "X",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(18.901f, 1.153f)
            horizontalLineTo(22.581f)
            lineTo(14.541f, 10.343f)
            lineTo(24f, 22.846f)
            horizontalLineTo(16.594f)
            lineTo(10.794f, 15.262f)
            lineTo(4.156f, 22.846f)
            horizontalLineTo(0.474f)
            lineTo(9.133f, 12.949f)
            lineTo(0f, 1.154f)
            horizontalLineTo(7.594f)
            lineTo(12.837f, 8.086f)
            lineTo(18.901f, 1.153f)
            close()
            moveTo(17.611f, 20.644f)
            horizontalLineTo(19.65f)
            lineTo(6.486f, 3.24f)
            horizontalLineTo(4.298f)
            lineTo(17.61f, 20.644f)
            close()
        }
    }.build()
}
