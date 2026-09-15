package com.klarfinance.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun OtpInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    otpLength: Int = 6,
    requestFocusOnAppear: Boolean = true,
) {
    val focusRequester = remember { FocusRequester() }

    BasicTextField(
        value = value,
        onValueChange = { new ->
            val sanitized = new.filter(Char::isDigit).take(otpLength)
            onValueChange(sanitized)
        },
        modifier = modifier.focusRequester(focusRequester),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        textStyle = TextStyle.Default.copy(color = androidx.compose.ui.graphics.Color.Transparent),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.Transparent),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(otpLength) { index ->
                    val char = value.getOrNull(index)?.toString().orEmpty()
                    val isFocused = index == value.length
                    OtpCell(char = char, isFocused = isFocused)
                }
            }
        },
    )

    if (requestFocusOnAppear) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
}

@Composable
private fun OtpCell(char: String, isFocused: Boolean) {
    val borderColor = if (isFocused || char.isNotEmpty()) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .border(width = if (isFocused) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = char, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}
