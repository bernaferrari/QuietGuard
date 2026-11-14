package com.bernaferrari.quietguard

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.pm.PackageInfoCompat
import com.bernaferrari.quietguard.data.preferences
import com.bernaferrari.quietguard.netguard.isNumericAddress as nativeIsNumericAddress
import com.bernaferrari.quietguard.shared.R
import com.bernaferrari.quietguard.ui.theme.THEME_DEFAULT
import com.bernaferrari.quietguard.ui.theme.themePrimaryColor
import java.io.File
import java.util.Locale

object Util {
    private const val TAG = "NetGuard.Util"

    // Roam like at home
    private val listEU =
        listOf(
            "AT", // Austria
            "BE", // Belgium
            "BG", // Bulgaria
            "HR", // Croatia
            "CY", // Cyprus
            "CZ", // Czech Republic
            "DK", // Denmark
            "EE", // Estonia
            "FI", // Finland
            "FR", // France
            "DE", // Germany
            "GR", // Greece
            "HU", // Hungary
            "IS", // Iceland
            "IE", // Ireland
            "IT", // Italy
            "LV", // Latvia
            "LI", // Liechtenstein
            "LT", // Lithuania
            "LU", // Luxembourg
            "MT", // Malta
            "NL", // Netherlands
            "NO", // Norway
            "PL", // Poland
            "PT", // Portugal
            "RO", // Romania
            "SK", // Slovakia
            "SI", // Slovenia
            "ES", // Spain
            "SE", // Sweden
        )

    @JvmStatic
    fun getSelfVersionName(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: ""
        } catch (ex: PackageManager.NameNotFoundException) {
            ex.toString()
        }
    }

    @JvmStatic
    fun getSelfVersionCode(context: Context): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            PackageInfoCompat.getLongVersionCode(pInfo).toInt()
        } catch (_: PackageManager.NameNotFoundException) {
            -1
        }
    }

    @JvmStatic
    fun isConnected(context: Context): Boolean {
        val cm =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
                ?: return false

        val activeNetwork = cm.activeNetwork
        if (activeNetwork != null) {
            val caps = cm.getNetworkCapabilities(activeNetwork)
            if (
                caps != null &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            ) {
                return true
            }
        }

        return false
    }

    @JvmStatic
    fun isWifiActive(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val active = cm?.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    @JvmStatic
    fun isMeteredNetwork(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        return cm?.isActiveNetworkMetered == true
    }

    @JvmStatic
    fun getWifiSSID(context: Context): String {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            val active = cm?.activeNetwork
            val caps = if (active != null) cm.getNetworkCapabilities(active) else null
            val info = caps?.transportInfo as? android.net.wifi.WifiInfo
            val ssid = info?.ssid?.trim('"')
            if (!ssid.isNullOrBlank()) {
                return ssid
            }
        }

        @Suppress("DEPRECATION") val legacy = wm?.connectionInfo?.ssid
        return legacy?.trim('"') ?: "NULL"
    }

    @JvmStatic
    fun getNetworkType(context: Context): Int {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        val active = cm?.activeNetwork
        val caps = if (active != null) cm.getNetworkCapabilities(active) else null
        val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        return if (isCellular) {
            try {
                tm?.dataNetworkType ?: TelephonyManager.NETWORK_TYPE_UNKNOWN
            } catch (_: SecurityException) {
                TelephonyManager.NETWORK_TYPE_UNKNOWN
            }
        } else {
            TelephonyManager.NETWORK_TYPE_UNKNOWN
        }
    }

    @JvmStatic
    fun getNetworkGeneration(context: Context): String? {
        val type = getNetworkType(context)
        return if (type == TelephonyManager.NETWORK_TYPE_UNKNOWN) null
        else getNetworkGeneration(type)
    }

    @JvmStatic
    fun isRoaming(context: Context): Boolean {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        if (tm != null) {
            return tm.isNetworkRoaming
        }
        return false
    }

    @JvmStatic
    fun isNational(context: Context): Boolean {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
            tm != null && tm.simCountryIso != null && tm.simCountryIso == tm.networkCountryIso
        } catch (_: Throwable) {
            false
        }
    }

    @JvmStatic
    fun isEU(context: Context): Boolean {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
            tm != null && isEU(tm.simCountryIso) && isEU(tm.networkCountryIso)
        } catch (_: Throwable) {
            false
        }
    }

    @JvmStatic
    fun isEU(country: String?): Boolean {
        return country != null && listEU.contains(country.uppercase(Locale.ROOT))
    }

    @JvmStatic
    fun getPrivateDnsSpecifier(context: Context): String? {
        val dnsMode = Settings.Global.getString(context.contentResolver, "private_dns_mode")
        return if ("hostname" == dnsMode) {
            Settings.Global.getString(context.contentResolver, "private_dns_specifier")
        } else {
            null
        }
    }

    @JvmStatic
    @Suppress("DEPRECATION")
    fun getNetworkGeneration(networkType: Int): String {
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_1xRTT,
            TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_IDEN,
            TelephonyManager.NETWORK_TYPE_GSM -> "2G"

            TelephonyManager.NETWORK_TYPE_EHRPD,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_EVDO_B,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "3G"

            TelephonyManager.NETWORK_TYPE_LTE,
            TelephonyManager.NETWORK_TYPE_IWLAN -> "4G"

            else -> "?G"
        }
    }

    @JvmStatic
    fun hasPhoneStatePermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED

    @JvmStatic
    fun getDefaultDNS(context: Context): List<String> {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val network = connectivity.activeNetwork ?: return emptyList()
        return connectivity.getLinkProperties(network)?.dnsServers.orEmpty().mapNotNull {
            it.hostAddress?.substringBefore('%')
        }
    }

    @JvmStatic fun isNumericAddress(ip: String): Boolean = nativeIsNumericAddress(ip)

    @JvmStatic
    fun isInteractive(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
        return pm?.isInteractive == true
    }

    @JvmStatic
    fun isSystem(uid: Int, context: Context): Boolean {
        val pm = context.packageManager
        val pkgs = pm.getPackagesForUid(uid)
        if (pkgs != null) {
            for (pkg in pkgs) {
                if (isSystem(pkg, context)) {
                    return true
                }
            }
        }
        return false
    }

    @JvmStatic
    @Suppress("DEPRECATION")
    fun isSystem(packageName: String, context: Context): Boolean {
        return try {
            val pm = context.packageManager
            val info = pm.getPackageInfo(packageName, 0)
            val flags = info.applicationInfo?.flags ?: 0
            flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    @JvmStatic
    fun hasInternet(packageName: String, context: Context): Boolean {
        val pm = context.packageManager
        return pm.checkPermission("android.permission.INTERNET", packageName) ==
            PackageManager.PERMISSION_GRANTED
    }

    @JvmStatic
    fun hasInternet(uid: Int, context: Context): Boolean {
        val pm = context.packageManager
        val pkgs = pm.getPackagesForUid(uid)
        if (pkgs != null) {
            for (pkg in pkgs) {
                if (hasInternet(pkg, context)) {
                    return true
                }
            }
        }
        return false
    }

    @JvmStatic
    fun isEnabled(info: PackageInfo, context: Context): Boolean {
        val setting =
            try {
                val pm = context.packageManager
                pm.getApplicationEnabledSetting(info.packageName)
            } catch (ex: IllegalArgumentException) {
                Log.w(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
            }
        return if (setting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
            info.applicationInfo?.enabled ?: false
        } else {
            setting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
    }

    @JvmStatic
    fun getApplicationNames(uid: Int, context: Context): List<String> {
        val listResult = mutableListOf<String>()
        when (uid) {
            0 -> listResult.add(context.getString(R.string.title_root))
            1013 -> listResult.add(context.getString(R.string.title_mediaserver))
            9999 -> listResult.add(context.getString(R.string.title_nobody))
            else -> {
                val pm = context.packageManager
                val pkgs = pm.getPackagesForUid(uid)
                if (pkgs != null) {
                    for (pkg in pkgs) {
                        try {
                            val info = pm.getApplicationInfo(pkg, 0)
                            val name = pm.getApplicationLabel(info).toString()
                            if (!TextUtils.isEmpty(name)) {
                                listResult.add(name)
                            }
                        } catch (_: PackageManager.NameNotFoundException) {}
                    }
                }
                listResult.sort()
            }
        }
        return listResult
    }

    @JvmStatic
    fun canFilter(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true
        }

        // https://android-review.googlesource.com/#/c/206710/1/untrusted_app.te
        val tcp = File("/proc/net/tcp")
        val tcp6 = File("/proc/net/tcp6")
        try {
            if (tcp.exists() && tcp.canRead()) {
                return true
            }
        } catch (_: SecurityException) {}
        return try {
            tcp6.exists() && tcp6.canRead()
        } catch (_: SecurityException) {
            false
        }
    }

    @JvmStatic
    fun isPlayStoreInstall(context: Context): Boolean {
        if (BuildConfig.PLAY_STORE_RELEASE) {
            return true
        }
        return try {
            val pm = context.packageManager
            val installer =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    pm.getInstallSourceInfo(context.packageName).installingPackageName
                } else {
                    @Suppress("DEPRECATION") pm.getInstallerPackageName(context.packageName)
                }
            "com.android.vending" == installer
        } catch (ex: Throwable) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            false
        }
    }

    @JvmStatic
    fun setTheme(context: Context) {
        val theme = context.preferences().getString("theme", THEME_DEFAULT)

        if (context is Activity) {
            setTaskColor(context, theme)
        }
    }

    private fun setTaskColor(context: Context, theme: String?) {
        val defaultColor = themePrimaryColor(theme)
        val activity = context as Activity
        val description =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityManager.TaskDescription.Builder().setPrimaryColor(defaultColor).build()
            } else {
                @Suppress("DEPRECATION") ActivityManager.TaskDescription(null, null, defaultColor)
            }
        activity.setTaskDescription(description)
    }

    @JvmStatic
    fun dips2pixels(dips: Int, context: Context): Int {
        return Math.round(dips * context.resources.displayMetrics.density + 0.5f)
    }

    @JvmStatic
    fun getProtocolName(protocol: Int, version: Int, brief: Boolean): String {
        // https://en.wikipedia.org/wiki/List_of_IP_protocol_numbers
        var p: String? = null
        var b: String? = null
        when (protocol) {
            0 -> {
                p = "HOPO"
                b = "H"
            }

            2 -> {
                p = "IGMP"
                b = "G"
            }

            1,
            58 -> {
                p = "ICMP"
                b = "I"
            }

            6 -> {
                p = "TCP"
                b = "T"
            }

            17 -> {
                p = "UDP"
                b = "U"
            }

            50 -> {
                p = "ESP"
                b = "E"
            }
        }
        if (p == null) {
            return "$protocol/$version"
        }
        return (if (brief) b else p) + if (version > 0) version.toString() else ""
    }

    @JvmStatic
    fun logExtras(intent: Intent?) {
        intent?.extras?.let { logBundle(it) }
    }

    @JvmStatic
    @Suppress("DEPRECATION")
    fun logBundle(data: Bundle?) {
        if (data != null) {
            val keys = data.keySet()
            val stringBuilder = StringBuilder()
            for (key in keys) {
                val value = data[key]
                stringBuilder
                    .append(key)
                    .append("=")
                    .append(value)
                    .append(if (value == null) "" else " (" + value.javaClass.simpleName + ")")
                    .append("\r\n")
            }
            Log.d(TAG, stringBuilder.toString())
        }
    }

    @JvmStatic
    fun canNotify(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            true
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }
    }
}
