package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// TTS Academy Brand Colors - Market Standard Emerald & Fresh Mint Palette
// User Specified: #4ca771, #C0E6BA, #EAF9E7
val BrandGreenPrimary = Color(0xFF4CA771)     // Primary Action & Branding #4ca771
val BrandGreenSecondary = Color(0xFFC0E6BA)   // Soft Accent / Pistachio Tint #C0E6BA
val BrandGreenSurface = Color(0xFFEAF9E7)     // Fresh Light Background Tint #EAF9E7

// Supporting Harmonious Brand Tones
val BrandGreenDark = Color(0xFF1E5235)        // Deep Forest for contrast headers
val BrandGreenDeep = Color(0xFF0F3120)        // Midnight Emerald
val BrandGreenVibrant = Color(0xFF388E3C)     // Active Vibrant Green
val BrandGreenDarkSurface = Color(0xFF13231A) // Dark Mode Surface
val BrandGreenDarkBackground = Color(0xFF0B1710) // Dark Mode Background

// Retain legacy aliases mapped to the new fresh green theme for backward compatibility
val AcademicBluePrimary = BrandGreenPrimary
val AcademicBlueDark = BrandGreenDark
val AcademicCobalt = BrandGreenPrimary
val AcademicSky = BrandGreenSecondary
val AcademicGold = Color(0xFFE6A23C)          // Warm Gold for streak fire & badges
val AcademicAmberDark = Color(0xFFC67D17)

// Modern Header Gradient Colors
val GradientDeepNavy = BrandGreenDark
val GradientOcean = Color(0xFF2E6B47)
val GradientTealNavy = BrandGreenPrimary

// Frosted Glassmorphism Tokens
val GlassWhite10 = Color(0x1AFFFFFF)
val GlassWhite15 = Color(0x26FFFFFF)
val GlassWhite20 = Color(0x33FFFFFF)
val GlassBorder = Color(0x3DFFFFFF)

// Functional Semantic Colors
val SuccessGreen = Color(0xFF4CA771)
val SuccessGreenLight = BrandGreenSurface
val SuccessGreenDark = Color(0xFF1E5235)

val ErrorRed = Color(0xFFE53935)
val ErrorRedLight = Color(0xFFFFEBEE)
val ErrorRedDark = Color(0xFFB71C1C)

val WarningYellow = Color(0xFFE6A23C)
val WarningYellowLight = Color(0xFFFFF8E1)

val InfoBlueLight = BrandGreenSurface

// Category & Accent Colors
val AccentCyan = Color(0xFF26A69A)
val AccentGold = Color(0xFFE6A23C)
val AccentEmerald = BrandGreenPrimary
val AccentRose = Color(0xFFE57373)
val AccentPurple = Color(0xFF7E57C2)

// Light Theme Neutrals (Crisp, High-Standard, Minimalist)
val LightBackground = Color(0xFFF6FAF6)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = BrandGreenSurface
val LightOutline = Color(0xFFCFE4D2)
val LightOnBackground = Color(0xFF14241B)
val LightOnSurface = Color(0xFF14241B)
val LightOnSurfaceVariant = Color(0xFF4A6353)

// Dark Theme Neutrals (Sleek Obsidian & Forest Emerald)
val DarkBackground = BrandGreenDarkBackground
val DarkSurface = BrandGreenDarkSurface
val DarkSurfaceVariant = Color(0xFF1B2F23)
val DarkOutline = Color(0xFF2D4D38)
val DarkOnBackground = Color(0xFFF1F7F2)
val DarkOnSurface = Color(0xFFF1F7F2)
val DarkOnSurfaceVariant = Color(0xFFA1B8A8)
val DarkPrimary = Color(0xFF6ED196)           // Accessible high-contrast mint green
val DarkSecondary = BrandGreenSecondary
val DarkTertiary = Color(0xFFFFD54F)
