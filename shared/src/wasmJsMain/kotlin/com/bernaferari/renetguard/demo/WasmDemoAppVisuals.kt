package com.bernaferari.renetguard.demo

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicVideo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow

import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

internal data class WasmDemoAppVisual(
    val backgroundColor: Color,
    val icon: ImageVector? = null,
    val letter: String? = null,
    val iconTint: Color = Color.White,
)

internal val wasmDemoAppVisuals: Map<String, WasmDemoAppVisual> =
    mapOf(
        "com.android.chrome" to WasmDemoAppVisual(Color(0xFF4285F4), icon = Icons.Default.Language),
        "com.google.android.gm" to WasmDemoAppVisual(Color(0xFFEA4335), icon = Icons.Default.Email),
        "com.spotify.music" to WasmDemoAppVisual(Color(0xFF1DB954), icon = Icons.Default.MusicNote),
        "com.instagram.android" to WasmDemoAppVisual(Color(0xFFE1306C), icon = Icons.Default.CameraAlt),
        "com.whatsapp" to WasmDemoAppVisual(Color(0xFF25D366), icon = Icons.AutoMirrored.Filled.Chat),
        "com.google.android.youtube" to WasmDemoAppVisual(Color(0xFFFF0000), icon = Icons.Default.PlayArrow),
        "com.twitter.android" to WasmDemoAppVisual(Color(0xFF000000), letter = "𝕏"),
        "com.facebook.katana" to WasmDemoAppVisual(Color(0xFF1877F2), icon = Icons.Default.Group),
        "com.google.android.apps.maps" to WasmDemoAppVisual(Color(0xFF34A853), icon = Icons.Default.Map),
        "com.amazon.mShop.android.shopping" to WasmDemoAppVisual(Color(0xFFFF9900), icon = Icons.Default.ShoppingCart),
        "com.netflix.mediaclient" to WasmDemoAppVisual(Color(0xFFE50914), icon = Icons.Default.Movie),
        "com.discord" to WasmDemoAppVisual(Color(0xFF5865F2), icon = Icons.Default.Headset),
        "com.android.vending" to WasmDemoAppVisual(Color(0xFF01875F), icon = Icons.Default.Shop),
        "com.google.android.gms" to WasmDemoAppVisual(Color(0xFF5F6368), icon = Icons.Default.Extension),
        "com.reddit.frontpage" to WasmDemoAppVisual(Color(0xFFFF4500), icon = Icons.Default.Forum),
        "org.telegram.messenger" to WasmDemoAppVisual(Color(0xFF2AABEE), icon = Icons.AutoMirrored.Filled.Send),
        "com.zhiliaoapp.musically" to WasmDemoAppVisual(Color(0xFF010101), icon = Icons.Default.MusicVideo, iconTint = Color(0xFFFE2C55)),
        "com.Slack" to WasmDemoAppVisual(Color(0xFF4A154B), icon = Icons.Default.Work),
        "com.linkedin.android" to WasmDemoAppVisual(Color(0xFF0A66C2), icon = Icons.Default.BusinessCenter),
        "org.thoughtcrime.securesms" to WasmDemoAppVisual(Color(0xFF3A76F0), icon = Icons.Default.Lock),
        "com.google.android.apps.photos" to WasmDemoAppVisual(Color(0xFFFBBC04), icon = Icons.Default.PhotoLibrary, iconTint = Color(0xFF202124)),
        "com.google.android.apps.docs" to WasmDemoAppVisual(Color(0xFF0F9D58), icon = Icons.Default.Cloud),
        "com.ubercab" to WasmDemoAppVisual(Color(0xFF000000), icon = Icons.Default.LocalTaxi),
        "com.chase.sig.android" to WasmDemoAppVisual(Color(0xFF117ACA), icon = Icons.Default.AccountBalance),
    )

internal fun wasmDemoAppVisual(
    packageName: String?,
    displayName: String?,
): WasmDemoAppVisual? {
    packageName?.let { wasmDemoAppVisuals[it] }?.let { return it }
    return displayName
        ?.let { name ->
            wasmDemoApps.values
                .firstOrNull { it.label.equals(name, ignoreCase = true) }
                ?.packageName
                ?.let { wasmDemoAppVisuals[it] }
        }
}

internal fun wasmDemoFallbackVisual(label: String): WasmDemoAppVisual {
    val palette =
        listOf(
            Color(0xFF5C6BC0),
            Color(0xFF26A69A),
            Color(0xFFEF5350),
            Color(0xFFAB47BC),
            Color(0xFF42A5F5),
            Color(0xFFFF7043),
            Color(0xFF66BB6A),
            Color(0xFF8D6E63),
        )
    val index = label.fold(0) { acc, char -> acc + char.code } % palette.size
    val letter = label.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?"
    return WasmDemoAppVisual(backgroundColor = palette[index], letter = letter)
}