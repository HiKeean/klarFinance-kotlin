package com.klarfinance.app.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val KlarLightColorScheme = lightColorScheme(
    primary = KlarTeal,
    onPrimary = KlarSurface,
    primaryContainer = KlarTealLight,
    onPrimaryContainer = KlarTealDark,
    background = KlarBackground,
    onBackground = KlarTextPrimary,
    surface = KlarSurface,
    onSurface = KlarTextPrimary,
    surfaceVariant = KlarBackground,
    onSurfaceVariant = KlarTextSecondary,
    outline = KlarOutline,
    error = KlarError,
)

@Composable
fun KlarFinanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // MVP ships a single light palette matching the approved Figma design;
    // dark theme can be layered in once designs for it exist.
    MaterialTheme(
        colorScheme = KlarLightColorScheme,
        typography = KlarTypography,
        content = content,
    )
}
