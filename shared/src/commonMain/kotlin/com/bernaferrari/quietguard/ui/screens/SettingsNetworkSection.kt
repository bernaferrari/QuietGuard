package com.bernaferrari.quietguard.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.bernaferrari.quietguard.generated.resources.*
import com.bernaferrari.quietguard.ui.SettingsSection
import com.bernaferrari.quietguard.ui.icons.Icon
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
                            title = stringResource(Res.string.setting_rcode, str("rcode", "3")),
                            tooltip = stringResource(Res.string.tooltip_rcode),
                            value = str("rcode", "3"),
                            keyboardType = KeyboardType.Number,
                            isFirst = first,
                            isLast = last,
                        ) { viewModel.putString("rcode", it) }
                    }
                    item { first, last ->
                        SettingTextRowWithTooltip(
                            title = stringResource(Res.string.setting_ttl, "…"),
                            tooltip = stringResource(Res.string.tooltip_ttl),
                            value = str("ttl", "259200"),
                            keyboardType = KeyboardType.Number,
                            isFirst = first,
                            isLast = last,
                        ) { viewModel.putString("ttl", it) }
                    }
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(Res.string.setting_validate, str("validate", "")),
                            value = str("validate", ""),
                            isFirst = first,
                            isLast = last,
                            onValueChange = { viewModel.putString("validate", it) },
                        )
                    }
                }
                FilledTonalButton(onClick = onOpenDns) {
                    Icon(icon = MaterialSymbols.Filled.Dns, contentDescription = null)
                    Spacer(modifier = Modifier.width(spacing.small))
                    Text(text = stringResource(Res.string.setting_show_resolved))
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
                item { first, last ->
                    SettingTextRow(
                        title = stringResource(
                            Res.string.setting_socks5_addr,
                            str("socks5_addr", ""),
                        ),
                        value = str("socks5_addr", ""),
                        isFirst = first,
                        isLast = last,
                        onValueChange = { viewModel.putString("socks5_addr", it) },
                    )
                }
                item { first, last ->
                    SettingTextRow(
                        title = stringResource(
                            Res.string.setting_socks5_port,
                            str("socks5_port", "0"),
                        ),
                        value = str("socks5_port", "0"),
                        keyboardType = KeyboardType.Number,
                        isFirst = first,
                        isLast = last,
                        onValueChange = { viewModel.putString("socks5_port", it) },
                    )
                }
                item { first, last ->
                    SettingTextRow(
                        title = stringResource(
                            Res.string.setting_socks5_username,
                            str("socks5_username", ""),
                        ),
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
                        title = stringResource(
                            Res.string.setting_socks5_password,
                            str("socks5_password", ""),
                        ),
                        value = str("socks5_password", ""),
                        isFirst = first,
                        isLast = last,
                        onValueChange = {
                            viewModel.putString("socks5_password", it)
                        },
                    )
                }
            }

            SettingsGroup(title = stringResource(Res.string.setting_section_vpn)) {
                item { first, last ->
                    SettingTextRow(
                        title = stringResource(Res.string.setting_vpn4, str("vpn4", "10.1.10.1")),
                        value = str("vpn4", "10.1.10.1"),
                        isFirst = first,
                        isLast = last,
                        onValueChange = { viewModel.putString("vpn4", it) },
                    )
                }
                item { first, last ->
                    SettingTextRow(
                        title = stringResource(
                            Res.string.setting_vpn6,
                            str("vpn6", "fd00:1:fd00:1:fd00:1:fd00:1"),
                        ),
                        value = str("vpn6", "fd00:1:fd00:1:fd00:1:fd00:1"),
                        isFirst = first,
                        isLast = last,
                        onValueChange = { viewModel.putString("vpn6", it) },
                    )
                }
                item { first, last ->
                    SettingTextRow(
                        title = stringResource(Res.string.setting_dns, str("dns", "")),
                        value = str("dns", ""),
                        isFirst = first,
                        isLast = last,
                        onValueChange = { viewModel.putString("dns", it) },
                    )
                }
                item { first, last ->
                    SettingTextRow(
                        title = stringResource(Res.string.setting_dns2, str("dns2", "")),
                        value = str("dns2", ""),
                        isFirst = first,
                        isLast = last,
                        onValueChange = { viewModel.putString("dns2", it) },
                    )
                }
            }

            // Forwarding
            Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                SettingsSubheader(stringResource(Res.string.setting_forwarding))
                FilledTonalButton(onClick = onOpenForwarding) {
                    Icon(
                        icon = MaterialSymbols.AutoMirrored.Filled.Forward,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(spacing.small))
                    Text(text = stringResource(Res.string.setting_forwarding))
                }
            }
        }

}
