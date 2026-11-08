package com.zakeleo877.saeedcore.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {
    /** Western digits and grouping, independent of the phone's Arabic locale. */
    fun money(amount: BigDecimal, currency: String): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
            roundingMode = RoundingMode.HALF_UP
        }
        return "${formatter.format(amount)}\u00A0$currency"
    }


    /**
     * Reads a price the way an Algerian phone actually types one.
     *
     * The Arabic keyboard produces Arabic-Indic digits (٢٥٠٠) and the Persian
     * layout produces the same shape with different code points; `toBigDecimal`
     * accepts neither, so a manager typing a price on an Arabic keyboard was
     * told the price was invalid while staring at four digits on the screen.
     *
     * The separators are the subtle part. The Arabic decimal separator is U+066B
     * and the thousands separator is U+066C, but a comma on a numeric keypad is
     * just as often a decimal point as a thousands mark. `12,50` means twelve
     * and a half; turning it into 1250 would overcharge the customer a hundred
     * fold. So a comma between exactly three digits, followed by nothing else,
     * is read as grouping; every other comma is a decimal point.
     *
     * Anything still unreadable returns null, and the caller shows its own
     * message rather than guessing a number.
     */
    fun parsePrice(input: String): BigDecimal? {
        var text = input.trim()
        if (text.isEmpty()) return null

        // Arabic-Indic and Persian digits, plus both decimal separators. A
        // `continue` inside a `when` used as a value is the kind of construct
        // whose legality depends on the compiler version, so the loop says what
        // it means without one.
        val normalized = StringBuilder(text.length)
        for (c in text) {
            if (c.isWhitespace()) continue
            normalized.append(
                when (c) {
                    in '\u0660'..'\u0669' -> '0' + (c - '\u0660')
                    in '\u06F0'..'\u06F9' -> '0' + (c - '\u06F0')
                    '\u066B' -> '.'
                    '\u066C' -> ','
                    else -> c
                },
            )
        }
        text = normalized.toString()
        if (text.isEmpty()) return null

        text = when {
            text.count { it == ',' } == 0 -> text
            NUMBER_WITH_THOUSANDS.matches(text) -> text.replace(",", "")
            text.count { it == ',' } == 1 && text.count { it == '.' } == 0 -> text.replace(',', '.')
            else -> return null
        }
        if (!NUMBER_WITH_THOUSANDS_STRIPPED.matches(text)) return null
        return text.toBigDecimalOrNull()?.takeIf { it.signum() >= 0 }
    }

    /** `1,234,567.89` or `1,234,567`. */
    private val NUMBER_WITH_THOUSANDS = Regex("""\d{1,3}(,\d{3})+(\.\d+)?""")

    /** `1234567.89` or `1234567`. */
    private val NUMBER_WITH_THOUSANDS_STRIPPED = Regex("""\d+(\.\d+)?""")

    fun orderNumber(number: Long): String {
        require(number >= 0)
        return "#${number.toString().padStart(3, '0')}"
    }

    /** Day first, as used in the Algerian locale: dd/MM/yyyy HH:mm. */
    fun dateTime(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.US).withZone(zone).format(instant)
}
