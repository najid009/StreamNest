package com.ngi.streamnest.admin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Admin অ্যাপে কমলা accent, যাতে user অ্যাপ থেকে আলাদা বোঝা যায় */
private val AdminColors = darkColorScheme(
    primary = Color(0xFFFF9F1C),
    onPrimary = Color(0xFF2B1700),
    secondary = Color(0xFF39E58C),
    onSecondary = Color(0xFF00210F),
    background = Color(0xFF0B1B2B),
    onBackground = Color(0xFFEAF2F8),
    surface = Color(0xFF13293D),
    onSurface = Color(0xFFEAF2F8),
    surfaceVariant = Color(0xFF1C3750),
    onSurfaceVariant = Color(0xFFB8C7D6),
    error = Color(0xFFFF5A5F),
)

@Composable
fun AdminTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AdminColors, content = content)
}
