package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = PrimaryIndigoDark,
    onPrimary = OnPrimaryContainerLight,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryBlueDark,
    onSecondary = Color(0xFF232742),
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = EntityGoalsDark,
    onTertiary = Color(0xFF08301F),
    tertiaryContainer = Color(0xFF124B37),
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainerLowest = Color(0xFF0B0C0E),
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = Color(0xFF2E3037),
    // Inverse surface (audit #12): "code snippet" tiles want a bg distinct
    // from the page so monospace code pops.
    inverseSurface = Color(0xFF1E2026),
    inverseOnSurface = Color(0xFFE9EAEC),
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorRedDark,
    errorContainer = ErrorContainerDark,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryIndigo,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryBlue,
    onSecondary = Color.White,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = EntityGoals,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF064E3B),
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceContainerLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = Color(0xFFD7DDE6),
    // Inverse surface (audit #12): same as dark-mode value — code tiles stay
    // on a navy chip regardless of theme so monospace text always contrasts.
    // Swapping to white-on-navy would collapse the chip into the page bg.
    inverseSurface = Color(0xFF283044),
    inverseOnSurface = Color(0xFFEEF0FF),
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorRed,
    errorContainer = ErrorContainerLight,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

