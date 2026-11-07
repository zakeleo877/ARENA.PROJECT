package com.zakeleo877.saeedcore.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object NeonVoidTheme {
    val NeonVoidBackground = Color(0xFF000000)
    val NeonVoidPrimary = Color(0xFF00FFFF) // Cyan
    val NeonVoidSecondary = Color(0xFFFF00FF) // Magenta
    val NeonVoidSurface = Color(0xFF121212)

    val NeonVoidTypography = Typography(
        bodyLarge = androidx.compose.ui.text.font.Font(
            family = FontFamily.SansSerif,
            weight = FontWeight.Normal,
            size = 16.sp
        )
    )

    @Composable
    fun NeonVoidAppTheme(
        content: @Composable () -> Unit
    ) {
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme.copy(
                background = NeonVoidBackground,
                surface = NeonVoidSurface,
                primary = NeonVoidPrimary,
                secondary = NeonVoidSecondary
            ),
            typography = NeonVoidTypography,
            content = content
        )
    }
}