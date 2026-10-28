package com.bernaferrari.quietguard.storecapture

/**
 * Store screenshots use the real apps on the capture emulator's Google Play image, so names and
 * icons come from Android itself. The rules tell one story: messaging, mail and maps stay online,
 * streaming and backups wait for Wi-Fi, and apps with no reason to connect are cut off.
 */
internal data class FixtureApp(
    val packageName: String,
    val wifiBlocked: Boolean = false,
    val mobileBlocked: Boolean = false,
)

internal val fixtureApps: List<FixtureApp> =
    listOf(
        FixtureApp("com.google.android.calendar"),
        FixtureApp("com.android.camera2", wifiBlocked = true, mobileBlocked = true),
        // Wi-Fi only: the first screen shows one app allowed on Wi-Fi and blocked on mobile data.
        FixtureApp("com.android.chrome", mobileBlocked = true),
        FixtureApp("com.google.android.deskclock", wifiBlocked = true, mobileBlocked = true),
        FixtureApp("com.google.android.contacts"),
        FixtureApp("com.google.android.apps.wellbeing", wifiBlocked = true, mobileBlocked = true),
        FixtureApp("com.google.android.apps.docs", mobileBlocked = true),
        FixtureApp("com.google.android.gm"),
        FixtureApp("com.google.android.googlequicksearchbox"),
        FixtureApp("com.google.android.apps.maps"),
        FixtureApp("com.google.android.apps.messaging"),
        FixtureApp("com.google.android.dialer"),
        FixtureApp("com.google.android.apps.photos", mobileBlocked = true),
        FixtureApp("com.android.vending", mobileBlocked = true),
        FixtureApp("com.google.android.youtube", mobileBlocked = true),
        FixtureApp("com.google.android.apps.youtube.music", mobileBlocked = true),
    )

/** One connection attempt, [minutesAgo] before 9:41 (the status bar time in every capture). */
internal data class FixtureConnection(
    val packageName: String,
    val domain: String,
    val address: String,
    val port: Int,
    val allowed: Boolean,
    val minutesAgo: Double,
    val protocol: Int = 6,
)

internal val fixtureConnections: List<FixtureConnection> =
    listOf(
        FixtureConnection("com.android.camera2", "firebaselogging-pa.googleapis.com", "142.250.78.138", 443, false, 0.2),
        FixtureConnection("com.google.android.apps.messaging", "instantmessaging-pa.googleapis.com", "142.250.79.74", 443, true, 0.6),
        FixtureConnection("com.google.android.youtube", "rr4---sn-bg0eznee.googlevideo.com", "173.194.22.137", 443, false, 0.9),
        FixtureConnection("com.google.android.apps.wellbeing", "app-measurement.com", "142.250.219.14", 443, false, 1.4),
        FixtureConnection("com.google.android.gm", "mail.google.com", "142.250.79.69", 443, true, 2.1),
        FixtureConnection("com.google.android.deskclock", "clientservices.googleapis.com", "142.250.78.163", 443, false, 2.8),
        FixtureConnection("com.google.android.apps.maps", "maps.googleapis.com", "142.250.218.106", 443, true, 3.5),
        FixtureConnection("com.android.camera2", "app-measurement.com", "142.250.219.14", 443, false, 4.2),
        FixtureConnection("com.android.chrome", "www.wikipedia.org", "208.80.154.224", 443, true, 5.0),
        FixtureConnection("com.google.android.apps.photos", "photosdata-pa.googleapis.com", "142.250.78.170", 443, false, 6.3),
        FixtureConnection("com.google.android.apps.youtube.music", "music.youtube.com", "142.250.79.46", 443, false, 7.1),
        FixtureConnection("com.google.android.apps.wellbeing", "firebaselogging-pa.googleapis.com", "142.250.78.138", 443, false, 8.4),
        FixtureConnection("com.google.android.calendar", "calendar-pa.googleapis.com", "142.250.219.234", 443, true, 9.6),
        FixtureConnection("com.android.vending", "play-fe.googleapis.com", "142.250.78.202", 443, false, 11.0),
        FixtureConnection("com.google.android.deskclock", "firebaseinstallations.googleapis.com", "142.250.79.10", 443, false, 12.5),
        FixtureConnection("com.google.android.googlequicksearchbox", "www.google.com", "142.250.79.36", 443, true, 14.2),
        FixtureConnection("com.android.camera2", "googleads.g.doubleclick.net", "142.250.218.226", 443, false, 16.0),
        FixtureConnection("com.google.android.apps.docs", "drive.google.com", "142.250.79.78", 443, false, 18.3),
        FixtureConnection("com.google.android.contacts", "people-pa.googleapis.com", "142.250.78.234", 443, true, 21.0),
        FixtureConnection("com.google.android.apps.wellbeing", "app-measurement.com", "142.250.219.14", 443, false, 23.7),
        FixtureConnection("com.google.android.dialer", "dialercallerid-pa.googleapis.com", "142.250.79.106", 443, true, 26.4),
        FixtureConnection("com.google.android.youtube", "youtubei.googleapis.com", "142.250.78.74", 443, false, 29.0),
    )
