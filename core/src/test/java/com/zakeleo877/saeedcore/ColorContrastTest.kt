package com.zakeleo877.saeedcore

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.zakeleo877.saeedcore.designsystem.ElectricCyan
import com.zakeleo877.saeedcore.designsystem.NeonBlue
import com.zakeleo877.saeedcore.designsystem.NeonVoidColorScheme
import com.zakeleo877.saeedcore.designsystem.RoyalPurple
import com.zakeleo877.saeedcore.designsystem.ShadowBlack
import com.zakeleo877.saeedcore.designsystem.SpectralWhite
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorContrastTest {
    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double {
            val x = value.toDouble()
            return if (x <= 0.04045) x / 12.92 else ((x + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    private fun contrast(foreground: Color, background: Color): Double {
        val fg = foreground.compositeOver(background)
        val l1 = luminance(fg)
        val l2 = luminance(background)
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    @Test fun primaryTextAndOverlayPassWcagAA() {
        assertTrue(contrast(ShadowBlack, ElectricCyan) >= 4.5)
        assertTrue(contrast(ShadowBlack, NeonBlue) >= 4.5)
        assertTrue(contrast(SpectralWhite, RoyalPurple) >= 4.5)
        assertTrue(contrast(SpectralWhite, RoyalPurple.copy(alpha = 0.90f).compositeOver(ElectricCyan)) >= 4.5)
        assertTrue(contrast(NeonVoidColorScheme.onSurfaceVariant, NeonVoidColorScheme.surfaceContainerHighest) >= 4.5)
    }
}
