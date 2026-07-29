package com.bernaferrari.quietguard.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.bernaferrari.quietguard.generated.resources.Res
import com.bernaferrari.quietguard.generated.resources.menu_about
import com.bernaferrari.quietguard.generated.resources.settings_advanced_summary
import com.bernaferrari.quietguard.generated.resources.settings_background_summary
import com.bernaferrari.quietguard.generated.resources.settings_firewall_summary
import com.bernaferrari.quietguard.generated.resources.settings_hosts_summary
import com.bernaferrari.quietguard.generated.resources.settings_more
import com.bernaferrari.quietguard.generated.resources.settings_network_summary
import com.bernaferrari.quietguard.generated.resources.settings_quick_controls
import com.bernaferrari.quietguard.generated.resources.settings_about_summary
import com.bernaferrari.quietguard.generated.resources.setting_section_advanced
import com.bernaferrari.quietguard.generated.resources.setting_section_background
import com.bernaferrari.quietguard.generated.resources.setting_section_firewall
import com.bernaferrari.quietguard.generated.resources.setting_section_hosts
import com.bernaferrari.quietguard.generated.resources.setting_section_network
import com.bernaferrari.quietguard.generated.resources.title_mobile
import com.bernaferrari.quietguard.generated.resources.title_wifi
import com.bernaferrari.quietguard.ui.SettingsSection
import com.bernaferrari.quietguard.ui.components.FirewallTile
import com.bernaferrari.quietguard.ui.components.GroupInnerCorner
import com.bernaferrari.quietguard.ui.components.GroupOuterCorner
import com.bernaferrari.quietguard.ui.components.groupPairTileShape
import com.bernaferrari.quietguard.ui.icons.Icon
import com.bernaferrari.quietguard.ui.icons.MaterialIcon
import com.bernaferrari.quietguard.ui.icons.MaterialSymbols
import com.bernaferrari.quietguard.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
internal fun QuickFirewallControls(
    wifiAllowed: Boolean,
    mobileAllowed: Boolean,
    onToggleWifi: () -> Unit,
    onToggleMobile: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        SettingsSubheader(stringResource(Res.string.settings_quick_controls))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            FirewallTile(
                allowedIcon = MaterialSymbols.Filled.Wifi,
                blockedIcon = MaterialSymbols.Filled.WifiOff,
                label = stringResource(Res.string.title_wifi),
                allowed = wifiAllowed,
                onToggle = onToggleWifi,
                shape = groupPairTileShape(isLeadingTile = true, isFirst = true, isLast = true),
                modifier = Modifier.weight(1f),
            )
            FirewallTile(
                allowedIcon = MaterialSymbols.Outlined.MobiledataArrows,
                blockedIcon = MaterialSymbols.Outlined.MobiledataOff,
                label = stringResource(Res.string.title_mobile),
                allowed = mobileAllowed,
                onToggle = onToggleMobile,
                shape = groupPairTileShape(isLeadingTile = false, isFirst = true, isLast = true),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun SettingsDirectory(onOpenSection: (SettingsSection) -> Unit) {
    SettingsGroup(title = stringResource(Res.string.settings_more)) {
        SettingsDestination.entries.forEach { destination ->
            item { first, last ->
                SettingsDestinationRow(
                    title = stringResource(destination.title),
                    subtitle = stringResource(destination.summary),
                    icon = destination.icon,
                    isFirst = first,
                    isLast = last,
                    onClick = { onOpenSection(destination.section) },
                )
            }
        }
    }
}

private enum class SettingsDestination(
    val section: SettingsSection,
    val title: org.jetbrains.compose.resources.StringResource,
    val summary: org.jetbrains.compose.resources.StringResource,
    val icon: MaterialIcon,
) {
    Firewall(
        SettingsSection.Firewall,
        Res.string.setting_section_firewall,
        Res.string.settings_firewall_summary,
        MaterialSymbols.Outlined.Shield,
    ),
    Advanced(
        SettingsSection.Advanced,
        Res.string.setting_section_advanced,
        Res.string.settings_advanced_summary,
        MaterialSymbols.Filled.Tune,
    ),
    Hosts(
        SettingsSection.Hosts,
        Res.string.setting_section_hosts,
        Res.string.settings_hosts_summary,
        MaterialSymbols.Filled.Download,
    ),
    Network(
        SettingsSection.Network,
        Res.string.setting_section_network,
        Res.string.settings_network_summary,
        MaterialSymbols.Filled.Public,
    ),
    Background(
        SettingsSection.Background,
        Res.string.setting_section_background,
        Res.string.settings_background_summary,
        MaterialSymbols.Filled.Refresh,
    ),
    About(
        SettingsSection.About,
        Res.string.menu_about,
        Res.string.settings_about_summary,
        MaterialSymbols.Filled.Code,
    ),
}

@Composable
internal fun SettingsDestinationRow(
    title: String,
    subtitle: String,
    icon: MaterialIcon,
    trailingIcon: MaterialIcon = MaterialSymbols.Filled.ChevronRight,
    isFirst: Boolean = true,
    isLast: Boolean = true,
    onClick: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 700f),
        label = "destPressScale",
    )
    val topCorner by animateDpAsState(
        targetValue = if (pressed || isFirst) GroupOuterCorner else GroupInnerCorner,
        animationSpec = spring(stiffness = 700f),
        label = "destTopCorner",
    )
    val bottomCorner by animateDpAsState(
        targetValue = if (pressed || isLast) GroupOuterCorner else GroupInnerCorner,
        animationSpec = spring(stiffness = 700f),
        label = "destBottomCorner",
    )
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale },
        shape = RoundedCornerShape(topCorner, topCorner, bottomCorner, bottomCorner),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(
                start = spacing.default,
                top = spacing.medium,
                end = spacing.small,
                bottom = spacing.medium,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null) }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                icon = trailingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
