package com.bernaferrari.quietguard.storecapture

import com.bernaferrari.quietguard.ApplicationEx
import com.bernaferrari.quietguard.DatabaseHelper
import com.bernaferrari.quietguard.Packet
import com.bernaferrari.quietguard.data.PreferencesRepository
import com.bernaferrari.quietguard.domain.FirewallRule
import com.bernaferrari.quietguard.domain.RulesRepository
import com.bernaferrari.quietguard.platform.FirewallBridge
import com.bernaferrari.quietguard.platform.NetGuardPlatform
import com.bernaferrari.quietguard.platform.ProFeaturesBridge
import com.bernaferrari.quietguard.platform.UiHelperBridge
import com.bernaferrari.quietguard.platform.WidgetBridge
import com.bernaferrari.quietguard.ui.components.AppIconOverrides
import androidx.work.WorkManager
import org.koin.android.ext.android.getKoin
import org.koin.dsl.module
import java.util.Calendar

/**
 * Store-capture host: the real app UI over fictional apps and traffic. The firewall bridge is
 * inert, so the VPN never starts; the app has no INTERNET permission in this build.
 */
class StoreCaptureApplication : ApplicationEx() {
    override fun onCreate() {
        super.onCreate()
        // Housekeeping would start the (removed) VPN service; nothing runs in the background here.
        WorkManager.getInstance(this).cancelAllWork()
        val koin = getKoin()
        val preferences = koin.get<PreferencesRepository>()
        koin.loadModules(
            listOf(module { single<RulesRepository> { FixtureRulesRepository() } }),
            allowOverride = true,
        )
        installFixtureBridges()
        AppIconOverrides.lookup = { fixtureByPackage[it]?.art }

        preferences.putBoolean("enabled", true)
        preferences.putBoolean("log", true)
        preferences.putBoolean("filter", true)
        preferences.putBoolean("update_check", false)
        // Apps without their own rule may connect; the fixture rules carry the blocks.
        preferences.putBoolean("whitelist_wifi", false)
        preferences.putBoolean("whitelist_other", false)
        seedTraffic()
    }

    private fun installFixtureBridges() {
        val realHelpers = NetGuardPlatform.uiHelpers
        NetGuardPlatform.firewall =
            object : FirewallBridge {
                override fun reload(reason: String, userInteraction: Boolean) {}
                override fun reloadStats(reason: String) {}
                override fun checkForUpdateNow(reason: String) {}
                override val updateCheckAction = "$packageName.STORE_CAPTURE_UPDATE"
                override val extraUpdateCheckStatus = "status"
                override val extraUpdateCheckVersion = "version"
            }
        NetGuardPlatform.widgets =
            object : WidgetBridge {
                override fun updateFirewall() {}
                override fun updateAll() {}
                override fun updateLockdown() {}
            }
        val realPro = NetGuardPlatform.proFeatures
        NetGuardPlatform.proFeatures =
            object : ProFeaturesBridge {
                override val logSku = realPro.logSku
                override fun isPurchased(sku: String) = true
                override fun openProScreen() {}
            }
        NetGuardPlatform.uiHelpers =
            object : UiHelperBridge by realHelpers {
                override fun getApplicationNames(uid: Int): List<String> =
                    fixtureByUid[uid]?.let { listOf(it.name) } ?: realHelpers.getApplicationNames(uid)

                override fun getPackageForUid(uid: Int): String? =
                    fixtureByUid[uid]?.packageName ?: realHelpers.getPackageForUid(uid)
            }
    }

    /** Replaces the log and per-app access history with the fixture story, ending at 9:41 today. */
    private fun seedTraffic() {
        val db = DatabaseHelper.getInstance(this)
        db.clearLog(-1)
        db.clearAccess()
        val end =
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 41)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        fixtureConnections.sortedByDescending { it.minutesAgo }.forEach { c ->
            val packet =
                Packet(
                    time = end - (c.minutesAgo * 60_000).toLong(),
                    version = 4,
                    protocol = c.protocol,
                    flags = "S",
                    saddr = "10.1.10.1",
                    sport = 40000 + c.uid % 1000,
                    daddr = c.address,
                    dport = c.port,
                    uid = c.uid,
                    allowed = c.allowed,
                )
            db.insertLog(packet, c.domain, 1, false)
            db.updateAccess(packet, c.domain, -1)
        }
    }
}

private class FixtureRulesRepository : RulesRepository {
    private var rules: List<FirewallRule>? = null

    override suspend fun loadRules(refresh: Boolean): List<FirewallRule> =
        rules ?: fixtureApps.map { app ->
            FirewallRule(
                uid = app.uid,
                packageName = app.packageName,
                name = app.name,
                wifi_blocked = app.wifiBlocked,
                other_blocked = app.mobileBlocked,
                screen_wifi_default = false,
                screen_other_default = false,
                internet = true,
                enabled = true,
                system = app.system,
            )
        }.also { rules = it }

    override fun persistRule(rule: FirewallRule, allRules: List<FirewallRule>) {
        rules = allRules
    }
}
