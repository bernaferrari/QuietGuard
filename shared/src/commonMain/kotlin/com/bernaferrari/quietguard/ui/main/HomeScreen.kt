package com.bernaferrari.quietguard.ui.main

import com.bernaferrari.quietguard.ui.icons.MaterialSymbols
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.quietguard.ui.theme.LocalMotion
import com.bernaferrari.quietguard.ui.theme.spacing
import com.bernaferrari.quietguard.ui.util.StatePlaceholder
import com.bernaferrari.quietguard.generated.resources.Res
import com.bernaferrari.quietguard.generated.resources.home_status_off_body
import com.bernaferrari.quietguard.generated.resources.home_status_on_body
import com.bernaferrari.quietguard.generated.resources.menu_firewall
import com.bernaferrari.quietguard.generated.resources.status_disabled
import com.bernaferrari.quietguard.generated.resources.status_enabled
import com.bernaferrari.quietguard.generated.resources.ui_home_hint
import com.bernaferrari.quietguard.generated.resources.ui_loading
import org.jetbrains.compose.resources.stringResource

import com.bernaferrari.quietguard.ui.icons.Icon
import com.bernaferrari.quietguard.ui.theme.effectsSpec
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onToggleEnabled: (Boolean) -> Unit,
) {
    val firewallState by viewModel.firewallState.collectAsStateWithLifecycle()
    if (!firewallState.isReady) {
        StatePlaceholder(
            title = stringResource(Res.string.ui_loading),
            message = stringResource(Res.string.ui_home_hint),
            icon = MaterialSymbols.Filled.Shield,
            isLoading = true,
        )
        return
    }
    val enabled = firewallState.data
    val motion = LocalMotion.current
    val backgroundProgress by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = tween(
            durationMillis = motion.durationSlow * 2,
            easing = FastOutSlowInEasing,
        ),
        label = "homeShaderColorProgress",
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            FirewallStateShaderBackground(
                enabledProgress = backgroundProgress,
                modifier = Modifier.fillMaxSize(),
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 32.dp)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StatusCard(
                    enabled = enabled,
                    onToggle = onToggleEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Home hero: one large square toggle whose shape morphs from a soft square (off) to a
 * rotating 9-sided cookie (on). The morph needs a square box — stretching a 1:1 polygon
 * across a wide card distorts it into a lens.
 */
@Composable
private fun StatusCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotion.current
    val spacing = MaterialTheme.spacing
    val colors = MaterialTheme.colorScheme

    val containerColor by animateColorAsState(
        targetValue = if (enabled) colors.primary else colors.surfaceContainerHigh,
        animationSpec = effectsSpec(),
        label = "heroContainer",
    )
    val iconTint by animateColorAsState(
        targetValue = if (enabled) colors.onPrimary else colors.onSurfaceVariant,
        animationSpec = effectsSpec(),
        label = "heroIcon",
    )
    // Halos bloom out from behind the core when protection turns on and fold back in when off.
    val haloProgress by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = if (motion.reducedMotion) {
            tween(0)
        } else {
            spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)
        },
        label = "heroHalo",
    )
    val morphProgress by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = if (motion.reducedMotion) {
            tween(0)
        } else {
            spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "heroMorph",
    )
    val carrierRotation = remember { Animatable(0f) }
    LaunchedEffect(enabled, motion.reducedMotion) {
        if (enabled && !motion.reducedMotion) {
            while (true) {
                carrierRotation.animateTo(
                    targetValue = carrierRotation.value + 360f,
                    animationSpec = tween(
                        durationMillis = motion.durationSlow * 24,
                        easing = LinearEasing,
                    ),
                )
                if (carrierRotation.value > 10000f) {
                    carrierRotation.snapTo(carrierRotation.value % 360f)
                }
            }
        } else {
            // Settle on the nearest upright angle so the off state is never tilted.
            val normalized = ((carrierRotation.value % 360f) + 360f) % 360f
            val settleTarget = if (normalized > 180f) {
                carrierRotation.value + (360f - normalized)
            } else {
                carrierRotation.value - normalized
            }
            if (motion.reducedMotion) {
                carrierRotation.snapTo(settleTarget)
            } else {
                carrierRotation.animateTo(
                    targetValue = settleTarget,
                    animationSpec = tween(motion.durationMedium, easing = FastOutSlowInEasing),
                )
            }
            carrierRotation.snapTo(0f)
        }
    }
    val rotation = carrierRotation.value
    val coreShape = rememberHeroCoreShape(morphProgress)
    val innerHaloShape = rememberHeroInnerHaloShape()
    val outerHaloShape = rememberHeroOuterHaloShape()
    val innerHaloColor = colors.primaryContainer
    val outerHaloColor = colors.tertiaryContainer

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "heroPress",
    )

    val statusLabel = stringResource(if (enabled) Res.string.status_enabled else Res.string.status_disabled)
    val statusBody = stringResource(if (enabled) Res.string.home_status_on_body else Res.string.home_status_off_body)
    val firewallLabel = stringResource(Res.string.menu_firewall)

    // The whole hero (shape, text, switch) is one toggle so screen readers hear a single control.
    Column(
        modifier = modifier
            .toggleable(
                value = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch,
                onValueChange = onToggle,
            )
            .semantics {
                contentDescription = firewallLabel
                stateDescription = statusLabel
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.large),
    ) {
        Box(
            modifier = Modifier
                .size(244.dp)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                },
            contentAlignment = Alignment.Center,
        ) {
            // Layered tonal shapes instead of an outline: the outer halo turns one way, the
            // inner one the other, and the core drifts between them.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val scale = 0.72f + 0.28f * haloProgress
                        scaleX = scale
                        scaleY = scale
                        alpha = haloProgress.coerceIn(0f, 1f) * 0.5f
                        rotationZ = -rotation * 0.5f
                        shape = outerHaloShape
                        clip = true
                    }
                    .background(outerHaloColor),
            )
            Box(
                modifier = Modifier
                    .size(198.dp)
                    .graphicsLayer {
                        val scale = 0.78f + 0.22f * haloProgress
                        scaleX = scale
                        scaleY = scale
                        alpha = haloProgress.coerceIn(0f, 1f)
                        rotationZ = rotation
                        shape = innerHaloShape
                        clip = true
                    }
                    .background(innerHaloColor),
            )
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .graphicsLayer {
                        rotationZ = rotation * 0.4f
                        shape = coreShape
                        clip = true
                    }
                    .background(containerColor)
                    .indication(interactionSource, ripple()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon = if (enabled) MaterialSymbols.Filled.GppGood else MaterialSymbols.Outlined.Shield,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer { rotationZ = -(rotation * 0.4f) },
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = statusBody,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Switch(
            checked = enabled,
            onCheckedChange = null,
        )
    }
}
