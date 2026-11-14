package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF081B3E),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = BrandBlueSurface,
    secondary = DarkSecondary,
    onSecondary = Color(0xFF0A244D),
    secondaryContainer = Color(0xFF162C54),
    onSecondaryContainer = BrandBlueSecondary,
    tertiary = DarkTertiary,
    onTertiary = Color(0xFF3E2723),
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = Color(0xFF1E3154)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BrandBlueBright,
    onPrimary = Color.White,
    primaryContainer = BrandBlueSurface,
    onPrimaryContainer = BrandBlueDark,
    secondary = BrandBlueSecondary,
    onSecondary = BrandBlueDark,
    secondaryContainer = BrandBlueSurface,
    onSecondaryContainer = BrandBluePrimary,
    tertiary = AcademicGold,
    onTertiary = Color.White,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = Color(0xFFE2E8F0)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep consistent White & Blue brand identity
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
