package com.yarnl.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val YarnlDarkColorScheme = darkColorScheme(
    primary = YarnlOrange,
    onPrimary = YarnlDarker,
    primaryContainer = YarnlOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = YarnlPurple,
    onSecondary = Color.White,
    secondaryContainer = YarnlLavender,
    onSecondaryContainer = YarnlDarker,
    background = YarnlDarker,
    onBackground = YarnlTextPrimary,
    surface = YarnlDark,
    onSurface = YarnlTextPrimary,
    surfaceVariant = YarnlSurface,
    onSurfaceVariant = YarnlTextDim,
    error = YarnlError,
    onError = Color.White,
    outline = YarnlBorder,
)

@Composable
fun YarnlTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = YarnlDarkColorScheme,
        typography = YarnlTypography,
        content = content,
    )
}
