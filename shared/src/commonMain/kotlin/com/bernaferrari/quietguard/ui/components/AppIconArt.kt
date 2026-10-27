package com.bernaferrari.quietguard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.bernaferrari.quietguard.ui.icons.Icon
import com.bernaferrari.quietguard.ui.icons.MaterialIcon

/** A drawn app icon: a coloured tile with a symbol or a short mark. */
@Immutable
data class AppIconArt(
    val background: Color,
    val backgroundEnd: Color = background,
    val icon: MaterialIcon? = null,
    val letter: String? = null,
    val tint: Color = Color.White,
)

/**
 * Lets a host supply drawn icons for packages that are not installed (the store-capture build's
 * fictional apps). Production hosts leave it null, so [AppIcon] always uses the installed icon.
 */
object AppIconOverrides {
    var lookup: ((packageName: String) -> AppIconArt?)? = null
}

@Composable
fun AppIconArtTile(
    art: AppIconArt,
    size: Dp,
    cornerRadius: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.verticalGradient(listOf(art.background, art.backgroundEnd))),
        contentAlignment = Alignment.Center,
    ) {
        when {
            art.letter != null ->
                Text(
                    text = art.letter,
                    color = art.tint,
                    fontSize = (size.value * if (art.letter.length > 1) 0.36f else 0.5f).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            art.icon != null ->
                Icon(
                    icon = art.icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(size * 0.56f),
                    tint = art.tint,
                )
        }
    }
}
