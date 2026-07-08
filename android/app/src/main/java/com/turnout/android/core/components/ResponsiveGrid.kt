package com.turnout.android.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Wraps LazyVerticalGrid with an adaptive column count (from AdaptiveLayoutConfig.columnCount).
 * "Falls back to a column on Compact" is implemented as a 1-column grid rather than switching
 * to a real LazyColumn — LazyColumn and LazyVerticalGrid use different scope types
 * (LazyListScope vs LazyGridScope), so a single content lambda can't drive both. A 1-column
 * LazyVerticalGrid renders identically to a LazyColumn, so this keeps one code path with no
 * visual difference on Compact screens.
 */
@Composable
fun ResponsiveGrid(
    columnCount: Int,
    itemSpacing: Dp,
    modifier: Modifier = Modifier,
    content: LazyGridScope.() -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount.coerceAtLeast(1)),
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        content = content
    )
}
