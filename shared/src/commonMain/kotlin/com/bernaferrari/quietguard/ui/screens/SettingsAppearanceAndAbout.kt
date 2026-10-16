package com.bernaferrari.quietguard.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bernaferrari.quietguard.generated.resources.Res
import com.bernaferrari.quietguard.generated.resources.about_social_title
import com.bernaferrari.quietguard.generated.resources.about_source_summary
import com.bernaferrari.quietguard.generated.resources.about_source_title
import com.bernaferrari.quietguard.generated.resources.about_version
import com.bernaferrari.quietguard.generated.resources.app_description
import com.bernaferrari.quietguard.generated.resources.app_name
import com.bernaferrari.quietguard.ui.icons.Icon
import com.bernaferrari.quietguard.ui.icons.MaterialSymbols
import com.bernaferrari.quietguard.ui.theme.TouchTargets
import com.bernaferrari.quietguard.ui.theme.spacing
import androidx.compose.material3.Icon as Material3Icon
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.ResourceItem
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.bernaferrari.quietguard.ui.theme.effectsSpec
import com.bernaferrari.quietguard.ui.theme.fastEffectsSpec

private const val PROJECT_GITHUB_URL = "https://github.com/bernaferrari/QuietGuard"
private const val LINKEDIN_URL = "https://www.linkedin.com/in/bernaferrari"
private const val X_URL = "https://x.com/bernaferrari"
private const val REDDIT_URL = "https://www.reddit.com/user/bernaferrari"

private const val ABOUT_RESOURCE_PREFIX =
    "composeResources/com.bernaferrari.quietguard.generated.resources/drawable/"

@OptIn(InternalResourceApi::class)
private val linkedInLogo = DrawableResource(
    "drawable:linkedin_logo",
    setOf(ResourceItem(setOf(), "${ABOUT_RESOURCE_PREFIX}linkedin_logo.xml", -1, -1)),
)

@OptIn(InternalResourceApi::class)
private val xLogo = DrawableResource(
    "drawable:x_logo",
    setOf(ResourceItem(setOf(), "${ABOUT_RESOURCE_PREFIX}x_logo.xml", -1, -1)),
)

@OptIn(InternalResourceApi::class)
private val redditLogo = DrawableResource(
    "drawable:reddit_logo",
    setOf(ResourceItem(setOf(), "${ABOUT_RESOURCE_PREFIX}reddit_logo.xml", -1, -1)),
)

@Composable
internal fun AboutContent(onOpenUrl: (String) -> Unit) {
    val spacing = MaterialTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.large)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = spacing.large,
                    vertical = spacing.large,
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.large),
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon = MaterialSymbols.Filled.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f),
                    ) {
                        Text(
                            text = stringResource(Res.string.about_version),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
            }
        }

        Text(
            text = stringResource(Res.string.app_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.medium),
        )

        SettingsDestinationRow(
            title = stringResource(Res.string.about_source_title),
            subtitle = stringResource(Res.string.about_source_summary),
            icon = MaterialSymbols.Filled.Code,
            trailingIcon = MaterialSymbols.AutoMirrored.Filled.OpenInNew,
            onClick = { onOpenUrl(PROJECT_GITHUB_URL) },
        )

        AboutSocialLinks(onOpenUrl = onOpenUrl)
    }
}

private data class AboutSocialLink(
    val name: String,
    val url: String,
    val drawable: DrawableResource,
)

@Composable
private fun AboutSocialLinks(onOpenUrl: (String) -> Unit) {
    val links = listOf(
        AboutSocialLink("LinkedIn", LINKEDIN_URL, drawable = linkedInLogo),
        AboutSocialLink("X", X_URL, drawable = xLogo),
        AboutSocialLink("Reddit", REDDIT_URL, drawable = redditLogo),
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(Res.string.about_social_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            links.forEach { link ->
                IconButton(
                    onClick = { onOpenUrl(link.url) },
                    modifier = Modifier.size(TouchTargets.minimum),
                ) {
                    Material3Icon(
                        painter = painterResource(link.drawable),
                        contentDescription = link.name,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Animated theme swatch with springy selection, soft glow and shape morphing.
 */
@Composable
internal fun ThemeSwatch(
    theme: String,
    seedColor: Color?,
    isSelected: Boolean,
    isEnabled: Boolean,
    dynamicColor: Color,
    onClick: () -> Unit,
) {
    val isDynamic = theme == "dynamic"
    val baseColor = seedColor ?: dynamicColor
    val displayColor = if (isEnabled) baseColor else baseColor.copy(alpha = 0.42f)
    val tokenSize = 50.dp
    val glowSize = 56.dp
    val orbSize = 40.dp
    val orbCornerFraction by animateFloatAsState(
        targetValue = if (isSelected) 0.5f else 0.26f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 520f),
        label = "orbCorner_$theme",
    )
    val orbShape = RoundedCornerShape(percent = (orbCornerFraction * 100).toInt())

    val orbScale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 0.86f,
        animationSpec = spring(dampingRatio = 0.56f, stiffness = 600f),
        label = "orbScale_$theme",
    )
    val orbRotation by animateFloatAsState(
        targetValue = if (isSelected) 8f else 0f,
        animationSpec = spring(dampingRatio = 0.66f, stiffness = 420f),
        label = "orbRotation_$theme",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = effectsSpec(),
        label = "glow_$theme",
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (isSelected || isDynamic) 1f else 0f,
        animationSpec = fastEffectsSpec(),
        label = "icon_$theme",
    )
    val interactionSource = remember { MutableInteractionSource() }
    val overallAlpha = if (isEnabled) 1f else 0.52f

    Box(
        modifier = Modifier
            .size(tokenSize)
            .graphicsLayer { alpha = overallAlpha }
            .selectable(
                selected = isSelected,
                enabled = isEnabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics { contentDescription = theme.replaceFirstChar { it.uppercase() } },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(glowSize)
                .graphicsLayer {
                    alpha = glowAlpha
                }
                .drawBehind {
                    drawCircle(
                        color = displayColor.copy(alpha = 0.44f),
                        radius = size.minDimension * 0.5f,
                    )
                },
        )

        Box(
            modifier = Modifier
                .size(orbSize)
                .graphicsLayer {
                    scaleX = orbScale
                    scaleY = orbScale
                    rotationZ = orbRotation
                }
                .clip(orbShape)
                .indication(
                    interactionSource = interactionSource,
                    indication = ripple(
                        bounded = true,
                        radius = 18.dp,
                        color = Color.White.copy(alpha = 0.32f),
                    ),
                )
                .background(displayColor),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                            start = Offset.Zero,
                            end = Offset(60f, 60f),
                        ),
                    ),
            )

            if (isSelected || isDynamic) {
                val iconSize = if (isSelected) 18.dp else 16.dp
                Icon(
                    icon = if (isSelected) MaterialSymbols.Filled.Check else MaterialSymbols.Filled.Palette,
                    contentDescription = null,
                    modifier = Modifier
                        .size(iconSize)
                        .graphicsLayer {
                            alpha = iconAlpha
                            // Counter-rotate to keep glyph upright while orb spins.
                            rotationZ = -orbRotation
                        },
                    tint = Color.White.copy(alpha = if (isSelected) 1f else 0.9f),
                )
            }
        }
    }
}
