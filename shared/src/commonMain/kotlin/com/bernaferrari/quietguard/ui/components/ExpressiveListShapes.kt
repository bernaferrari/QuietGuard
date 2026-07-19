package com.bernaferrari.quietguard.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * M3 Expressive grouped-list corners: large radius on the group's outer edges,
 * small radius between items so a stack of filled surfaces reads as one group.
 */
val GroupOuterCorner = 24.dp
val GroupInnerCorner = 6.dp

fun groupItemShape(isFirst: Boolean, isLast: Boolean): RoundedCornerShape =
    RoundedCornerShape(
        topStart = if (isFirst) GroupOuterCorner else GroupInnerCorner,
        topEnd = if (isFirst) GroupOuterCorner else GroupInnerCorner,
        bottomEnd = if (isLast) GroupOuterCorner else GroupInnerCorner,
        bottomStart = if (isLast) GroupOuterCorner else GroupInnerCorner,
    )

/**
 * Shape for one tile of a horizontally split pair inside a group: only the
 * outward-facing corners of the pair pick up the group's outer radius.
 */
fun groupPairTileShape(
    isLeadingTile: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
): RoundedCornerShape =
    RoundedCornerShape(
        topStart = if (isLeadingTile && isFirst) GroupOuterCorner else GroupInnerCorner,
        topEnd = if (!isLeadingTile && isFirst) GroupOuterCorner else GroupInnerCorner,
        bottomEnd = if (!isLeadingTile && isLast) GroupOuterCorner else GroupInnerCorner,
        bottomStart = if (isLeadingTile && isLast) GroupOuterCorner else GroupInnerCorner,
    )
