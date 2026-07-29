package com.bernaferrari.quietguard.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bernaferrari.quietguard.generated.resources.*
import com.bernaferrari.quietguard.platform.NetGuardPlatform
import com.bernaferrari.quietguard.platform.PlatformContext
import com.bernaferrari.quietguard.ui.SettingsSection
import com.bernaferrari.quietguard.ui.icons.Icon
import com.bernaferrari.quietguard.ui.icons.MaterialSymbols
import com.bernaferrari.quietguard.ui.screens.vm.SettingsViewModel
import com.bernaferrari.quietguard.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SettingsBackgroundSection(
    section: SettingsSection?,
    bool: (String, Boolean) -> Boolean,
    str: (String, String) -> String,
    viewModel: SettingsViewModel,
    updateCheckInProgress: Boolean,
    updateCheckStatus: String?,
    updateCheckVersion: String?,
    onCheckForUpdate: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
        // Background Section
        if (section == SettingsSection.Background) {
            SettingsGroup {
                item { first, last ->
                    SettingTextRow(
                        title = stringResource(Res.string.setting_watchdog, "…"),
                        value = str("watchdog", "0"),
                        keyboardType = KeyboardType.Number,
                        isFirst = first,
                        isLast = last,
                        onValueChange = { value ->
                            viewModel.putString("watchdog", value)
                            val enabled = viewModel.getBoolean("enabled", false)
                            NetGuardPlatform.workScheduler.scheduleWatchdog(
                                value.toIntOrNull() ?: 0,
                                enabled,
                            )
                        },
                    )
                }
                if (PlatformContext.isAndroid()) {
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_update),
                            checked = bool("update_check", true),
                            isFirst = first,
                            isLast = last,
                        ) { viewModel.putBoolean("update_check", it) }
                    }
                }
            }

            if (PlatformContext.isAndroid()) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    FilledTonalButton(
                        onClick = onCheckForUpdate,
                        enabled = !updateCheckInProgress,
                    ) {
                        if (updateCheckInProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(spacing.small))
                            Text(text = stringResource(Res.string.setting_update_checking))
                        } else {
                            Icon(icon = MaterialSymbols.Filled.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(spacing.small))
                            Text(text = stringResource(Res.string.setting_update_now))
                        }
                    }

                    updateCheckStatus?.let { status ->
                        val message =
                            when (status) {
                                "available" ->
                                    if (!updateCheckVersion.isNullOrBlank()) {
                                        stringResource(
                                            Res.string.setting_update_result_available,
                                                updateCheckVersion,
                                        )
                                    } else {
                                        stringResource(
                                            Res.string.setting_update_result_available_unknown,
                                        )
                                    }

                                "upToDate" ->
                                    stringResource(Res.string.setting_update_result_up_to_date)

                                "unavailable" ->
                                    stringResource(Res.string.setting_update_result_unavailable)

                                else -> stringResource(Res.string.setting_update_result_failed)
                            }
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Diagnostics (merged into Background)
            SettingsGroup(title = stringResource(Res.string.setting_section_diagnostics)) {
                item { first, last ->
                    SettingToggleRowWithTooltip(
                        title = stringResource(Res.string.setting_pcap),
                        tooltip = stringResource(Res.string.tooltip_pcap),
                        checked = bool("pcap", false),
                        isFirst = first,
                        isLast = last,
                    ) { viewModel.putBoolean("pcap", it) }
                }
                item { first, last ->
                    SettingTextRow(
                        title = stringResource(Res.string.setting_pcap_record_size, "…"),
                        value = str("pcap_record_size", "64"),
                        keyboardType = KeyboardType.Number,
                        isFirst = first,
                        isLast = last,
                        onValueChange = {
                            viewModel.putString("pcap_record_size", it)
                        },
                    )
                }
                item { first, last ->
                    SettingTextRow(
                        title = stringResource(Res.string.setting_pcap_file_size, "…"),
                        value = str("pcap_file_size", "2"),
                        keyboardType = KeyboardType.Number,
                        isFirst = first,
                        isLast = last,
                        onValueChange = { viewModel.putString("pcap_file_size", it) },
                    )
                }
            }
        }

}
