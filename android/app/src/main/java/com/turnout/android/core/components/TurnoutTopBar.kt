package com.turnout.android.core.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.turnout.android.core.theme.SpaceGroteskFontFamily
import com.turnout.android.core.utils.LocalAdaptiveConfig
import com.turnout.android.core.utils.TurnoutWindowSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurnoutTopBar(
    title: String,
    onNavigateBack: (() -> Unit)? = null,
    actions: List<Pair<ImageVector, () -> Unit>> = emptyList()
) {
    val windowSize = LocalAdaptiveConfig.current.windowSize

    // headlineLarge/titleLarge are both already Space Grotesk-family styles in
    // TurnoutTypography, so this picks the size, not the font — the family comes
    // along with whichever style is chosen.
    val titleStyle = if (windowSize == TurnoutWindowSize.Expanded) {
        MaterialTheme.typography.headlineLarge
    } else {
        MaterialTheme.typography.titleLarge
    }

    TopAppBar(
        title = {
            Text(
                text = title,
                style = titleStyle.copy(fontFamily = SpaceGroteskFontFamily)
            )
        },
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = {
            actions.forEach { (icon, action) ->
                IconButton(onClick = action) {
                    Icon(imageVector = icon, contentDescription = null)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}
