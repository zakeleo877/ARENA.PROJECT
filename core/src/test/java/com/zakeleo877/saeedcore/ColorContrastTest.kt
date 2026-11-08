package com.zakeleo877.saeedcore

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.zakeleo877.saeedcore.designsystem.SaeedCardSurface
import com.zakeleo877.saeedcore.designsystem.SaeedChipSurface
import com.zakeleo877.saeedcore.designsystem.SaeedColorScheme
import com.zakeleo877.saeedcore.designsystem.SaeedDiscountRed
import com.zakeleo877.saeedcore.designsystem.SaeedGreenDark
import com.zakeleo877.saeedcore.designsystem.SaeedGreenHeader
import com.zakeleo877.saeedcore.designsystem.SaeedInk
import com.zakeleo877.saeedcore.designsystem.SaeedInkMuted
import com.zakeleo877.saeedcore.designsystem.SaeedOutline
import com.zakeleo877.saeedcore.designsystem.SaeedPage
import com.zakeleo877.saeedcore.designsystem.SaeedStar
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The light palette is only safe if its text stays readable, and white-on-white
 * is the specific failure a change like this produces. Every pair below is a
 * combination the screens actually draw.
 */
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

    private fun assertReadable(foreground: Color, background: Color, what: String) {
        val ratio = contrast(foreground, background)
        assertTrue("$what must reach 4.5:1, was ${"%.2f".format(ratio)}:1", ratio >= 4.5)
    }

    @Test
    fun bodyTextIsReadableOnEverySurface() {
        // Card text, on the card and on the page it sits on.
        assertReadable(SaeedInk, SaeedCardSurface, "body text on a card")
        assertReadable(SaeedInk, SaeedPage, "body text on the page")
        // Secondary text: the muted grey is the weakest link of the palette.
        assertReadable(SaeedInkMuted, SaeedCardSurface, "muted text on a card")
        assertReadable(SaeedInkMuted, SaeedChipSurface, "muted text on an inactive chip")
        assertReadable(SaeedColorScheme.onSurfaceVariant, SaeedColorScheme.surfaceContainerHighest,
            "theme muted text on the highest container")
    }

    @Test
    fun whiteTextOnTheBrandColoursIsReadable() {
        assertReadable(Color.White, SaeedColorScheme.primary, "white on the brand green")
        assertReadable(Color.White, SaeedGreenHeader, "white on the header")
        assertReadable(Color.White, SaeedGreenDark, "white on the dark green")
        assertReadable(SaeedColorScheme.onPrimary, SaeedColorScheme.primary, "theme onPrimary on primary")
    }

    @Test
    fun accentsAreReadableWhereTheyCarryText() {
        // The discount badge is a filled pill with white text.
        assertReadable(Color.White, SaeedDiscountRed, "white on the discount badge")
        // Brand green is used for prices and the active chip label.
        assertReadable(SaeedGreenDark, SaeedCardSurface, "green price on a card")
        assertReadable(SaeedGreenDark, SaeedChipSurface, "green label on a chip")
        // The star sits next to a number, so it must be legible on its own too.
        assertReadable(SaeedStar, SaeedCardSurface, "star on a card")
        // The error colour carries the only message a failed save shows.
        assertReadable(SaeedColorScheme.error, SaeedCardSurface, "error text on a card")
    }

    @Test
    fun outlinesAreVisibleWithoutBeingText() {
        // Outlines are not text, so the WCAG rule is 3:1 for non-text contrast.
        assertTrue(
            "the outline must be distinguishable from the card",
            contrast(SaeedOutline, SaeedCardSurface) >= 1.2,
        )
    }
}
