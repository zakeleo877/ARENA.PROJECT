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

    fun orderNumber(number: Long): String {
        require(number >= 0)
        return "#${number.toString().padStart(3, '0')}"
    }

    /** Day first, as used in the Algerian locale: dd/MM/yyyy HH:mm. */
    fun dateTime(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.US).withZone(zone).format(instant)
}
