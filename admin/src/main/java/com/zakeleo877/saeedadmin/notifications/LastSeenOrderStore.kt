package com.zakeleo877.saeedadmin.notifications

import android.content.Context

/**
 * Remembers the highest order number already reported, so the background worker
 * only announces genuinely new orders. Stored in SharedPreferences.
 */
class LastSeenOrderStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("admin_notifications", Context.MODE_PRIVATE)

    fun lastSeenNumber(): Long = preferences.getLong(KEY_LAST_SEEN, 0L)

    fun setLastSeenNumber(number: Long) {
        preferences.edit().putLong(KEY_LAST_SEEN, number).apply()
    }

    private companion object {
        const val KEY_LAST_SEEN = "last_seen_order_number"
    }
}
