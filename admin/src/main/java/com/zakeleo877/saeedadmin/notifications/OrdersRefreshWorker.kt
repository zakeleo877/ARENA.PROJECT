package com.zakeleo877.saeedadmin.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zakeleo877.saeedadmin.data.AdminRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Polls for new orders while the admin app is closed.
 * The interval is 15 minutes, the minimum Android allows for periodic work.
 */
@HiltWorker
class OrdersRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val repository: AdminRepository,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val session = repository.sessionHolder.session.value ?: return Result.success()
        val lastSeen = LastSeenOrderStore(applicationContext).lastSeenNumber()
        val newOrders = runCatching { repository.newOrdersSince(session, lastSeen) }.getOrNull()
            ?: return Result.retry()
        if (newOrders.isEmpty()) return Result.success()

        val highest = newOrders.maxOf { it.number }
        newOrders.forEach { NewOrderNotifier.notifyNewOrder(applicationContext, it.number) }
        LastSeenOrderStore(applicationContext).setLastSeenNumber(highest)
        return Result.success()
    }
}
