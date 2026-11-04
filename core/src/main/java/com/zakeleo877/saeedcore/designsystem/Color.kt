package com.zakeleo877.saeedcore.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// The product's single palette. Semantic colors are reserved for state only.
val ElectricCyan = Color(0xFF00F0FF)
val NeonBlue = Color(0xFF0077FF)
val RoyalPurple = Color(0xFF7000FF)
val DeepVoidViolet = Color(0xFF240046)
val ShadowBlack = Color(0xFF0A0A0F)
val SpectralWhite = Color(0xFFE0FFFF)
val SemanticError = Color(0xFFFFB4AB)
val SemanticErrorContainer = Color(0xFF690005)
val SemanticOnErrorContainer = Color(0xFFFFDAD6)
val SemanticSuccess = Color(0xFF70DFAD)
val SemanticWarning = Color(0xFFFFC65C)

private fun layer(amount: Float) = lerp(ShadowBlack, DeepVoidViolet, amount)

val NeonVoidColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ShadowBlack,
    primaryContainer = lerp(ShadowBlack, NeonBlue, 0.35f),
    onPrimaryContainer = SpectralWhite,
    secondary = RoyalPurple,
    onSecondary = SpectralWhite,
    secondaryContainer = DeepVoidViolet,
    onSecondaryContainer = SpectralWhite,
    tertiary = NeonBlue,
    onTertiary = ShadowBlack,
    tertiaryContainer = lerp(ShadowBlack, NeonBlue, 0.28f),
    onTertiaryContainer = SpectralWhite,
    background = ShadowBlack,
    onBackground = SpectralWhite,
    surface = ShadowBlack,
    onSurface = SpectralWhite,
    surfaceVariant = layer(0.24f),
    onSurfaceVariant = SpectralWhite.copy(alpha = 0.70f),
    surfaceContainerLowest = layer(0.06f),
    surfaceContainerLow = layer(0.10f),
    surfaceContainer = layer(0.14f),
    surfaceContainerHigh = layer(0.18f),
    surfaceContainerHighest = layer(0.24f),
    outline = NeonBlue,
    outlineVariant = NeonBlue.copy(alpha = 0.30f),
    error = SemanticError,
    onError = SemanticErrorContainer,
    errorContainer = SemanticErrorContainer,
    onErrorContainer = SemanticOnErrorContainer,
    scrim = ShadowBlack,
)
