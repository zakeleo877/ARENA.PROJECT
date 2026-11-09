package com.zakeleo877.saeedcore.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.zakeleo877.saeedcore.R

/** Arabic type used by the approved preview; static weights also work on older Android. */
val Alexandria = FontFamily(
    Font(R.font.alexandria_regular, FontWeight.Normal),
    Font(R.font.alexandria_medium, FontWeight.Medium),
    Font(R.font.alexandria_bold, FontWeight.Bold),
)

private fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = Alexandria,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
)

val SaeedTypography = Typography(
    displayLarge = style(57, 64),
    displayMedium = style(45, 52),
    displaySmall = style(36, 44),
    headlineLarge = style(32, 40, FontWeight.Bold),
    headlineMedium = style(28, 36, FontWeight.Bold),
    headlineSmall = style(24, 32, FontWeight.Bold),
    titleLarge = style(22, 28, FontWeight.Bold),
    titleMedium = style(16, 24, FontWeight.Medium),
    titleSmall = style(14, 20, FontWeight.Medium),
    bodyLarge = style(16, 24),
    bodyMedium = style(14, 20),
    bodySmall = style(12, 18),
    labelLarge = style(14, 20, FontWeight.Medium),
    labelMedium = style(12, 18, FontWeight.Medium),
    labelSmall = style(11, 16, FontWeight.Medium),
)
