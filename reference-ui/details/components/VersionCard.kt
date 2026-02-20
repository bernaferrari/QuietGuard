package com.bernaferrari.sdkmonitor.ui.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bernaferrari.sdkmonitor.R
import com.bernaferrari.sdkmonitor.domain.model.AppVersion
import com.bernaferrari.sdkmonitor.extensions.apiToColor
import com.bernaferrari.sdkmonitor.ui.theme.SDKMonitorTheme

@Composable
fun VersionTimelineEntry(
    modifier: Modifier = Modifier,
    versionInfo: AppVersion,
    isLatest: Boolean = false,
    isLast: Boolean = false,
) {
    val apiColor = Color(versionInfo.sdkVersion.apiToColor())

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Simple timeline indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Timeline dot
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .background(
                            if (isLatest) apiColor else MaterialTheme.colorScheme.outline,
                            CircleShape,
                        ),
            )

            // Timeline line
            if (!isLast) {
                Box(
                    modifier =
                        Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }

        // Version content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left side: Version name and LATEST badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = versionInfo.versionName,
                        style =
                            MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isLatest) FontWeight.Bold else FontWeight.Medium,
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Right side: API badge - always visible, consistent position
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = apiColor.copy(alpha = 0.15f),
                ) {
                    Text(
                        text = "API ${versionInfo.sdkVersion}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                            ),
                        color = apiColor,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = versionInfo.lastUpdateTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (isLatest) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = apiColor.copy(alpha = 0.2f),
                    ) {
                        Text(
                            text = stringResource(R.string.latest),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style =
                                MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                ),
                            color = apiColor,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Beautiful unified version timeline container
 */
@Composable
fun VersionTimeline(
    modifier: Modifier = Modifier,
    versions: List<AppVersion>,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        versions.forEachIndexed { index, version ->
            VersionTimelineEntry(
                versionInfo = version,
                isLatest = index == 0,
                isLast = index == versions.lastIndex,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VersionTimelinePreview() {
    SDKMonitorTheme {
        VersionTimeline(
            versions =
                listOf(
                    AppVersion(
                        packageName = "com.whatsapp",
                        title = "WhatsApp",
                        sdkVersion = 34,
                        versionName = "2.24.1.75",
                        versionCode = 242175,
                        lastUpdateTime = "2 hours ago",
                    ),
                    AppVersion(
                        packageName = "com.whatsapp",
                        title = "WhatsApp",
                        sdkVersion = 33,
                        versionName = "2.24.1.74",
                        versionCode = 242174,
                        lastUpdateTime = "1 week ago",
                    ),
                    AppVersion(
                        packageName = "com.whatsapp",
                        title = "WhatsApp",
                        sdkVersion = 32,
                        versionName = "2.24.1.70",
                        versionCode = 242170,
                        lastUpdateTime = "3 weeks ago",
                    ),
                    AppVersion(
                        packageName = "com.whatsapp",
                        title = "WhatsApp",
                        sdkVersion = 31,
                        versionName = "2.24.1.65",
                        versionCode = 242165,
                        lastUpdateTime = "2 months ago",
                    ),
                ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VersionTimelineDarkPreview() {
    SDKMonitorTheme(darkTheme = true) {
        VersionTimeline(
            versions =
                listOf(
                    AppVersion(
                        packageName = "com.instagram.android",
                        title = "Instagram",
                        sdkVersion = 34,
                        versionName = "305.0.0.37.120",
                        versionCode = 305000037,
                        lastUpdateTime = "5 minutes ago",
                    ),
                    AppVersion(
                        packageName = "com.instagram.android",
                        title = "Instagram",
                        sdkVersion = 33,
                        versionName = "305.0.0.36.120",
                        versionCode = 305000036,
                        lastUpdateTime = "2 weeks ago",
                    ),
                ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
