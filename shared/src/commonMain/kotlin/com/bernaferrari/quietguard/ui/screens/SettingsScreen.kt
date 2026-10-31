package com.bernaferrari.quietguard.ui.screens

import com.bernaferrari.quietguard.ui.icons.MaterialSymbols

import org.jetbrains.compose.resources.stringResource
import com.bernaferrari.quietguard.generated.resources.Res
import com.bernaferrari.quietguard.generated.resources.action_back
import com.bernaferrari.quietguard.generated.resources.menu_about
import com.bernaferrari.quietguard.generated.resources.menu_ok
import com.bernaferrari.quietguard.generated.resources.setting_access
import com.bernaferrari.quietguard.generated.resources.setting_auto
import com.bernaferrari.quietguard.generated.resources.setting_delay
import com.bernaferrari.quietguard.generated.resources.setting_dns
import com.bernaferrari.quietguard.generated.resources.setting_dns2
import com.bernaferrari.quietguard.generated.resources.setting_pcap_file_size
import com.bernaferrari.quietguard.generated.resources.setting_pcap_record_size
import com.bernaferrari.quietguard.generated.resources.setting_rcode
import com.bernaferrari.quietguard.generated.resources.setting_socks5_addr
import com.bernaferrari.quietguard.generated.resources.setting_socks5_password
import com.bernaferrari.quietguard.generated.resources.setting_socks5_port
import com.bernaferrari.quietguard.generated.resources.setting_socks5_username
import com.bernaferrari.quietguard.generated.resources.setting_stats_frequency
import com.bernaferrari.quietguard.generated.resources.setting_stats_samples
import com.bernaferrari.quietguard.generated.resources.setting_ttl
import com.bernaferrari.quietguard.generated.resources.setting_update_result_available
import com.bernaferrari.quietguard.generated.resources.setting_validate
import com.bernaferrari.quietguard.generated.resources.setting_vpn4
import com.bernaferrari.quietguard.generated.resources.setting_vpn6
import com.bernaferrari.quietguard.generated.resources.setting_watchdog
import com.bernaferrari.quietguard.generated.resources.setting_wifi_home
import com.bernaferrari.quietguard.generated.resources.settings_group_mobile
import com.bernaferrari.quietguard.generated.resources.settings_group_screen
import com.bernaferrari.quietguard.generated.resources.settings_group_local_network
import com.bernaferrari.quietguard.generated.resources.settings_group_automation
import com.bernaferrari.quietguard.generated.resources.settings_group_metered
import com.bernaferrari.quietguard.generated.resources.settings_group_logging
import com.bernaferrari.quietguard.generated.resources.settings_group_filtering
import com.bernaferrari.quietguard.generated.resources.settings_group_protection
import com.bernaferrari.quietguard.generated.resources.settings_group_statistics
import com.bernaferrari.quietguard.generated.resources.setting_appearance_auto
import com.bernaferrari.quietguard.generated.resources.setting_appearance_dark
import com.bernaferrari.quietguard.generated.resources.setting_appearance_light
import com.bernaferrari.quietguard.generated.resources.setting_call
import com.bernaferrari.quietguard.generated.resources.setting_eu_roaming
import com.bernaferrari.quietguard.generated.resources.setting_filter
import com.bernaferrari.quietguard.generated.resources.setting_filter_udp
import com.bernaferrari.quietguard.generated.resources.setting_forwarding
import com.bernaferrari.quietguard.generated.resources.setting_hosts
import com.bernaferrari.quietguard.generated.resources.setting_hosts_append
import com.bernaferrari.quietguard.generated.resources.hosts_import_action
import com.bernaferrari.quietguard.generated.resources.hosts_import_done
import com.bernaferrari.quietguard.generated.resources.hosts_import_failed
import com.bernaferrari.quietguard.platform.rememberHostsFileImporter
import com.bernaferrari.quietguard.generated.resources.setting_hosts_download
import com.bernaferrari.quietguard.generated.resources.setting_hosts_url
import com.bernaferrari.quietguard.generated.resources.setting_ip6
import com.bernaferrari.quietguard.generated.resources.setting_lan
import com.bernaferrari.quietguard.generated.resources.setting_lockdown
import com.bernaferrari.quietguard.generated.resources.setting_lockdown_other
import com.bernaferrari.quietguard.generated.resources.setting_lockdown_wifi
import com.bernaferrari.quietguard.generated.resources.setting_log_app
import com.bernaferrari.quietguard.generated.resources.setting_log_retention_days
import com.bernaferrari.quietguard.generated.resources.setting_malware
import com.bernaferrari.quietguard.generated.resources.setting_metered
import com.bernaferrari.quietguard.generated.resources.setting_metered_2g
import com.bernaferrari.quietguard.generated.resources.setting_metered_3g
import com.bernaferrari.quietguard.generated.resources.setting_metered_4g
import com.bernaferrari.quietguard.generated.resources.setting_national_roaming
import com.bernaferrari.quietguard.generated.resources.setting_pcap
import com.bernaferrari.quietguard.generated.resources.setting_screen_on
import com.bernaferrari.quietguard.generated.resources.setting_screen_other
import com.bernaferrari.quietguard.generated.resources.setting_screen_wifi
import com.bernaferrari.quietguard.generated.resources.setting_section_advanced
import com.bernaferrari.quietguard.generated.resources.setting_section_appearance
import com.bernaferrari.quietguard.generated.resources.setting_section_background
import com.bernaferrari.quietguard.generated.resources.setting_section_diagnostics
import com.bernaferrari.quietguard.generated.resources.setting_section_dns
import com.bernaferrari.quietguard.generated.resources.setting_section_firewall
import com.bernaferrari.quietguard.generated.resources.setting_section_hosts
import com.bernaferrari.quietguard.generated.resources.setting_section_network
import com.bernaferrari.quietguard.generated.resources.setting_section_proxy
import com.bernaferrari.quietguard.generated.resources.setting_section_vpn
import com.bernaferrari.quietguard.generated.resources.setting_show_resolved
import com.bernaferrari.quietguard.generated.resources.setting_socks5_enabled
import com.bernaferrari.quietguard.generated.resources.setting_stats
import com.bernaferrari.quietguard.generated.resources.setting_stats_top
import com.bernaferrari.quietguard.generated.resources.setting_subnet
import com.bernaferrari.quietguard.generated.resources.setting_system
import com.bernaferrari.quietguard.generated.resources.setting_tethering
import com.bernaferrari.quietguard.generated.resources.setting_track_usage
import com.bernaferrari.quietguard.generated.resources.setting_update
import com.bernaferrari.quietguard.generated.resources.setting_update_checking
import com.bernaferrari.quietguard.generated.resources.setting_update_now
import com.bernaferrari.quietguard.generated.resources.setting_update_result_available_unknown
import com.bernaferrari.quietguard.generated.resources.setting_update_result_failed
import com.bernaferrari.quietguard.generated.resources.setting_update_result_unavailable
import com.bernaferrari.quietguard.generated.resources.setting_update_result_up_to_date
import com.bernaferrari.quietguard.generated.resources.setting_use_hosts
import com.bernaferrari.quietguard.generated.resources.setting_whitelist_roaming
import com.bernaferrari.quietguard.generated.resources.summary_block_domains
import com.bernaferrari.quietguard.generated.resources.summary_log_retention_days
import com.bernaferrari.quietguard.generated.resources.tooltip_filter
import com.bernaferrari.quietguard.generated.resources.tooltip_lockdown
import com.bernaferrari.quietguard.generated.resources.tooltip_pcap
import com.bernaferrari.quietguard.generated.resources.tooltip_rcode
import com.bernaferrari.quietguard.generated.resources.tooltip_ttl
import com.bernaferrari.quietguard.generated.resources.ui_settings_title
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.bernaferrari.quietguard.platform.NetGuardPlatform
import com.bernaferrari.quietguard.ui.screens.vm.SettingsViewModel
import com.bernaferrari.quietguard.ui.SettingsSection
import org.koin.compose.viewmodel.koinViewModel
import com.bernaferrari.quietguard.ui.components.groupItemShape
import com.bernaferrari.quietguard.ui.components.ColorStylePicker
import com.bernaferrari.quietguard.ui.theme.ColorStyle
import com.bernaferrari.quietguard.ui.theme.themeSeedColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.heightIn
import com.bernaferrari.quietguard.ui.theme.DynamicThemeSwatchColor
import com.bernaferrari.quietguard.ui.theme.THEME_INITIAL
import com.bernaferrari.quietguard.ui.theme.platformDynamicColorScheme
import com.bernaferrari.quietguard.ui.theme.resolveThemeName
import com.bernaferrari.quietguard.ui.theme.spacing
import com.bernaferrari.quietguard.ui.theme.themeSwatchOptions

import com.bernaferrari.quietguard.ui.icons.Icon
import androidx.compose.material3.Icon as Material3Icon

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    section: SettingsSection? = null,
    onBack: () -> Unit = {},
    onOpenSection: (SettingsSection) -> Unit = {},
    onOpenDns: () -> Unit,
    onOpenForwarding: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val viewModel: SettingsViewModel = koinViewModel()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    fun bool(key: String, default: Boolean) = prefs[booleanPreferencesKey(key)] ?: default
    fun str(key: String, default: String) = prefs[stringPreferencesKey(key)] ?: default
    fun strSet(key: String, default: Set<String>) = prefs[stringSetPreferencesKey(key)] ?: default
    fun updateFlag(
        key: String,
        value: Boolean,
        reload: Boolean = false,
        reloadStats: Boolean = false,
        updateWidgets: (() -> Unit)? = null,
    ) {
        viewModel.putBoolean(key, value)
        if (reload) {
            NetGuardPlatform.firewall.reload("settings", false)
        }
        if (reloadStats) {
            NetGuardPlatform.firewall.reloadStats("settings")
        }
        updateWidgets?.invoke()
    }

    var hostImportResult by remember { mutableStateOf<Boolean?>(null) }
    val importHostsFile = rememberHostsFileImporter { hostImportResult = it }
    var updateCheckInProgress by remember { mutableStateOf(false) }
    var updateCheckStatus by remember { mutableStateOf<String?>(null) }
    var updateCheckVersion by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val dispose =
            viewModel.registerUpdateCheck { status, version ->
                updateCheckInProgress = false
                updateCheckStatus = status
                updateCheckVersion = version
            }
        onDispose { dispose() }
    }

    val appearanceMode =
        when (str("appearance", "")) {
            "light", "dark", "auto" -> str("appearance", "auto")
            else ->
                if (prefs.asMap().containsKey(booleanPreferencesKey("dark_theme"))) {
                    if (bool("dark_theme", false)) "dark" else "light"
                } else {
                    "auto"
                }
        }

    fun updateAppearance(mode: String) {
        viewModel.putString("appearance", mode)
        when (mode) {
            "auto" -> viewModel.removeString("dark_theme")
            "dark" -> viewModel.putBoolean("dark_theme", true)
            "light" -> viewModel.putBoolean("dark_theme", false)
        }
        NetGuardPlatform.widgets.updateAll()
    }

    val screenTitle =
        when (section) {
            null -> stringResource(Res.string.ui_settings_title)
            SettingsSection.Firewall -> stringResource(Res.string.setting_section_firewall)
            SettingsSection.Advanced -> stringResource(Res.string.setting_section_advanced)
            SettingsSection.Hosts -> stringResource(Res.string.setting_section_hosts)
            SettingsSection.Network -> stringResource(Res.string.setting_section_network)
            SettingsSection.Background -> stringResource(Res.string.setting_section_background)
            SettingsSection.About -> stringResource(Res.string.menu_about)
        }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (section == null) {
                LargeFlexibleTopAppBar(
                    title = {
                        Text(
                            text = screenTitle,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            } else {
                MediumFlexibleTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                icon = MaterialSymbols.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.action_back),
                            )
                        }
                    },
                    title = {
                        Text(
                            text = screenTitle,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = spacing.large, vertical = spacing.default),
            verticalArrangement = Arrangement.spacedBy(spacing.extraLarge),
        ) {

            if (section == null) {
                val currentTheme = resolveThemeName(str("theme", THEME_INITIAL))
                val themeChoices = themeSwatchOptions()
                val dynamicSwatchColor =
                    platformDynamicColorScheme(isDark = false)?.primary ?: DynamicThemeSwatchColor

                QuickFirewallControls(
                    wifiAllowed = !bool("whitelist_wifi", true),
                    mobileAllowed = !bool("whitelist_other", true),
                    onToggleWifi = {
                        updateFlag(
                            "whitelist_wifi",
                            !bool("whitelist_wifi", true),
                            reload = true,
                        )
                    },
                    onToggleMobile = {
                        updateFlag(
                            "whitelist_other",
                            !bool("whitelist_other", true),
                            reload = true,
                        )
                    },
                )

                SettingsDirectory(onOpenSection = onOpenSection)

                SettingsGroup(title = stringResource(Res.string.setting_section_appearance)) {
                    item { first, last ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = groupItemShape(first, last),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            val modeOptions = listOf(
                                Triple(
                                    "light",
                                    stringResource(Res.string.setting_appearance_light),
                                    MaterialSymbols.Outlined.LightMode,
                                ),
                                Triple(
                                    "dark",
                                    stringResource(Res.string.setting_appearance_dark),
                                    MaterialSymbols.Outlined.DarkMode,
                                ),
                                Triple(
                                    "auto",
                                    stringResource(Res.string.setting_appearance_auto),
                                    MaterialSymbols.Outlined.BrightnessAuto,
                                ),
                            )
                            val selectedIndex =
                                modeOptions.indexOfFirst { it.first == appearanceMode }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(spacing.default),
                                horizontalArrangement = Arrangement.spacedBy(
                                    ButtonGroupDefaults.ConnectedSpaceBetween,
                                ),
                            ) {
                                modeOptions.forEachIndexed { index, (mode, label, icon) ->
                                    val selected = index == selectedIndex
                                    ToggleButton(
                                        checked = selected,
                                        onCheckedChange = { isChecked ->
                                            if (isChecked) updateAppearance(mode)
                                        },
                                        shapes = when (index) {
                                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                            modeOptions.lastIndex ->
                                                ButtonGroupDefaults.connectedTrailingButtonShapes()

                                            else ->
                                                ButtonGroupDefaults.connectedMiddleButtonShapes()
                                        },
                                        // The family standard: the chosen mode solid in primary with a check,
                                        // the others raised off the card.
                                        colors = ToggleButtonDefaults.colors(
                                            checkedContainerColor = MaterialTheme.colorScheme.primary,
                                            checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            // 48dp, like the style row below and the touch target guideline.
                                            .heightIn(min = 48.dp)
                                            .semantics { role = Role.RadioButton },
                                    ) {
                                        Icon(
                                            icon =
                                                if (selected) MaterialSymbols.Filled.Check else icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(ToggleButtonDefaults.IconSize),
                                        )
                                        Spacer(
                                            modifier =
                                                Modifier.size(ToggleButtonDefaults.IconSpacing),
                                        )
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { first, last ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = groupItemShape(first, last),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(spacing.default),
                                horizontalArrangement = Arrangement.spacedBy(
                                    0.dp,
                                    Alignment.CenterHorizontally,
                                ),
                                verticalArrangement = Arrangement.spacedBy(0.dp),
                            ) {
                                themeChoices.forEach { option ->
                                    val theme = option.id
                                    ThemeSwatch(
                                        theme = theme,
                                        seedColor = option.seedColor,
                                        isSelected = currentTheme == theme,
                                        isEnabled = true,
                                        dynamicColor = dynamicSwatchColor,
                                        onClick = {
                                            viewModel.putString("theme", theme)
                                            NetGuardPlatform.widgets.updateAll()
                                        },
                                    )
                                }
                            }
                        }
                    }
                    // The style of the chosen color, as wordless gradient segments under the swatches. Only for
                    // the app's own colors: wallpaper colors come as Android gives them.
                    if (currentTheme != "dynamic") item { first, last ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = groupItemShape(first, last),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            ColorStylePicker(
                                selected = ColorStyle.from(str("color_style", "")),
                                seedColor = if (currentTheme == "dynamic") dynamicSwatchColor else themeSeedColor(currentTheme),
                                dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f,
                                onSelect = {
                                    viewModel.putString("color_style", it.id)
                                    NetGuardPlatform.widgets.updateAll()
                                },
                                modifier = Modifier.fillMaxWidth().padding(spacing.default),
                            )
                        }
                    }
                }
            }

            // Firewall Section
            if (section == SettingsSection.Firewall) {
                SettingsGroup(title = stringResource(Res.string.settings_group_mobile)) {
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_whitelist_roaming),
                            checked = bool("whitelist_roaming", true),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("whitelist_roaming", it, reload = true) }
                    }
                }

                SettingsGroup(title = stringResource(Res.string.settings_group_screen)) {
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_screen_on),
                            checked = bool("screen_on", true),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("screen_on", it, reload = true) }
                    }
                    item { first, last ->
                        SettingTogglePairRow(
                            firstTitle = stringResource(Res.string.setting_screen_wifi),
                            firstChecked = bool("screen_wifi", false),
                            onFirstCheckedChange = { updateFlag("screen_wifi", it, reload = true) },
                            secondTitle = stringResource(Res.string.setting_screen_other),
                            secondChecked = bool("screen_other", false),
                            onSecondCheckedChange = { updateFlag("screen_other", it, reload = true) },
                            isFirst = first,
                            isLast = last,
                        )
                    }
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(Res.string.setting_delay, SettingValuePlaceholder),
                            value = str("screen_delay", "0"),
                            keyboardType = KeyboardType.Number,
                            isFirst = first,
                            isLast = last,
                            onValueChange = { viewModel.putString("screen_delay", it) },
                        )
                    }
                }

                SettingsGroup(title = stringResource(Res.string.settings_group_local_network)) {
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_subnet),
                            checked = bool("subnet", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("subnet", it, reload = true) }
                    }
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_tethering),
                            checked = bool("tethering", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("tethering", it, reload = true) }
                    }
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_lan),
                            checked = bool("lan", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("lan", it, reload = true) }
                    }
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_ip6),
                            checked = bool("ip6", true),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("ip6", it, reload = true) }
                    }
                }

                SettingsGroup(title = stringResource(Res.string.settings_group_automation)) {
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(Res.string.setting_auto, SettingValuePlaceholder),
                            value = str("auto_enable", "0"),
                            keyboardType = KeyboardType.Number,
                            isFirst = first,
                            isLast = last,
                            onValueChange = { viewModel.putString("auto_enable", it) },
                        )
                    }
                    item { first, last ->
                        // Keep the raw text locally: re-deriving it from the saved set would
                        // strip a trailing comma and make it impossible to type a second SSID.
                        var homesText by remember {
                            mutableStateOf(strSet("wifi_homes", emptySet()).joinToString(", "))
                        }
                        SettingTextRow(
                            title = stringResource(Res.string.setting_wifi_home, SettingValuePlaceholder),
                            value = homesText,
                            isFirst = first,
                            isLast = last,
                            onValueChange = { value ->
                                homesText = value
                                val set =
                                    value.split(",").map { it.trim() }
                                        .filter { it.isNotEmpty() }.toSet()
                                viewModel.putStringSet("wifi_homes", set)
                            },
                        )
                    }
                }

                SettingsGroup(title = stringResource(Res.string.settings_group_metered)) {
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_metered),
                            checked = bool("use_metered", true),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("use_metered", it, reload = true) }
                    }
                    item { first, last ->
                        SettingTogglePairRow(
                            firstTitle = stringResource(Res.string.setting_metered_2g),
                            firstChecked = bool("unmetered_2g", false),
                            onFirstCheckedChange = { updateFlag("unmetered_2g", it, reload = true) },
                            secondTitle = stringResource(Res.string.setting_metered_3g),
                            secondChecked = bool("unmetered_3g", false),
                            onSecondCheckedChange = { updateFlag("unmetered_3g", it, reload = true) },
                            isFirst = first,
                            isLast = last,
                        )
                    }
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_metered_4g),
                            checked = bool("unmetered_4g", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("unmetered_4g", it, reload = true) }
                    }
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_national_roaming),
                            checked = bool("national_roaming", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("national_roaming", it, reload = true) }
                    }
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_eu_roaming),
                            checked = bool("eu_roaming", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("eu_roaming", it, reload = true) }
                    }
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_call),
                            checked = bool("disable_on_call", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("disable_on_call", it, reload = true) }
                    }
                }
            }

            // Advanced Section
            if (section == SettingsSection.Advanced) {
                SettingsGroup(title = stringResource(Res.string.settings_group_logging)) {
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_system),
                            checked = bool("manage_system", false),
                            isFirst = first,
                            isLast = last,
                        ) { enabled ->
                            viewModel.putBoolean("manage_system", enabled)
                            viewModel.putBoolean("show_system", enabled)
                            NetGuardPlatform.firewall.reload("settings", false)
                        }
                    }
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_log_app),
                            checked = bool("log", false),
                            isFirst = first,
                            isLast = last,
                        ) { enabled ->
                            viewModel.putBoolean("log", enabled)
                            NetGuardPlatform.firewall.reload("settings", false)
                        }
                    }
                    item { first, last ->
                        SettingTextRowWithTooltip(
                            title = stringResource(Res.string.setting_log_retention_days),
                            tooltip = stringResource(Res.string.summary_log_retention_days),
                            value = str("log_retention_days", "3"),
                            keyboardType = KeyboardType.Number,
                            isFirst = first,
                            isLast = last,
                        ) { input ->
                            val numeric = input.filter(Char::isDigit).take(3)
                            val normalized =
                                numeric.toIntOrNull()?.coerceIn(0, 365)?.toString() ?: numeric
                            viewModel.putString("log_retention_days", normalized)
                        }
                    }
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_access),
                            checked = bool("notify_access", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("notify_access", it) }
                    }
                }

                SettingsGroup(title = stringResource(Res.string.settings_group_filtering)) {
                    item { first, last ->
                        SettingToggleRowWithTooltip(
                            title = stringResource(Res.string.setting_filter),
                            tooltip = stringResource(Res.string.tooltip_filter),
                            checked = bool("filter", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("filter", it, reload = true) }
                    }
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_filter_udp),
                            checked = bool("filter_udp", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("filter_udp", it, reload = true) }
                    }
                    item { first, last ->
                        SettingToggleRowWithTooltip(
                            title = stringResource(Res.string.setting_lockdown),
                            tooltip = stringResource(Res.string.tooltip_lockdown),
                            checked = bool("lockdown", false),
                            isFirst = first,
                            isLast = last,
                        ) {
                            updateFlag(
                                "lockdown",
                                it,
                                reload = true,
                                updateWidgets = { NetGuardPlatform.widgets.updateLockdown() },
                            )
                        }
                    }
                    item { first, last ->
                        SettingTogglePairRow(
                            firstTitle = stringResource(Res.string.setting_lockdown_wifi),
                            firstChecked = bool("lockdown_wifi", false),
                            onFirstCheckedChange = { updateFlag("lockdown_wifi", it, reload = true) },
                            secondTitle = stringResource(Res.string.setting_lockdown_other),
                            secondChecked = bool("lockdown_other", false),
                            onSecondCheckedChange = {
                                updateFlag("lockdown_other", it, reload = true)
                            },
                            isFirst = first,
                            isLast = last,
                        )
                    }
                }

                SettingsGroup(title = stringResource(Res.string.settings_group_protection)) {
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_malware),
                            checked = bool("malware", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("malware", it, reload = true) }
                    }
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_track_usage),
                            checked = bool("track_usage", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("track_usage", it, reload = true) }
                    }
                }

                SettingsGroup(title = stringResource(Res.string.settings_group_statistics)) {
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_stats),
                            checked = bool("show_stats", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("show_stats", it, reloadStats = true) }
                    }
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_stats_top),
                            checked = bool("show_top", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("show_top", it, reloadStats = true) }
                    }
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(Res.string.setting_stats_frequency, SettingValuePlaceholder),
                            value = str("stats_frequency", "1000"),
                            keyboardType = KeyboardType.Number,
                            isFirst = first,
                            isLast = last,
                            onValueChange = {
                                viewModel.putString("stats_frequency", it)
                            },
                        )
                    }
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(
                                Res.string.setting_stats_samples,
                                SettingValuePlaceholder,
                            ),
                            value = str("stats_samples", "10"),
                            keyboardType = KeyboardType.Number,
                            isFirst = first,
                            isLast = last,
                            onValueChange = { viewModel.putString("stats_samples", it) },
                        )
                    }
                }
            }

            // Hosts Section
            if (section == SettingsSection.Hosts) {
                SettingsGroup {
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_use_hosts),
                            checked = bool("use_hosts", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("use_hosts", it, reload = true) }
                    }
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(Res.string.setting_hosts_url),
                            value = str("hosts_url", ""),
                            isFirst = first,
                            isLast = last,
                            onValueChange = { viewModel.putString("hosts_url", it) },
                        )
                    }
                }

                // Download is the primary action; importing a local file is Android-only.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                ) {
                    FilledTonalButton(
                        onClick = { NetGuardPlatform.hosts.startHostsDownload() },
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(icon = MaterialSymbols.Filled.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(spacing.small))
                        Text(
                            text = stringResource(Res.string.setting_hosts_download),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (importHostsFile != null) {
                        Box {
                            var showImportMenu by remember { mutableStateOf(false) }
                            OutlinedButton(
                                onClick = { showImportMenu = true },
                                shapes = ButtonDefaults.shapes(),
                            ) {
                                Icon(icon = MaterialSymbols.Filled.UploadFile, contentDescription = null)
                                Spacer(modifier = Modifier.width(spacing.small))
                                Text(text = stringResource(Res.string.hosts_import_action))
                            }
                            DropdownMenu(
                                expanded = showImportMenu,
                                onDismissRequest = { showImportMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.setting_hosts)) },
                                    leadingIcon = {
                                        Icon(MaterialSymbols.Filled.UploadFile, contentDescription = null)
                                    },
                                    onClick = {
                                        showImportMenu = false
                                        importHostsFile(false)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.setting_hosts_append)) },
                                    leadingIcon = {
                                        Icon(MaterialSymbols.Filled.PlaylistAdd, contentDescription = null)
                                    },
                                    onClick = {
                                        showImportMenu = false
                                        importHostsFile(true)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            SettingsNetworkSection(
                section = section,
                bool = ::bool,
                str = ::str,
                viewModel = viewModel,
                onOpenDns = onOpenDns,
                onOpenForwarding = onOpenForwarding,
            )

            SettingsBackgroundSection(
                section = section,
                bool = ::bool,
                str = ::str,
                viewModel = viewModel,
                updateCheckInProgress = updateCheckInProgress,
                updateCheckStatus = updateCheckStatus,
                updateCheckVersion = updateCheckVersion,
                onCheckForUpdate = {
                    updateCheckInProgress = true
                    updateCheckStatus = null
                    updateCheckVersion = null
                    NetGuardPlatform.firewall.checkForUpdateNow("settings")
                },
            )

            if (section == SettingsSection.About) {
                AboutContent(onOpenUrl = uriHandler::openUri)
            }
        }
    } // end Scaffold

    hostImportResult?.let { success ->
        AlertDialog(
            onDismissRequest = { hostImportResult = null },
            confirmButton = {
                TextButton(onClick = { hostImportResult = null }) {
                    Text(text = stringResource(Res.string.menu_ok))
                }
            },
            title = {
                Text(
                    text = stringResource(
                        if (success) Res.string.hosts_import_done else Res.string.hosts_import_failed,
                    ),
                )
            },
            text = if (success) {
                { Text(text = stringResource(Res.string.summary_block_domains)) }
            } else {
                null
            },
        )
    }
}

/**
 * M3 Expressive grouped list: items are collected and rendered as individual filled
 * surfaces separated by 2dp gaps; the first and last items get large outer corners.
 */
internal class SettingsGroupScope {
    val items = mutableListOf<@Composable (isFirst: Boolean, isLast: Boolean) -> Unit>()

    fun item(content: @Composable (isFirst: Boolean, isLast: Boolean) -> Unit) {
        items += content
    }
}

@Composable
internal fun SettingsGroup(
    title: String? = null,
    builder: SettingsGroupScope.() -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val scope = SettingsGroupScope().apply(builder)
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        title?.let { SettingsSubheader(it) }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            scope.items.forEachIndexed { index, item ->
                item(index == 0, index == scope.items.lastIndex)
            }
        }
    }
}

@Composable
internal fun SettingsSubheader(title: String) {
    val spacing = MaterialTheme.spacing
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = spacing.default),
    )
}
