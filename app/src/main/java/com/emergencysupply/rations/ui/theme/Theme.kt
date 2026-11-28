package com.emergencysupply.rations.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealSoft,
    onPrimaryContainer = TealPrimary,
    secondary = InkBlack,
    onSecondary = Color.White,
    secondaryContainer = SoftTileSurface,
    onSecondaryContainer = InkBlack,
    background = BackgroundSurface,
    onBackground = MidnightSlate,
    surface = CardSurface,
    onSurface = MidnightSlate,
    surfaceVariant = SoftTileSurface,
    onSurfaceVariant = MediumSlate,
    outline = BorderSlate,
    outlineVariant = Color(0xFFF1F5F9)
)

@Composable
fun EmergencySupplyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
