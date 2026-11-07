package com.aftab.curioalarm.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * "Nightstand" palette: deep ink blues with a warm champagne accent.
 * Dark-only by design — this app is mostly used in the dark.
 */
object Palette {
    val Ink = Color(0xFF0A0D13)          // page background
    val Surface = Color(0xFF10151E)      // card
    val SurfaceHigh = Color(0xFF171E2A)  // raised card / inputs
    val Outline = Color(0xFF1E2634)      // hairline borders
    val Outline2 = Color(0xFF2C3648)     // stronger borders
    val Text = Color(0xFFEDF1F7)         // primary text
    val Muted = Color(0xFF8C97AB)        // secondary text
    val Accent = Color(0xFFE3C27E)       // champagne gold
    val AccentDim = Color(0xFFB99A5B)    // quieter gold
    val AccentSoft = Color(0xFF2B2517)   // warm tinted container
    val Danger = Color(0xFFE57A7A)       // soft red for destructive / error states
}

private val CurioScheme = darkColorScheme(
    primary = Palette.Accent,
    onPrimary = Palette.Ink,
    primaryContainer = Palette.AccentSoft,
    onPrimaryContainer = Palette.Accent,
    secondary = Palette.AccentDim,
    onSecondary = Palette.Ink,
    secondaryContainer = Palette.SurfaceHigh,
    onSecondaryContainer = Palette.Text,
    tertiary = Palette.AccentDim,
    onTertiary = Palette.Ink,
    tertiaryContainer = Palette.AccentSoft,
    onTertiaryContainer = Palette.Accent,
    background = Palette.Ink,
    onBackground = Palette.Text,
    surface = Palette.Surface,
    onSurface = Palette.Text,
    surfaceVariant = Palette.SurfaceHigh,
    onSurfaceVariant = Palette.Muted,
    surfaceContainerLowest = Palette.Ink,
    surfaceContainerLow = Palette.Surface,
    surfaceContainer = Palette.Surface,
    surfaceContainerHigh = Palette.SurfaceHigh,
    surfaceContainerHighest = Palette.SurfaceHigh,
    outline = Palette.Outline2,
    outlineVariant = Palette.Outline,
    error = Palette.Danger,
    onError = Palette.Ink
)

private val CurioShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val Base = Typography()

private val CurioTypography = Typography(
    displayLarge = Base.displayLarge.copy(
        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, letterSpacing = (-1).sp
    ),
    displayMedium = Base.displayMedium.copy(
        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, letterSpacing = (-0.5).sp
    ),
    displaySmall = Base.displaySmall.copy(
        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal
    ),
    headlineLarge = Base.headlineLarge.copy(
        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, letterSpacing = (-0.25).sp
    ),
    headlineMedium = Base.headlineMedium.copy(
        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal
    ),
    headlineSmall = Base.headlineSmall.copy(
        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal
    ),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.Medium),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp),
    titleSmall = Base.titleSmall.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp),
    bodyLarge = Base.bodyLarge.copy(letterSpacing = 0.1.sp),
    bodyMedium = Base.bodyMedium.copy(letterSpacing = 0.15.sp),
    bodySmall = Base.bodySmall.copy(letterSpacing = 0.2.sp),
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
    labelMedium = Base.labelMedium.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp),
    labelSmall = Base.labelSmall.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp)
)

@Composable
fun CurioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CurioScheme,
        typography = CurioTypography,
        shapes = CurioShapes,
        content = content
    )
}
