package com.turnout.android.core.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import com.turnout.android.core.theme.Blue
import com.turnout.android.core.theme.ErrorRed
import com.turnout.android.core.theme.TextPrimary

@Composable
fun TurnoutTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = leadingIcon?.let { icon ->
            { Icon(imageVector = icon, contentDescription = null) }
        },
        trailingIcon = trailingIcon,
        isError = isError,
        supportingText = if (isError && errorMessage != null) {
            { Text(text = errorMessage, color = ErrorRed) }
        } else null,
        visualTransformation = visualTransformation,
        singleLine = singleLine,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Blue,
            focusedLabelColor    = Blue,
            focusedLeadingIconColor = Blue,
            errorBorderColor     = ErrorRed,
            errorLabelColor      = ErrorRed,
            // Explicit, not inherited from the theme — this component is used inside
            // hardcoded white-background cards, so it can't rely on colorScheme.onSurface,
            // which may be white in a dark-first theme and become invisible here.
            focusedTextColor     = TextPrimary,
            unfocusedTextColor   = TextPrimary
        ),
        modifier = modifier.fillMaxWidth()
    )
}
