package com.zakeleo877.saeedcore.designsystem

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The shared identity approved for both apps: deep green, a quiet light page
 * and a restrained warm accent. Text/surface pairs are checked in
 * ColorContrastTest; the amber is never used as light text on a light surface.
 */
val SaeedGreen = Color(0xFF173F38)
val SaeedGreenDark = Color(0xFF1C5446)
val SaeedGreenHeader = Color(0xFF173F38)
val SaeedInk = Color(0xFF19312B)
val SaeedInkMuted = Color(0xFF546A5E)
val SaeedPage = Color(0xFFFBFCF9)
val SaeedCardSurface = Color(0xFFFFFFFF)
val SaeedChipSurface = Color(0xFFEDF3EE)
val SaeedGreenSoft = Color(0xFFE4F0E6)
val SaeedGold = Color(0xFFE9B96A)
val SaeedGoldSoft = Color(0xFFF9EFD9)
val SaeedGoldInk = Color(0xFF78551F)
val SaeedOutline = Color(0xFFCBDAD0)
val SaeedDiscountRed = Color(0xFFD62828)
val SaeedStar = Color(0xFF9A5600)

val SemanticError = Color(0xFFB3261E)
val SemanticErrorContainer = Color(0xFFF9DEDC)
val SemanticOnErrorContainer = Color(0xFF410E0B)
val SemanticSuccess = Color(0xFF1B6B3A)
val SemanticWarning = Color(0xFF7A4F01)

/** Fixed light scheme; both apps keep the same identity on every device. */
val SaeedColorScheme = lightColorScheme(
    primary = SaeedGreen,
    onPrimary = Color.White,
    primaryContainer = SaeedGreenSoft,
    onPrimaryContainer = SaeedGreen,
    secondary = SaeedGreenDark,
    onSecondary = Color.White,
    secondaryContainer = SaeedGreenSoft,
    onSecondaryContainer = SaeedGreen,
    tertiary = SaeedDiscountRed,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDAD6),
    onTertiaryContainer = Color(0xFF410002),
    background = SaeedPage,
    onBackground = SaeedInk,
    surface = SaeedCardSurface,
    onSurface = SaeedInk,
    surfaceVariant = SaeedChipSurface,
    onSurfaceVariant = SaeedInkMuted,
    surfaceContainerLowest = SaeedCardSurface,
    surfaceContainerLow = SaeedCardSurface,
    surfaceContainer = SaeedCardSurface,
    surfaceContainerHigh = SaeedCardSurface,
    surfaceContainerHighest = SaeedChipSurface,
    outline = SaeedOutline,
    outlineVariant = SaeedOutline,
    error = SemanticError,
    onError = Color.White,
    errorContainer = SemanticErrorContainer,
    onErrorContainer = SemanticOnErrorContainer,
    scrim = Color.Black,
)

// Compatibility aliases for older callers.
@Deprecated("Use SaeedGreen", ReplaceWith("SaeedGreen"))
val ElectricCyan = SaeedGreen
@Deprecated("Use SaeedGreenDark", ReplaceWith("SaeedGreenDark"))
val NeonBlue = SaeedGreenDark
@Deprecated("Use SaeedGreenDark", ReplaceWith("SaeedGreenDark"))
val RoyalPurple = SaeedGreenDark
@Deprecated("Use SaeedGreenSoft", ReplaceWith("SaeedGreenSoft"))
val DeepVoidViolet = SaeedGreenSoft
@Deprecated("Use SaeedPage", ReplaceWith("SaeedPage"))
val ShadowBlack = SaeedPage
@Deprecated("Use SaeedInk", ReplaceWith("SaeedInk"))
val SpectralWhite = SaeedInk
@Deprecated("Use SaeedColorScheme", ReplaceWith("SaeedColorScheme"))
val NeonVoidColorScheme = SaeedColorScheme
