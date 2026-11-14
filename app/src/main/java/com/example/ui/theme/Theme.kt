package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = MidnightTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E6F2),
    onPrimaryContainer = MidnightTeal,
    secondary = SolarAmberDark,
    onSecondary = Color.White,
    secondaryContainer = SolarAmberSoft,
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = ElectricEmeraldDark,
    onTertiary = Color.White,
    tertiaryContainer = ElectricEmeraldSoft,
    onTertiaryContainer = Color(0xFF064E3B),
    error = OverdueCrimson,
    onError = Color.White,
    errorContainer = OverdueCrimsonSoft,
    onErrorContainer = Color(0xFF7F1D1D),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = Color(0xFFE2E8F0)
)

private val DarkColors = darkColorScheme(
    primary = SolarAmber,
    onPrimary = Color(0xFF1F1600),
    primaryContainer = DeepSlate,
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = ElectricEmerald,
    onSecondary = Color(0xFF002114),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = ElectricEmeraldSoft,
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF002233),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = OverdueCrimsonSoft,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = Color(0xFF1E293B)
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BijliPayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
