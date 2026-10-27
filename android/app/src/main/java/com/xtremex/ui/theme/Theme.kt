package com.xtremex.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Tokens mirrored from the old app's theme/tokens.ts.
// Primary #111111, backgrounds white / #f7f7f7, muted text #666666.
private val XtremeXColorScheme = lightColorScheme(
    primary = Color(0xFF111111),
    onPrimary = Color.White,
    secondary = Color(0xFF444444),
    background = Color.White,
    surface = Color.White,
    surfaceVariant = Color(0xFFF7F7F7),
    onBackground = Color(0xFF111111),
    onSurface = Color(0xFF111111),
    onSurfaceVariant = Color(0xFF666666),
    outline = Color(0xFFE0E0E0)
)

@Composable
fun XtremeXTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = XtremeXColorScheme,
        content = content
    )
}
