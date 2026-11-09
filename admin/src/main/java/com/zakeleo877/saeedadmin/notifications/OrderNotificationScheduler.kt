package com.zakeleo877.saeedadmin.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Local preference and scheduling for the admin's background order checks. */
object OrderNotificationScheduler {
    private const val WORK_NAME = "orders_refresh"
    private const val NOW_NAME = "orders_refresh_now"
    private const val PREFERENCES = "admin_notifications"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_PERMISSION_PROMPTED = "permission_prompted"

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = preferences(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) start(context) else stop(context)
    }

    fun permissionWasPrompted(context: Context): Boolean =
        preferences(context).getBoolean(KEY_PERMISSION_PROMPTED, false)

    fun markPermissionPrompted(context: Context) {
        preferences(context).edit().putBoolean(KEY_PERMISSION_PROMPTED, true).apply()
    }

    private fun connected(): Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Android's minimum periodic interval is 15 minutes; actual notification may be later. */
    fun start(context: Context) {
        val work = PeriodicWorkRequestBuilder<OrdersRefreshWorker>(15, TimeUnit.MINUTES)
            .setConstraints(connected())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            work,
        )
        checkNow(context)
    }

    fun checkNow(context: Context) {
        if (!isEnabled(context)) return
        val work = OneTimeWorkRequestBuilder<OrdersRefreshWorker>()
            .setConstraints(connected())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(NOW_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, work)
    }

    fun stop(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(NOW_NAME)
    }
}
