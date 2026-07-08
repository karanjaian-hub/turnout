package com.turnout.android.core.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * List/detail split, meant for TurnoutWindowSize.Expanded only (tablets, open foldables).
 * On Compact/Medium, the caller shouldn't even reach this composable — that decision belongs
 * to the screen itself, since only it knows whether to show listPane alone or push to detailPane
 * as a full-screen replacement instead of a side-by-side split.
 *
 * showDetail toggles whether the right pane is rendered at all — false means detailPane
 * simply isn't shown yet (e.g. no item selected), not that it's hidden off-screen.
 */
@Composable
fun TwoPaneLayout(
    listPane: @Composable () -> Unit,
    detailPane: @Composable () -> Unit,
    showDetail: Boolean,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
        ) {
            listPane()
        }

        if (showDetail) {
            Box(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
            ) {
                AnimatedContent(
                    targetState = showDetail,
                    label = "twoPaneDetailTransition",
                    transitionSpec = {
                        fadeIn(animationSpec = androidx.compose.animation.core.tween(220)) togetherWith
                            fadeOut(animationSpec = androidx.compose.animation.core.tween(180))
                    }
                ) { detailVisible ->
                    if (detailVisible) {
                        detailPane()
                    }
                }
            }
        }
    }
}
