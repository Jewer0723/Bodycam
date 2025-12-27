package com.jewer.bodycam.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = White,
    secondary = DarkYellow,
    tertiary = Red
)

@Composable
fun BodycamTheme(
    content: @Composable () -> Unit
) {
    val colorScheme =DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}