package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = SleekBluePrimary,
    onPrimary = SleekBlueOnPrimary,
    primaryContainer = SleekBlueContainer,
    onPrimaryContainer = SleekBlueOnContainer,
    secondary = SleekSecondary,
    onSecondary = SleekOnSecondary,
    secondaryContainer = SleekSecondaryContainer,
    onSecondaryContainer = SleekOnSecondaryContainer,
    tertiary = SleekTertiaryPurple,
    tertiaryContainer = SleekTertiaryContainer,
    onTertiaryContainer = SleekOnTertiaryContainer,
    error = SleekError,
    errorContainer = SleekErrorContainer,
    onErrorContainer = SleekOnErrorContainer,
    background = SleekBackground,
    onBackground = SleekTextPrimary,
    surface = SleekSurface,
    onSurface = SleekTextPrimary,
    surfaceVariant = SleekSurfaceVariant,
    onSurfaceVariant = SleekTextSecondary,
    outline = SleekOutline,
    outlineVariant = SleekOutlineVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = SleekBluePrimaryDark,
    onPrimary = SleekBlueOnPrimaryDark,
    primaryContainer = SleekBlueContainerDark,
    onPrimaryContainer = SleekBlueOnContainerDark,
    secondary = SleekSecondaryDark,
    secondaryContainer = SleekSecondaryContainerDark,
    background = SleekBackgroundDark,
    onBackground = SleekTextPrimaryDark,
    surface = SleekSurfaceDark,
    onSurface = SleekTextPrimaryDark,
    surfaceVariant = SleekSurfaceVariantDark,
    onSurfaceVariant = SleekTextSecondaryDark,
    outline = SleekOutlineDark
)

private val SiraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun SiraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SiraShapes,
        content = content
    )
}
