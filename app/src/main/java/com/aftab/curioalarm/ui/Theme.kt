package com.aftab.curioalarm.ui

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/** Appearance choice, persisted in SharedPreferences. */
enum class ThemeMode(val title: String, val subtitle: String) {
    SYSTEM("System", "Follow the device appearance"),
    LIGHT("Light", "White background, black controls"),
    DARK("Dark", "Black background, white controls");

    companion object {
        fun parse(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

object ThemePrefs {
    private const val FILE = "curio_ui"
    private const val KEY = "theme_mode"

    fun load(context: Context): ThemeMode =
        ThemeMode.parse(context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null))

    fun save(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, mode.name).apply()
    }
}

/**
 * Monochrome design tokens. Two fixed palettes — no tinted accents, no gradients.
 * [accent] is the foreground inverse of the background: black on light, white on dark.
 */
@Immutable
data class CurioColors(
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val outline: Color,
    val outline2: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val accentDim: Color,
    val accentSoft: Color,
    val danger: Color,
    val isDark: Boolean
)

val LightCurioColors = CurioColors(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFF4F4F5),
    outline = Color(0xFFE4E4E7),
    outline2 = Color(0xFFD4D4D8),
    text = Color(0xFF18181B),
    muted = Color(0xFF71717A),
    accent = Color(0xFF18181B),
    onAccent = Color(0xFFFFFFFF),
    accentDim = Color(0xFF52525B),
    accentSoft = Color(0xFFF4F4F5),
    danger = Color(0xFFB42318),
    isDark = false
)

val DarkCurioColors = CurioColors(
    background = Color(0xFF09090B),
    surface = Color(0xFF121214),
    surfaceHigh = Color(0xFF1C1C1F),
    outline = Color(0xFF232327),
    outline2 = Color(0xFF35353A),
    text = Color(0xFFFAFAFA),
    muted = Color(0xFFA1A1AA),
    accent = Color(0xFFFAFAFA),
    onAccent = Color(0xFF18181B),
    accentDim = Color(0xFFD4D4D8),
    accentSoft = Color(0xFF202024),
    danger = Color(0xFFF97066),
    isDark = true
)

private val LocalCurioColors = staticCompositionLocalOf { DarkCurioColors }

/**
 * Semantic colors used across the app. Getters are composable so every screen
 * re-reads them when the theme changes — no hardcoded colors anywhere else.
 */
object Palette {
    val Ink: Color @Composable get() = LocalCurioColors.current.background
    val Surface: Color @Composable get() = LocalCurioColors.current.surface
    val SurfaceHigh: Color @Composable get() = LocalCurioColors.current.surfaceHigh
    val Outline: Color @Composable get() = LocalCurioColors.current.outline
    val Outline2: Color @Composable get() = LocalCurioColors.current.outline2
    val Text: Color @Composable get() = LocalCurioColors.current.text
    val Muted: Color @Composable get() = LocalCurioColors.current.muted
    val Accent: Color @Composable get() = LocalCurioColors.current.accent
    val OnAccent: Color @Composable get() = LocalCurioColors.current.onAccent
    val AccentDim: Color @Composable get() = LocalCurioColors.current.accentDim
    val AccentSoft: Color @Composable get() = LocalCurioColors.current.accentSoft
    val Danger: Color @Composable get() = LocalCurioColors.current.danger
    val IsDark: Boolean @Composable get() = LocalCurioColors.current.isDark
}

private fun schemeFor(c: CurioColors) = if (c.isDark) darkColorScheme(
    primary = c.accent, onPrimary = c.onAccent,
    primaryContainer = c.accentSoft, onPrimaryContainer = c.text,
    secondary = c.accentDim, onSecondary = c.onAccent,
    secondaryContainer = c.surfaceHigh, onSecondaryContainer = c.text,
    tertiary = c.accentDim, onTertiary = c.onAccent,
    tertiaryContainer = c.accentSoft, onTertiaryContainer = c.text,
    background = c.background, onBackground = c.text,
    surface = c.surface, onSurface = c.text,
    surfaceVariant = c.surfaceHigh, onSurfaceVariant = c.muted,
    surfaceContainerLowest = c.background, surfaceContainerLow = c.surface,
    surfaceContainer = c.surface, surfaceContainerHigh = c.surfaceHigh,
    surfaceContainerHighest = c.surfaceHigh,
    outline = c.outline2, outlineVariant = c.outline,
    error = c.danger, onError = c.onAccent
) else lightColorScheme(
    primary = c.accent, onPrimary = c.onAccent,
    primaryContainer = c.accentSoft, onPrimaryContainer = c.text,
    secondary = c.accentDim, onSecondary = c.onAccent,
    secondaryContainer = c.surfaceHigh, onSecondaryContainer = c.text,
    tertiary = c.accentDim, onTertiary = c.onAccent,
    tertiaryContainer = c.accentSoft, onTertiaryContainer = c.text,
    background = c.background, onBackground = c.text,
    surface = c.surface, onSurface = c.text,
    surfaceVariant = c.surfaceHigh, onSurfaceVariant = c.muted,
    surfaceContainerLowest = c.background, surfaceContainerLow = c.surface,
    surfaceContainer = c.surface, surfaceContainerHigh = c.surfaceHigh,
    surfaceContainerHighest = c.surfaceHigh,
    outline = c.outline2, outlineVariant = c.outline,
    error = c.danger, onError = c.onAccent
)

private val CurioShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

private val Base = Typography()

/** One quiet sans-serif scale — no display serif, generous but consistent weights. */
private val CurioTypography = Typography(
    displayLarge = Base.displayLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp),
    displayMedium = Base.displayMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    displaySmall = Base.displaySmall.copy(fontWeight = FontWeight.SemiBold),
    headlineLarge = Base.headlineLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.25).sp),
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
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
fun CurioTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkCurioColors else LightCurioColors

    // Match the system bar icons to the app theme (activities are edge-to-edge).
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(LocalCurioColors provides colors) {
        MaterialTheme(
            colorScheme = schemeFor(colors),
            typography = CurioTypography,
            shapes = CurioShapes,
            content = content
        )
    }
}
