package com.bernaferrari.quietguard.data

data class BooleanPreference(val name: String, val default: Boolean)

object PreferenceKeys {
    val SCREEN_ON = BooleanPreference("screen_on", true)
    val WHITELIST_WIFI = BooleanPreference("whitelist_wifi", false)
    val WHITELIST_OTHER = BooleanPreference("whitelist_other", false)
    val FILTER_UDP = BooleanPreference("filter_udp", true)

    const val ENABLED = "enabled"
    const val FILTER = "filter"
    const val LOCKDOWN = "lockdown"
    const val SHOW_STATS = "show_stats"
    const val APPEARANCE = "appearance"
    const val THEME = "theme"
    const val AUTO_ENABLE = "auto_enable"
    const val LOG = "log"
    const val UPDATE_CHECK = "update_check"

    /** Opt-in GitHub Releases check. Off until the user enables it. */
    const val UPDATE_CHECK_DEFAULT = false

    private val PER_APP_RULE_PREFIXES =
        listOf(
            "wifi",
            "other",
            "apply",
            "screen_wifi",
            "screen_other",
            "roaming",
            "lockdown",
            "notify",
        )

    fun isPerAppRuleKey(key: String): Boolean = PER_APP_RULE_PREFIXES.any { prefix ->
        key.startsWith("${prefix}_") && key.substring(prefix.length + 1).contains('.')
    }
}
