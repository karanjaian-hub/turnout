package com.turnout.android.core.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import com.turnout.android.core.theme.AccentBlue
import com.turnout.android.core.theme.TextOnCanvas
import com.turnout.android.core.theme.TextOnCanvasSecondary

/**
 * Dark-canvas-themed text field for auth screens (Login, Register, OTP, Forgot/Reset
 * Password) — these are the only screens with a dark background in the app, so this
 * lives as its own component rather than a "dark mode" flag bolted onto the regular
 * TurnoutTextField used everywhere else.
 */
@Composable
fun DarkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextOnCanvasSecondary) },
        leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = TextOnCanvasSecondary) },
        trailingIcon = trailingIcon,
        isError = isError,
        supportingText = if (isError && errorMessage != null) {
            { Text(errorMessage, color = androidx.compose.ui.graphics.Color(0xFFDC2626)) }
        } else null,
        visualTransformation = visualTransformation,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentBlue,
            unfocusedBorderColor = Color(0x1AFFFFFF),
            errorBorderColor = Color(0xFFDC2626),
            focusedTextColor = TextOnCanvas,
            unfocusedTextColor = TextOnCanvas,
            focusedContainerColor = Color(0x0DFFFFFF),
            unfocusedContainerColor = Color(0x0DFFFFFF),
            cursorColor = AccentBlue
        ),
        modifier = modifier.fillMaxWidth()
    )
}
