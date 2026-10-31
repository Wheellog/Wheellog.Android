package com.cooper.wheellog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.cooper.wheellog.utils.SomeUtil.getColorEx
import com.cooper.wheellog.utils.ThemeManager
import com.cooper.wheellog.R


private val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
)


private val DarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
)

@Composable
fun AppTheme(
  useDarkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable() () -> Unit
) {
  val palette = ThemeManager.palette
  val context = LocalContext.current
  val colors = if (palette != null) {
    // Settings text has its own readable foreground; the dashboard remains red.
    val foreground = Color(context.getColorEx(
      if (ThemeManager.theme == R.style.InfernoTheme) R.color.inferno_battery else palette.foreground
    ))
    val muted = Color(context.getColorEx(palette.muted))
    val accent = Color(context.getColorEx(palette.accent))
    val background = Color(context.getColorEx(palette.background))
    val surface = Color(context.getColorEx(palette.surface))
    darkColorScheme(
      primary = accent, onPrimary = if (androidx.core.graphics.ColorUtils.calculateLuminance(context.getColorEx(palette.accent)) < .179) Color.White else Color.Black,
      primaryContainer = surface, onPrimaryContainer = foreground,
      secondary = accent, onSecondary = if (androidx.core.graphics.ColorUtils.calculateLuminance(context.getColorEx(palette.accent)) < .179) Color.White else Color.Black,
      secondaryContainer = surface, onSecondaryContainer = muted,
      tertiary = Color(context.getColorEx(palette.temperature)), onTertiary = Color.Black,
      background = background, onBackground = foreground,
      surface = surface, onSurface = foreground,
      surfaceVariant = surface, onSurfaceVariant = muted,
      outline = muted, outlineVariant = Color(context.getColorEx(palette.arc)),
      surfaceTint = accent,
      error = Color.Red, onError = Color.Black,
      surfaceBright = surface, surfaceDim = background,
      surfaceContainer = surface, surfaceContainerLow = surface,
      surfaceContainerLowest = background, surfaceContainerHigh = surface,
      surfaceContainerHighest = surface,
    )
  } else if (!useDarkTheme) {
    LightColors
  } else {
    DarkColors
  }

  MaterialTheme(
    colorScheme = colors,
    content = content
  )
}
