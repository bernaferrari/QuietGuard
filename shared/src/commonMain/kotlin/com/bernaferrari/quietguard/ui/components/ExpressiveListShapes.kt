package com.bernaferrari.quietguard.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * M3 Expressive grouped-list corners: large radius on the group's outer edges,
 * small radius between items so a stack of filled surfaces reads as one group.
 * Every grouped list in the app (apps, logs, settings, rule detail) uses these.
 */
val GroupOuterCorner = 20.dp
val GroupInnerCorner = 4.dp

/** Where an item sits inside a visually connected group. */
enum class GroupPosition(val isFirst: Boolean, val isLast: Boolean) {
    Single(isFirst = true, isLast = true),
    First(isFirst = true, isLast = false),
    Middle(isFirst = false, isLast = false),
    Last(isFirst = false, isLast = true);

    val topCorner: Dp get() = if (isFirst) GroupOuterCorner else GroupInnerCorner
    val bottomCorner: Dp get() = if (isLast) GroupOuterCorner else GroupInnerCorner

    companion object {
        fun of(isFirst: Boolean, isLast: Boolean): GroupPosition = when {
            isFirst && isLast -> Single
            isFirst -> First
            isLast -> Last
            else -> Middle
        }

        fun of(index: Int, count: Int): GroupPosition =
            of(isFirst = index == 0, isLast = index == count - 1)
    }
}

fun groupItemShape(position: GroupPosition): RoundedCornerShape =
    RoundedCornerShape(
        topStart = position.topCorner,
        topEnd = position.topCorner,
        bottomEnd = position.bottomCorner,
        bottomStart = position.bottomCorner,
    )

fun groupItemShape(isFirst: Boolean, isLast: Boolean): RoundedCornerShape =
    groupItemShape(GroupPosition.of(isFirst, isLast))

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
