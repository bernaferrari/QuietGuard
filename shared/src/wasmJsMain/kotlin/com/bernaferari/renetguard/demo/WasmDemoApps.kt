package com.bernaferari.renetguard.demo

import com.bernaferari.renetguard.platform.AppDisplayInfo

internal data class WasmDemoApp(
    val packageName: String,
    val name: String,
    val uid: Int,
    val wifiBlocked: Boolean = false,
    val otherBlocked: Boolean = false,
    val system: Boolean = false,
)

internal val wasmDemoAppList: List<WasmDemoApp> =
    listOf(
        WasmDemoApp("com.android.chrome", "Chrome", 10101),
        WasmDemoApp("com.google.android.gm", "Gmail", 10102, wifiBlocked = true),
        WasmDemoApp("com.spotify.music", "Spotify", 10103, otherBlocked = true),
        WasmDemoApp("com.instagram.android", "Instagram", 10104, wifiBlocked = true, otherBlocked = true),
        WasmDemoApp("com.whatsapp", "WhatsApp", 10105),
        WasmDemoApp("com.google.android.youtube", "YouTube", 10106),
        WasmDemoApp("com.twitter.android", "X", 10107, wifiBlocked = true),
        WasmDemoApp("com.facebook.katana", "Facebook", 10108, wifiBlocked = true, otherBlocked = true),
        WasmDemoApp("com.google.android.apps.maps", "Maps", 10109),
        WasmDemoApp("com.amazon.mShop.android.shopping", "Amazon", 10110, otherBlocked = true),
        WasmDemoApp("com.netflix.mediaclient", "Netflix", 10111),
        WasmDemoApp("com.discord", "Discord", 10112, wifiBlocked = true),
        WasmDemoApp("com.reddit.frontpage", "Reddit", 10113, otherBlocked = true),
        WasmDemoApp("org.telegram.messenger", "Telegram", 10114),
        WasmDemoApp("com.zhiliaoapp.musically", "TikTok", 10115, wifiBlocked = true),
        WasmDemoApp("com.Slack", "Slack", 10116),
        WasmDemoApp("com.linkedin.android", "LinkedIn", 10117, wifiBlocked = true),
        WasmDemoApp("org.thoughtcrime.securesms", "Signal", 10118),
        WasmDemoApp("com.google.android.apps.photos", "Photos", 10119),
        WasmDemoApp("com.google.android.apps.docs", "Drive", 10120, otherBlocked = true),
        WasmDemoApp("com.ubercab", "Uber", 10121, wifiBlocked = true),
        WasmDemoApp("com.chase.sig.android", "Chase", 10122, wifiBlocked = true, otherBlocked = true),
        WasmDemoApp("com.android.vending", "Play Store", 10001, system = true),
        WasmDemoApp("com.google.android.gms", "Google Play services", 10002, system = true),
    )

internal val wasmDemoApps: Map<Int, AppDisplayInfo> =
    wasmDemoAppList.associate { app ->
        app.uid to AppDisplayInfo(app.name, app.packageName)
    }

internal val wasmSystemPackages: Set<String> =
    wasmDemoAppList
        .filter { it.system }
        .map { it.packageName }
        .toSet()