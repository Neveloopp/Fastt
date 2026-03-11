package com.fastt.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AccentOptions = listOf(
    Color(0xFF7A4DFF),
    Color(0xFF00C2FF),
    Color(0xFFFF4D8D),
    Color(0xFF2EE59D),
    Color(0xFFFFC84D)
)

@Composable
fun FasttTheme(
    themeMode: String,
    accentIndex: Int,
    content: @Composable () -> Unit
) {
    val sysDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> sysDark
    }

    val accent = AccentOptions.getOrElse(accentIndex) { AccentOptions[0] }

    val scheme = if (dark) darkColorScheme(
        primary = accent,
        secondary = accent,
        tertiary = accent
    ) else lightColorScheme(
        primary = accent,
        secondary = accent,
        tertiary = accent
    )

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(),
        content = content
    )
}

fun accentOptionsCount(): Int = AccentOptions.size
