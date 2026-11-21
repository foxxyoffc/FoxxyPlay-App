package com.foxxy.play.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable

private val FoxxyDarkScheme = darkColorScheme(
    primary = FoxxyGold,
    onPrimary = FoxxyBackground,
    primaryContainer = Color(0xFF3A2D18),
    onPrimaryContainer = FoxxyGoldLight,
    secondary = FoxxyGoldLight,
    onSecondary = FoxxyBackground,
    secondaryContainer = Color(0xFF292218),
    onSecondaryContainer = FoxxyText,
    background = FoxxyBackground,
    surface = FoxxySurface,
    surfaceVariant = FoxxySurfaceVariant,
    onBackground = FoxxyText,
    onSurface = FoxxyText,
    onSurfaceVariant = FoxxyMuted,
    outline = FoxxyGoldDark
)

@Composable
fun FoxxyPlayTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FoxxyDarkScheme,
        content = content
    )
}
