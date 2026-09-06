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
    onSecondary = Color(0xFF082F49),
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = EntityGoalsDark,
    onTertiary = Color(0xFF053225),
    tertiaryContainer = Color(0xFF0F4F3C),
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainerLowest = Color(0xFF050913),
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = Color(0xFF3F4F66),
    // Inverse surface (audit #12): "code snippet" tiles in dark mode want a
    // darker, slightly warmer bg than the page surface so monospace code pops.
    // On light mode we want the opposite — a tinted navy chip on a white page.
    inverseSurface = Color(0xFF283044),
    inverseOnSurface = Color(0xFFEEF0FF),
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

