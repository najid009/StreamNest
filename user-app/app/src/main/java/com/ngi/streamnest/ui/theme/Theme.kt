package com.ngi.streamnest.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NestColors = darkColorScheme(
    primary = Color(0xFF39E58C),
    onPrimary = Color(0xFF00210F),
    secondary = Color(0xFFFF9F1C),
    onSecondary = Color(0xFF2B1700),
    background = Color(0xFF0B1B2B),
    onBackground = Color(0xFFEAF2F8),
    surface = Color(0xFF13293D),
    onSurface = Color(0xFFEAF2F8),
    surfaceVariant = Color(0xFF1C3750),
    onSurfaceVariant = Color(0xFFB8C7D6),
    error = Color(0xFFFF5A5F),
)

/** StreamNest সবসময় dark theme-এ থাকবে (স্পোর্টস অ্যাপের জন্য) */
@Composable
fun StreamNestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NestColors,
        content = content,
    )
}
