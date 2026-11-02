/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * TV / Tablet: deliberate dark, low-chroma palette for ten-foot living room viewing.
 * Preserves the original OpenTV theme for non-phone devices.
 */
private val TvAccent = Color(0xFF7C93FF)
private val TvAccentDim = Color(0xFF4B5DB8)

private val TvDarkScheme = darkColorScheme(
    primary = TvAccent,
    onPrimary = Color(0xFF0A0C14),
    primaryContainer = TvAccentDim,
    onPrimaryContainer = Color(0xFFE6EAFF),
    secondary = Color(0xFF9AA3C0),
    background = Color(0xFF07080C),
    onBackground = Color(0xFFE8EAF0),
    surface = Color(0xFF0D0F16),
    onSurface = Color(0xFFE8EAF0),
    surfaceVariant = Color(0xFF171A24),
    onSurfaceVariant = Color(0xFFA8AEC0),
    outline = Color(0xFF2A2F3D),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF1A0505),
)

/**
 * Mobile Phone: Deep black and vibrant red palette inspired by modern mobile streaming aesthetics (REC TV).
 * Crimson/red highlights for active tabs, buttons, badges, and key affordances.
 */
private val MobileAccent = Color(0xFFE50914)

private val MobileDarkScheme = darkColorScheme(
    primary = MobileAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF380C0E),
    onPrimaryContainer = Color(0xFFFFDADA),
    secondary = Color(0xFFE43B42),
    onSecondary = Color.White,
    background = Color(0xFF08080B),
    onBackground = Color(0xFFF2F2F5),
    surface = Color(0xFF101218),
    onSurface = Color(0xFFF2F2F5),
    surfaceVariant = Color(0xFF181B24),
    onSurfaceVariant = Color(0xFFA2A6B6),
    outline = Color(0xFF262936),
    error = Color(0xFFFF4D4D),
    onError = Color(0xFF1A0505),
)

/** Light mode fallback. */
private val LightScheme = lightColorScheme(
    primary = Color(0xFF3B4FCC),
    onPrimary = Color.White,
    background = Color(0xFFFBFBFE),
    onBackground = Color(0xFF13141A),
    surface = Color.White,
    onSurface = Color(0xFF13141A),
    surfaceVariant = Color(0xFFEEF0F6),
    onSurfaceVariant = Color(0xFF4A4F60),
    outline = Color(0xFFD3D7E2),
)

/** Scaled typography tuned for both handheld mobile and ten-foot viewing. */
private val OpenTvTypography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun OpenTvTheme(
    isPhone: Boolean = true,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val darkScheme = if (isPhone) MobileDarkScheme else TvDarkScheme
    val lightScheme = if (isPhone) LightScheme.copy(
        primary = MobileAccent,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFDADA),
        onPrimaryContainer = Color(0xFF410002),
        secondary = Color(0xFFB81D24),
    ) else LightScheme
    MaterialTheme(
        colorScheme = if (darkTheme) darkScheme else lightScheme,
        typography = OpenTvTypography,
        content = content,
    )
}
