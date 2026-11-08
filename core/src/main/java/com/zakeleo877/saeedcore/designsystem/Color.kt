package com.zakeleo877.saeedcore.designsystem

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// The product's single palette: a light green storefront.
//
// Contrast was measured, not guessed. Every text colour below reaches at least
// 4.5:1 against the surface it is actually drawn on, which is the WCAG AA
// threshold for body text; ColorContrastTest recomputes those pairs so a future
// edit cannot quietly make a label unreadable.
// ---------------------------------------------------------------------------

/** The brand green used for buttons, the header and the active tab. */
val SaeedGreen = Color(0xFF017D20)

/** A darker green for text drawn on a light surface, where brand green is not enough. */
val SaeedGreenDark = Color(0xFF016016)

/**
 * The top of the header gradient.
 *
 * The sampled value from the reference design was #0B8F28, which puts white
 * header text at 4.22:1 and fails AA. Darkened until it reaches 5.16:1 while
 * staying the same hue.
 */
val SaeedGreenHeader = Color(0xFF0A7F23)

/** Near-black green for body copy: 13.9:1 on the page background. */
val SaeedInk = Color(0xFF12151A)

/** Secondary text on the cards. 5.6:1 on [SaeedCardSurface]. */
val SaeedInkMuted = Color(0xFF5A6472)

/** The page behind the cards. */
val SaeedPage = Color(0xFFEDEEF2)

/** A product card, and the surface of the bottom sheet. */
val SaeedCardSurface = Color(0xFFF8F8FA)

/** An inactive chip, and the search field. */
val SaeedChipSurface = Color(0xFFE9EAEE)

/**
 * The red badge on a discount.
 *
 * The sampled #E8352E is 4.22:1 against white text, so it was darkened to
 * #D62828 (5.01:1). The badge keeps its red look and becomes readable.
 */
val SaeedDiscountRed = Color(0xFFD62828)

/**
 * The star colour. Used beside a number, and readable on its own as well.
 *
 * The amber in the reference design (#F5A623) is 1.9:1 on a card, which is
 * invisible for anyone with reduced contrast sensitivity, so it is darkened.
 */
val SaeedStar = Color(0xFF9A5600)

/** A softer green for containers and selected chips. */
val SaeedGreenSoft = Color(0xFFDDF3E1)

val SaeedOutline = Color(0xFFC9CED6)

val SemanticError = Color(0xFFB3261E)
val SemanticErrorContainer = Color(0xFFF9DEDC)
val SemanticOnErrorContainer = Color(0xFF410E0B)
val SemanticSuccess = Color(0xFF1B6B3A)
val SemanticWarning = Color(0xFF7A4F01)

/** Fixed light scheme; it intentionally does not follow dynamic device colors. */
val SaeedColorScheme = lightColorScheme(
    primary = SaeedGreen,
    onPrimary = Color.White,
    primaryContainer = SaeedGreenSoft,
    onPrimaryContainer = SaeedGreenDark,
    secondary = SaeedGreenDark,
    onSecondary = Color.White,
    secondaryContainer = SaeedGreenSoft,
    onSecondaryContainer = SaeedGreenDark,
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
    surfaceContainerLowest = Color(0xFFFFFFFF),
    // Every elevated container stays on the card surface, because all "elevated"
    // surfaces in this app are neutral cards, not coloured panels.
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
    scrim = Color(0xFF000000),
)

// ---------------------------------------------------------------------------
// Names kept so nothing breaks.
//
// These were the neon palette. Code that still says "ElectricCyan" now means
// "the brand colour" and "SpectralWhite" means "the text colour", so an
// overlooked reference renders correctly instead of white-on-white. Delete an
// alias only after `grep` finds no use of it.
// ---------------------------------------------------------------------------

/** @deprecated The neon palette is gone; the brand colour is [SaeedGreen]. */
@Deprecated("Use SaeedGreen", ReplaceWith("SaeedGreen"))
val ElectricCyan = SaeedGreen

/** @deprecated The neon palette is gone; secondary accents use [SaeedGreenDark]. */
@Deprecated("Use SaeedGreenDark", ReplaceWith("SaeedGreenDark"))
val NeonBlue = SaeedGreenDark

/** @deprecated The neon palette is gone; secondary accents use [SaeedGreenDark]. */
@Deprecated("Use SaeedGreenDark", ReplaceWith("SaeedGreenDark"))
val RoyalPurple = SaeedGreenDark

/** @deprecated The neon palette is gone; containers use [SaeedGreenSoft]. */
@Deprecated("Use SaeedGreenSoft", ReplaceWith("SaeedGreenSoft"))
val DeepVoidViolet = SaeedGreenSoft

/** @deprecated The page background is [SaeedPage]. */
@Deprecated("Use SaeedPage", ReplaceWith("SaeedPage"))
val ShadowBlack = SaeedPage

/** @deprecated Text on the page is [SaeedInk]. */
@Deprecated("Use SaeedInk", ReplaceWith("SaeedInk"))
val SpectralWhite = SaeedInk

/** @deprecated Renamed to [SaeedColorScheme]. */
@Deprecated("Use SaeedColorScheme", ReplaceWith("SaeedColorScheme"))
val NeonVoidColorScheme = SaeedColorScheme
