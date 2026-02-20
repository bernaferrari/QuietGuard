package eu.faircode.netguard.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.faircode.netguard.R
import eu.faircode.netguard.ui.theme.LocalMotion
import eu.faircode.netguard.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onToggleEnabled: (Boolean) -> Unit,
) {
    val enabled by viewModel.enabled.collectAsStateWithLifecycle()
    val rulesUiState by viewModel.rulesUiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.ensureRulesLoaded()
    }

    val rules = rulesUiState.rules
    val blockedApps = rules.count { it.wifi_blocked || it.other_blocked }
    val allowedApps = rules.count { !it.wifi_blocked && !it.other_blocked }
    val totalApps = rules.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(scrollState)
            .padding(spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.large),
    ) {

        // Main Status Card with animated color
        StatusCard(
            enabled = enabled,
            onToggle = onToggleEnabled,
        )

        // Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.stat_blocked_today),
                value = blockedApps,
                icon = Icons.Default.Block,
                tint = MaterialTheme.colorScheme.error,
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.stat_allowed_today),
                value = allowedApps,
                icon = Icons.Default.CheckCircle,
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        StatCard(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.stat_active_rules),
            value = totalApps,
            icon = Icons.Default.Tune,
            tint = MaterialTheme.colorScheme.tertiary,
            emphasized = true,
        )
    }
    } // end Scaffold
}

@Composable
private fun StatusCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val motion = LocalMotion.current
    val spacing = MaterialTheme.spacing

    val containerColor by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(motion.durationMedium),
        label = "statusColor",
    )

    val statusDescription = if (enabled) {
        stringResource(R.string.status_enabled)
    } else {
        stringResource(R.string.status_disabled)
    }

    val cardShape = MaterialTheme.shapes.large
    Card(
        onClick = { onToggle(!enabled) },
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.padding(spacing.large),
            verticalArrangement = Arrangement.spacedBy(spacing.default),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color =
                        if (enabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = stringResource(R.string.content_desc_security_status),
                        tint =
                            if (enabled) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(spacing.small),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statusDescription,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (enabled) {
                            stringResource(R.string.status_running)
                        } else {
                            stringResource(R.string.status_not_running)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.semantics {
                        contentDescription = statusDescription
                    },
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: Int,
    icon: ImageVector,
    tint: Color,
    emphasized: Boolean = false,
) {
    val motion = LocalMotion.current
    val spacing = MaterialTheme.spacing

    val animatedValue by animateIntAsState(
        targetValue = value,
        animationSpec = tween(motion.durationMedium),
        label = "statValue",
    )

    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (emphasized) MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow,
            ),

    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tint,
            )
            Text(
                text = animatedValue.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color =
                    if (emphasized) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
