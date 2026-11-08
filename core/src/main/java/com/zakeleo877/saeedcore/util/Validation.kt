package com.zakeleo877.saeedcore.util

object Validation {
    // Matches server-side acceptance; validation is repeated authoritatively in place_order.
    private val phonePattern = Regex("^[0-9+ ]{8,15}$")

    fun customerName(value: String) = value.trim().length in 2..100
    fun phone(value: String) = phonePattern.matches(value) && value.count { it in '0'..'9' } in 8..15
    fun notes(value: String) = value.length <= 500
    /** Contact-only map link for shop-to-customer contact. */
    fun locationUrl(value: String) = value.length <= 200
    fun quantity(value: Int) = value in 1..99
}
