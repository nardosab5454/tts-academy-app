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
    onPrimary = Color(0xFF0A2214),
    primaryContainer = Color(0xFF1E5235),
    onPrimaryContainer = BrandGreenSurface,
    secondary = DarkSecondary,
    onSecondary = Color(0xFF10331C),
    secondaryContainer = Color(0xFF1B3B26),
    onSecondaryContainer = BrandGreenSecondary,
    tertiary = DarkTertiary,
    onTertiary = Color(0xFF3E2723),
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BrandGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandGreenSurface,
    onPrimaryContainer = Color(0xFF134327),
    secondary = BrandGreenSecondary,
    onSecondary = Color(0xFF1B4D33),
    secondaryContainer = BrandGreenSurface,
    onSecondaryContainer = Color(0xFF1B4D33),
    tertiary = AcademicGold,
    onTertiary = Color.White,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep consistent modern brand identity
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
