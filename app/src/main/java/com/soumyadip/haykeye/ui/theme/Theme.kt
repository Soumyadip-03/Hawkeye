package com.soumyadip.haykeye.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlueLight,
    onPrimary = DeepNavyBackground,
    primaryContainer = ElectricBlueContainer,
    onPrimaryContainer = OnNavyPrimary,
    secondary = ElectricBluePrimary,
    background = DeepNavyBackground,
    surface = DeepNavySurface,
    surfaceVariant = DeepNavySurfaceVariant,
    onBackground = OnNavyPrimary,
    onSurface = OnNavyPrimary,
    onSurfaceVariant = OnNavySecondary,
)

@Composable
fun HaykeyeTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content,
    )
}
