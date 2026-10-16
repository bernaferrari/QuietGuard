package com.bernaferrari.quietguard.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.bernaferrari.quietguard.generated.resources.*
import com.bernaferrari.quietguard.ui.SettingsSection
import com.bernaferrari.quietguard.ui.icons.MaterialSymbols
import com.bernaferrari.quietguard.ui.screens.vm.SettingsViewModel
import com.bernaferrari.quietguard.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SettingsNetworkSection(
    section: SettingsSection?,
    bool: (String, Boolean) -> Boolean,
    str: (String, String) -> String,
    viewModel: SettingsViewModel,
    onOpenDns: () -> Unit,
    onOpenForwarding: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
        // Network Section (DNS + Proxy + VPN)
        if (section == SettingsSection.Network) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
                SettingsGroup(title = stringResource(Res.string.setting_section_dns)) {
                    item { first, last ->
                        SettingTextRowWithTooltip(
                            title = stringResource(Res.string.setting_rcode, SettingValuePlaceholder),
                            tooltip = stringResource(Res.string.tooltip_rcode),
                            value = str("rcode", "3"),
                            keyboardType = KeyboardType.Number,
                            isFirst = first,
                            isLast = last,
                        ) { viewModel.putString("rcode", it) }
                    }
                    item { first, last ->
                        SettingTextRowWithTooltip(
                            title = stringResource(Res.string.setting_ttl, SettingValuePlaceholder),
                            tooltip = stringResource(Res.string.tooltip_ttl),
                            value = str("ttl", "259200"),
                            keyboardType = KeyboardType.Number,
                            isFirst = first,
                            isLast = last,
                        ) { viewModel.putString("ttl", it) }
                    }
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(Res.string.setting_validate, SettingValuePlaceholder),
                            value = str("validate", ""),
                            isFirst = first,
                            isLast = last,
                            onValueChange = { viewModel.putString("validate", it) },
                        )
                    }
                    item { first, last ->
                        SettingsDestinationRow(
                            title = stringResource(Res.string.setting_show_resolved),
                            subtitle = stringResource(Res.string.ui_dns_hint),
                            icon = MaterialSymbols.Filled.Dns,
                            isFirst = first,
                            isLast = last,
                            onClick = onOpenDns,
                        )
                    }
                }
            }

            SettingsGroup(title = stringResource(Res.string.setting_section_proxy)) {
                item { first, last ->
                    SettingToggleRow(
                        title = stringResource(Res.string.setting_socks5_enabled),
                        checked = bool("socks5_enabled", false),
                        isFirst = first,
                        isLast = last,
                    ) { viewModel.putBoolean("socks5_enabled", it) }
                }
                // Connection details only matter once the proxy is on.
                if (bool("socks5_enabled", false)) {
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(Res.string.setting_socks5_addr, SettingValuePlaceholder),
                            value = str("socks5_addr", ""),
                            isFirst = first,
                            isLast = last,
                            onValueChange = { viewModel.putString("socks5_addr", it) },
                        )
                    }
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(Res.string.setting_socks5_port, SettingValuePlaceholder),
                            value = str("socks5_port", "0"),
                            keyboardType = KeyboardType.Number,
                            isFirst = first,
                            isLast = last,
                            onValueChange = { viewModel.putString("socks5_port", it) },
                        )
                    }
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(Res.string.setting_socks5_username, SettingValuePlaceholder),
                            value = str("socks5_username", ""),
                            isFirst = first,
                            isLast = last,
                            onValueChange = {
                                viewModel.putString("socks5_username", it)
                            },
                        )
                    }
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(Res.string.setting_socks5_password, SettingValuePlaceholder),
                            value = str("socks5_password", ""),
                            keyboardType = KeyboardType.Password,
                            isFirst = first,
                            isLast = last,
                            onValueChange = {
                                viewModel.putString("socks5_password", it)
                            },
                        )
                    }
                }
            }

            SettingsGroup(title = stringResource(Res.string.setting_section_vpn)) {
                item { first, last ->
                    SettingTextRow(
                        title = stringResource(Res.string.setting_vpn4, SettingValuePlaceholder),
                        value = str("vpn4", "10.1.10.1"),
                        isFirst = first,
                        isLast = last,
                        onValueChange = { viewModel.putString("vpn4", it) },
                    )
                }
                item { first, last ->
                    SettingTextRow(
                        title = stringResource(Res.string.setting_vpn6, SettingValuePlaceholder),
                        value = str("vpn6", "fd00:1:fd00:1:fd00:1:fd00:1"),
                        isFirst = first,
                        isLast = last,
                        onValueChange = { viewModel.putString("vpn6", it) },
                    )
                }
                item { first, last ->
                    SettingTextRow(
                        title = stringResource(Res.string.setting_dns, SettingValuePlaceholder),
                        value = str("dns", ""),
                        isFirst = first,
                        isLast = last,
                        onValueChange = { viewModel.putString("dns", it) },
                    )
                }
                item { first, last ->
                    SettingTextRow(
                        title = stringResource(Res.string.setting_dns2, SettingValuePlaceholder),
                        value = str("dns2", ""),
                        isFirst = first,
                        isLast = last,
                        onValueChange = { viewModel.putString("dns2", it) },
                    )
                }
            }

            // Forwarding
            SettingsGroup {
                item { first, last ->
                    SettingsDestinationRow(
                        title = stringResource(Res.string.ui_forwarding_title),
                        subtitle = stringResource(Res.string.ui_forwarding_hint),
                        icon = MaterialSymbols.AutoMirrored.Filled.Forward,
                        isFirst = first,
                        isLast = last,
                        onClick = onOpenForwarding,
                    )
                }
            }
        }

}
