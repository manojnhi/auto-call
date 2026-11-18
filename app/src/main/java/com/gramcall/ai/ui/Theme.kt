package com.gramcall.ai.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF0B1F3A)
val NavySurface = Color(0xFF13294B)
val NavyCard = Color(0xFF1A355E)
val Green = Color(0xFF22C55E)
val Danger = Color(0xFFF87171)

private val scheme = darkColorScheme(
    primary = Green,
    onPrimary = Navy,
    background = Navy,
    onBackground = Color.White,
    surface = NavySurface,
    onSurface = Color.White,
    surfaceVariant = NavyCard,
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = Danger,
    outline = Color(0xFF64748B)
)

@Composable
fun GramCallTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
