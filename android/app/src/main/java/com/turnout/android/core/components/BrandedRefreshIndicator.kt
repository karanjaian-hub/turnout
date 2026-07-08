package com.turnout.android.core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Custom pull-to-refresh built on the older, still-functional androidx.compose.material
 * pullRefresh API (Material3's own PullToRefreshBox needs a newer Material3 version than
 * this project currently pins — see compose-bom in libs.versions.toml). The default
 * PullRefreshIndicator spinner is intentionally never used here; instead PulseLine sweeps
 * across a 2dp strip at the top, driven by the same pull state.
 *
 * Usage: wrap scrollable content's Modifier with .pullRefresh(state) from
 * rememberBrandedRefreshState(), and place this composable as a sibling at the top.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberBrandedRefreshState(
    isRefreshing: Boolean,
    onRefresh: () -> Unit
): PullRefreshState = rememberPullRefreshState(
    refreshing = isRefreshing,
    onRefresh = onRefresh,
    refreshThreshold = 80.dp
)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BrandedRefreshIndicator(
    pullRefreshState: PullRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    // state.progress is 0 at rest and increases toward/past 1 as the user pulls down —
    // used here both to decide whether PulseLine shows any motion at all, and to make
    // it sweep faster the further past the threshold the user has pulled.
    val isActive = isRefreshing || pullRefreshState.progress > 0f
    val speed = if (isRefreshing) 1f else (0.5f + pullRefreshState.progress.coerceIn(0f, 1f))

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        PulseLine(
            isActive = isActive,
            speedMultiplier = speed
        )
    }
}
