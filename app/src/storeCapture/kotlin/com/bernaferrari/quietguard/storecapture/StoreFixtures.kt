package com.bernaferrari.quietguard.storecapture

import androidx.compose.ui.graphics.Color
import com.bernaferrari.quietguard.ui.components.AppIconArt
import com.bernaferrari.quietguard.ui.icons.MaterialIcon
import com.bernaferrari.quietguard.ui.icons.MaterialSymbols

/**
 * Fictional apps for store screenshots. Names, packages and domains are invented; the mix of
 * rules tells one story: messaging and banking stay online, streaming waits for Wi-Fi, and the
 * flashlight, keyboard and game that only phone home are cut off entirely.
 */
internal data class FixtureApp(
    val uid: Int,
    val packageName: String,
    val name: String,
    val art: AppIconArt,
    val wifiBlocked: Boolean = false,
    val mobileBlocked: Boolean = false,
    val system: Boolean = false,
)

private fun art(top: Long, bottom: Long, icon: MaterialIcon? = null, letter: String? = null, tint: Long = 0xFFFFFFFF) =
    AppIconArt(Color(top), Color(bottom), icon, letter, Color(tint))

internal val fixtureApps: List<FixtureApp> =
    listOf(
        FixtureApp(10201, "app.atlasmaps", "Atlas Maps", art(0xFF34B27B, 0xFF1E8A5C, MaterialSymbols.Filled.Map)),
        FixtureApp(10202, "com.bloomphotos.android", "Bloom Photos", art(0xFFFFB648, 0xFFF08A24, MaterialSymbols.Filled.PhotoLibrary), mobileBlocked = true),
        FixtureApp(10203, "news.brightline", "Brightline News", art(0xFF3F6BE8, 0xFF2747B8, MaterialSymbols.Filled.Public)),
        FixtureApp(10204, "games.candyorbit", "Candy Orbit", art(0xFFFF6FA8, 0xFFE0407E, letter = "✦"), wifiBlocked = true, mobileBlocked = true),
        FixtureApp(10205, "com.clearsky.weather", "Clearsky Weather", art(0xFF5CC8F5, 0xFF2F9BDB, MaterialSymbols.Filled.Cloud)),
        FixtureApp(10206, "tools.flashlightplus", "Flashlight Plus", art(0xFF3B3F4A, 0xFF22252C, MaterialSymbols.Filled.LightMode, tint = 0xFFFFD54F), wifiBlocked = true, mobileBlocked = true),
        FixtureApp(10207, "io.glyphkeys", "Glyph Keyboard", art(0xFF8C6BF0, 0xFF6342C9, letter = "Aa"), wifiBlocked = true, mobileBlocked = true),
        FixtureApp(10208, "chat.hearth", "Hearth Messenger", art(0xFF2FC58F, 0xFF139C6C, MaterialSymbols.AutoMirrored.Filled.Chat)),
        FixtureApp(10209, "shop.northwind", "Northwind Shop", art(0xFFFF8A4C, 0xFFE2602A, MaterialSymbols.Filled.ShoppingCart), mobileBlocked = true),
        FixtureApp(10210, "mail.parcel", "Parcel Mail", art(0xFFEF5350, 0xFFC62F3B, MaterialSymbols.Filled.Email)),
        FixtureApp(10211, "bank.pocket", "Pocket Bank", art(0xFF1F4FA0, 0xFF143673, MaterialSymbols.Filled.AccountBalance)),
        FixtureApp(10212, "tv.reelhouse", "Reelhouse", art(0xFFD7263D, 0xFF9E1428, MaterialSymbols.Filled.Movie), mobileBlocked = true),
        FixtureApp(10213, "app.ridely", "Ridely", art(0xFF222428, 0xFF0E0F11, MaterialSymbols.Filled.LocalTaxi, tint = 0xFF6EE7B7)),
        FixtureApp(10214, "fm.soundwave", "Soundwave", art(0xFF7C4DFF, 0xFF5530D6, MaterialSymbols.Filled.Headset), mobileBlocked = true),
        FixtureApp(10215, "notes.tally", "Tally Notes", art(0xFFFFD54F, 0xFFF4B400, MaterialSymbols.Filled.Work, tint = 0xFF3E2C00)),
        FixtureApp(10216, "fit.trailrun", "Trail Runner", art(0xFF00A6A6, 0xFF007C80, MaterialSymbols.Filled.Group)),
        FixtureApp(10217, "video.wavecast", "Wavecast", art(0xFFFF5A36, 0xFFD93A18, MaterialSymbols.Filled.PlayArrow), mobileBlocked = true),
        FixtureApp(10218, "cloud.yardbox", "Yardbox Drive", art(0xFF4C8DF6, 0xFF2D68D0, MaterialSymbols.Filled.CloudUpload), mobileBlocked = true),
    )

internal val fixtureByUid: Map<Int, FixtureApp> = fixtureApps.associateBy { it.uid }
internal val fixtureByPackage: Map<String, FixtureApp> = fixtureApps.associateBy { it.packageName }

/** One connection attempt, [minutesAgo] before 9:41 (the status bar time in every capture). */
internal data class FixtureConnection(
    val uid: Int,
    val domain: String,
    val address: String,
    val port: Int,
    val allowed: Boolean,
    val minutesAgo: Double,
    val protocol: Int = 6,
)

internal val fixtureConnections: List<FixtureConnection> =
    listOf(
        FixtureConnection(10206, "ads.brightpixel.net", "203.0.113.24", 443, false, 0.2),
        FixtureConnection(10204, "track.playmetric.io", "198.51.100.71", 443, false, 0.6),
        FixtureConnection(10208, "push.hearth.chat", "192.0.2.18", 443, true, 0.9),
        FixtureConnection(10207, "telemetry.glyphkeys.io", "203.0.113.90", 443, false, 1.4),
        FixtureConnection(10212, "cdn.reelhouse.tv", "198.51.100.12", 443, false, 2.1),
        FixtureConnection(10211, "api.pocketbank.com", "192.0.2.45", 443, true, 2.8),
        FixtureConnection(10206, "sdk.adorbit.com", "203.0.113.55", 443, false, 3.5),
        FixtureConnection(10205, "api.clearsky.app", "198.51.100.30", 443, true, 4.2),
        FixtureConnection(10204, "ads.gemfall.net", "203.0.113.140", 443, false, 5.0),
        FixtureConnection(10209, "img.northwind.shop", "192.0.2.99", 443, false, 6.3),
        FixtureConnection(10201, "tiles.atlasmaps.app", "198.51.100.8", 443, true, 7.1),
        FixtureConnection(10206, "collector.datafunnel.io", "203.0.113.77", 443, false, 8.4),
        FixtureConnection(10214, "stream.soundwave.fm", "198.51.100.64", 443, false, 9.6),
        FixtureConnection(10210, "imap.parcelmail.com", "192.0.2.61", 993, true, 11.0),
        FixtureConnection(10207, "sync.glyphkeys.io", "203.0.113.91", 443, false, 12.5),
        FixtureConnection(10203, "feed.brightline.news", "198.51.100.140", 443, true, 14.2),
        FixtureConnection(10204, "events.playmetric.io", "198.51.100.72", 443, false, 16.0),
        FixtureConnection(10202, "upload.bloomphotos.com", "192.0.2.150", 443, false, 18.3),
        FixtureConnection(10213, "api.ridely.app", "198.51.100.200", 443, true, 21.0),
        FixtureConnection(10206, "ads.brightpixel.net", "203.0.113.24", 443, false, 23.7),
        FixtureConnection(10216, "sync.trailrun.fit", "192.0.2.201", 443, true, 26.4),
        FixtureConnection(10217, "media.wavecast.video", "198.51.100.90", 443, false, 29.0),
    )
