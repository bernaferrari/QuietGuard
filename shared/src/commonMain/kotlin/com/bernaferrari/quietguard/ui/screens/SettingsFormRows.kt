package com.bernaferrari.quietguard.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.quietguard.generated.resources.Res
import com.bernaferrari.quietguard.generated.resources.content_desc_show_info
import com.bernaferrari.quietguard.generated.resources.setting_value_off
import com.bernaferrari.quietguard.generated.resources.setting_value_on
import com.bernaferrari.quietguard.ui.components.ExpandableContent
import com.bernaferrari.quietguard.ui.components.groupItemShape
import com.bernaferrari.quietguard.ui.components.groupPairTileShape
import com.bernaferrari.quietguard.ui.icons.Icon
import com.bernaferrari.quietguard.ui.icons.MaterialSymbols
import com.bernaferrari.quietguard.ui.theme.TouchTargets
import com.bernaferrari.quietguard.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource
import com.bernaferrari.quietguard.ui.theme.effectsSpec


/**
 * Container color for toggle rows: rows that are ON get a subtle primary-tinted
 * fill so active settings read at a glance; animated so flips feel alive.
 */
@Composable
private fun settingRowContainerColor(checked: Boolean): Color {
    val base = MaterialTheme.colorScheme.surfaceContainerLow
    val target = if (checked) {
        lerp(base, MaterialTheme.colorScheme.primaryContainer, 0.35f)
    } else {
        base
    }
    val color by animateColorAsState(
        targetValue = target,
        animationSpec = effectsSpec(),
        label = "settingRowBg",
    )
    return color
}

@Composable
internal fun SettingToggleRow(
    title: String,
    checked: Boolean,
    subtitle: String? = null,
    isFirst: Boolean = true,
    isLast: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val localizedState = stringResource(
        if (checked) Res.string.setting_value_on else Res.string.setting_value_off,
    )
    val spacing = MaterialTheme.spacing
    val haptic = LocalHapticFeedback.current
    val onToggle = { newValue: Boolean ->
        haptic.performHapticFeedback(
            if (newValue) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff,
        )
        onCheckedChange(newValue)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = settingRowContainerColor(checked),
        shape = groupItemShape(isFirst, isLast),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .toggleable(
                    value = checked,
                    role = Role.Switch,
                    onValueChange = onToggle,
                )
                .padding(horizontal = spacing.large, vertical = spacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = spacing.default),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.semantics {
                    contentDescription = "$title: $localizedState"
                },
            )
        }
    }
}

@Composable
internal fun SettingToggleRowWithTooltip(
    title: String,
    tooltip: String,
    checked: Boolean,
    isFirst: Boolean = true,
    isLast: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val localizedState = stringResource(
        if (checked) Res.string.setting_value_on else Res.string.setting_value_off,
    )
    val spacing = MaterialTheme.spacing
    val haptic = LocalHapticFeedback.current
    var showTooltip by remember { mutableStateOf(false) }
    val onToggle = { newValue: Boolean ->
        haptic.performHapticFeedback(
            if (newValue) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff,
        )
        onCheckedChange(newValue)
    }

    val infoLabel = stringResource(Res.string.content_desc_show_info, title)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = settingRowContainerColor(checked),
        shape = groupItemShape(isFirst, isLast),
    ) {
        Column {
            // Split row: the text area expands the explanation, the switch toggles.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 64.dp)
                        .clickable(onClickLabel = infoLabel) { showTooltip = !showTooltip }
                        .padding(
                            start = spacing.large,
                            end = spacing.medium,
                            top = spacing.small,
                            bottom = spacing.small,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        icon = MaterialSymbols.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                VerticalDivider(
                    modifier = Modifier.height(32.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Box(
                    modifier = Modifier
                        .heightIn(min = 64.dp)
                        .toggleable(
                            value = checked,
                            role = Role.Switch,
                            onValueChange = onToggle,
                        )
                        .padding(horizontal = spacing.default),
                    contentAlignment = Alignment.Center,
                ) {
                    Switch(
                        checked = checked,
                        onCheckedChange = null,
                        modifier = Modifier.semantics {
                            contentDescription = "$title: $localizedState"
                        },
                    )
                }
            }
            ExpandableContent(expanded = showTooltip) {
                Text(
                    text = tooltip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = spacing.large,
                        end = spacing.large,
                        bottom = spacing.default,
                    ),
                )
            }
        }
    }
}

@Composable
internal fun SettingTextRow(
    title: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isFirst: Boolean = true,
    isLast: Boolean = true,
    onValueChange: (String) -> Unit,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { SettingFieldLabelText(settingFieldLabel(title, value)) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SettingFieldHeight),
        shape = groupItemShape(isFirst, isLast),
        singleLine = true,
        colors = settingTextFieldColors(),
    )
}

/**
 * Fixed field height so focusing an empty field (label floating up) never
 * changes the row's size mid-animation; matches the 64dp toggle rows.
 */
private val SettingFieldHeight = 64.dp

@Composable
private fun SettingFieldLabelText(text: String) {
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun settingTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
)

private fun settingFieldLabel(title: String, value: String): String {
    val withoutValue =
        if (value.isNotEmpty() && title.endsWith(value)) title.dropLast(value.length) else title
    return withoutValue
        .replace("%s", "")
        .replace(Regex("\\s+"), " ")
        .trim()
        .trimEnd(':', '-', ' ')
        .trimEnd(':')
        .trim()
}

@Composable
internal fun SettingTextRowWithTooltip(
    title: String,
    tooltip: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isFirst: Boolean = true,
    isLast: Boolean = true,
    onValueChange: (String) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    var showTooltip by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = groupItemShape(isFirst, isLast),
    ) {
        Column {
            TextField(
                value = value,
                onValueChange = onValueChange,
                label = { SettingFieldLabelText(settingFieldLabel(title, value)) },
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = SettingFieldHeight),
                singleLine = true,
                colors = settingTextFieldColors(),
                trailingIcon = {
                    IconButton(
                        onClick = { showTooltip = !showTooltip },
                        modifier = Modifier.size(TouchTargets.minimum),
                    ) {
                        Icon(
                            icon = MaterialSymbols.Outlined.Info,
                            contentDescription = stringResource(
                                Res.string.content_desc_show_info,
                                title,
                            ),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
            ExpandableContent(expanded = showTooltip) {
                Text(
                    text = tooltip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = spacing.default,
                        end = spacing.default,
                        top = spacing.extraSmall,
                        bottom = spacing.medium,
                    ),
                )
            }
        }
    }
}

@Composable
internal fun SettingTogglePairRow(
    firstTitle: String,
    firstChecked: Boolean,
    onFirstCheckedChange: (Boolean) -> Unit,
    secondTitle: String,
    secondChecked: Boolean,
    onSecondCheckedChange: (Boolean) -> Unit,
    isFirst: Boolean = true,
    isLast: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        CompactSettingToggleTile(
            modifier = Modifier.weight(1f),
            title = firstTitle,
            checked = firstChecked,
            onCheckedChange = onFirstCheckedChange,
            shape = groupPairTileShape(
                isLeadingTile = true,
                isFirst = isFirst,
                isLast = isLast,
            ),
        )
        CompactSettingToggleTile(
            modifier = Modifier.weight(1f),
            title = secondTitle,
            checked = secondChecked,
            onCheckedChange = onSecondCheckedChange,
            shape = groupPairTileShape(
                isLeadingTile = false,
                isFirst = isFirst,
                isLast = isLast,
            ),
        )
    }
}

@Composable
private fun CompactSettingToggleTile(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(
                if (!checked) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff,
            )
            onCheckedChange(!checked)
        },
        modifier = modifier.heightIn(min = 72.dp),
        shape = shape,
        color = settingRowContainerColor(checked),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.default),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.heightIn(min = spacing.small))
            Switch(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}
