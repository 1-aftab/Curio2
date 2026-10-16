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

object Palette {
    val Black = Color(0xFF0A0A0B)        // near-black background
    val Surface = Color(0xFF141416)      // charcoal card
    val SurfaceHigh = Color(0xFF1E1E21)  // raised charcoal
    val Outline = Color(0xFF26262A)      // hairline borders
    val Outline2 = Color(0xFF3A3A40)     // stronger borders
    val Text = Color(0xFFF2F2F4)         // white-ish
    val Muted = Color(0xFF8B8B93)        // muted grey labels
    val Silver = Color(0xFFC9CBD2)       // metallic silver accent
    val SilverDim = Color(0xFF9EA0A8)
}

private val CurioScheme = darkColorScheme(
    primary = Palette.Silver,
    onPrimary = Palette.Black,
    primaryContainer = Palette.SurfaceHigh,
    onPrimaryContainer = Palette.Text,
    secondary = Palette.SilverDim,
    onSecondary = Palette.Black,
    secondaryContainer = Palette.SurfaceHigh,
    onSecondaryContainer = Palette.Text,
    tertiary = Palette.SilverDim,
    onTertiary = Palette.Black,
    background = Palette.Black,
    onBackground = Palette.Text,
    surface = Palette.Surface,
    onSurface = Palette.Text,
    surfaceVariant = Palette.SurfaceHigh,
    onSurfaceVariant = Palette.Muted,
    surfaceContainerLowest = Palette.Black,
    surfaceContainerLow = Palette.Surface,
    surfaceContainer = Palette.Surface,
    surfaceContainerHigh = Palette.SurfaceHigh,
    surfaceContainerHighest = Palette.SurfaceHigh,
    outline = Palette.Outline2,
    outlineVariant = Palette.Outline,
    error = Color(0xFFE6E6EA),
    onError = Palette.Black
)

private val CurioShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun CurioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CurioScheme,
        typography = Typography(
            displayLarge = Typography().displayLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light, letterSpacing = (-0.5).sp),
            displayMedium = Typography().displayMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light),
            displaySmall = Typography().displaySmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light),
            headlineLarge = Typography().headlineLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, letterSpacing = (-0.25).sp),
            headlineMedium = Typography().headlineMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium),
            headlineSmall = Typography().headlineSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium),
            titleLarge = Typography().titleLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium),
            titleMedium = Typography().titleMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp),
            titleSmall = Typography().titleSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp),
            bodyLarge = Typography().bodyLarge.copy(fontFamily = FontFamily.SansSerif, letterSpacing = 0.1.sp),
            bodyMedium = Typography().bodyMedium.copy(fontFamily = FontFamily.SansSerif, letterSpacing = 0.15.sp),
            bodySmall = Typography().bodySmall.copy(fontFamily = FontFamily.SansSerif, letterSpacing = 0.2.sp),
            labelLarge = Typography().labelLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
            labelMedium = Typography().labelMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp),
            labelSmall = Typography().labelSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp)
        ),
        shapes = CurioShapes,
        content = content
    )
}
