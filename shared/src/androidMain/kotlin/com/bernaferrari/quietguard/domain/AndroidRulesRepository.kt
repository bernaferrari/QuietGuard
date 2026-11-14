package com.bernaferrari.quietguard.domain

import android.app.NotificationManager
import android.content.Context
import com.bernaferrari.quietguard.Rule
import com.bernaferrari.quietguard.data.PreferencesRepository
import com.bernaferrari.quietguard.platform.NetGuardPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class AndroidRulesRepository(
    private val preferencesRepository: PreferencesRepository,
    private val context: Context,
) : RulesRepository {
    @Volatile private var allInstalledRules: List<FirewallRule> = emptyList()

    override suspend fun loadRules(refresh: Boolean): List<FirewallRule> =
        withContext(Dispatchers.IO) {
            val rules = Rule.getRules(all = true, context, refresh)
            allInstalledRules = rules.map { it.toFirewallRule() }
            rules.filter { it.visible }.map { it.toFirewallRule() }
        }

    fun loadAllRules(): List<FirewallRule> =
        Rule.getRules(all = true, context).map { it.toFirewallRule() }

    override fun persistRule(rule: FirewallRule, allRules: List<FirewallRule>) {
        persistRules(
            rule,
            allInstalledRules.map { it.copy() } + allRules,
            wifi = true,
            mobile = true,
            includeOtherSettings = true,
        )
    }

    fun persistNetworkRule(
        rule: FirewallRule,
        allRules: List<FirewallRule>,
        wifi: Boolean,
        mobile: Boolean,
    ) {
        persistRules(rule, allRules, wifi, mobile, includeOtherSettings = false)
    }

    private fun persistRules(
        rule: FirewallRule,
        allRules: List<FirewallRule>,
        wifi: Boolean,
        mobile: Boolean,
        includeOtherSettings: Boolean,
    ) {
        val byPackage = allRules.associateBy { it.packageName }
        val visited = mutableSetOf<String>()
        val affectedUids = mutableSetOf<Int>()
        val changes = mutableMapOf<String, Boolean?>()

        fun write(target: FirewallRule) {
            val packageName = target.packageName ?: return
            if (!visited.add(packageName)) return
            affectedUids += target.uid

            fun store(prefix: String, value: Boolean, default: Boolean) {
                val key = PreferencesRepository.namespaced(prefix, packageName)
                changes[key] = value.takeUnless { it == default }
            }

            if (wifi) {
                target.wifi_blocked = rule.wifi_blocked
                store("wifi", target.wifi_blocked, target.wifi_default)
            }
            if (mobile) {
                target.other_blocked = rule.other_blocked
                store("other", target.other_blocked, target.other_default)
            }
            if (includeOtherSettings) {
                target.apply = rule.apply
                target.screen_wifi = rule.screen_wifi
                target.screen_other = rule.screen_other
                target.roaming = rule.roaming
                target.lockdown = rule.lockdown
                target.notify = rule.notify
                store("apply", target.apply, true)
                store("screen_wifi", target.screen_wifi, target.screen_wifi_default)
                store("screen_other", target.screen_other, target.screen_other_default)
                store("roaming", target.roaming, target.roaming_default)
                store("lockdown", target.lockdown, false)
                store("notify", target.notify, true)
            }
            target.related.forEach { name -> byPackage[name]?.let(::write) }
        }

        write(rule)
        if (changes.isEmpty()) return
        preferencesRepository.putBooleans(changes)
        val notifications = context.getSystemService(NotificationManager::class.java)
        affectedUids.forEach { notifications.cancel(it) }
        NetGuardPlatform.firewall.reload("rule changed", false)
        NetGuardPlatform.widgets.updateFirewall()
    }
}

private fun Rule.toFirewallRule(): FirewallRule =
    FirewallRule(
        uid = uid,
        packageName = packageName,
        name = name,
        icon = icon,
        wifi_blocked = wifi_blocked,
        other_blocked = other_blocked,
        wifi_default = wifi_default,
        other_default = other_default,
        screen_wifi = screen_wifi,
        screen_other = screen_other,
        screen_wifi_default = screen_wifi_default,
        screen_other_default = screen_other_default,
        roaming = roaming,
        roaming_default = roaming_default,
        lockdown = lockdown,
        notify = notify,
        apply = apply,
        system = system,
        internet = internet,
        enabled = enabled,
        installedPackage = pkg,
        related = related?.toList() ?: emptyList(),
    )
