package com.zakeleo877.saeedstore.orders

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.zakeleo877.saeedcore.data.OrderRepository
import com.zakeleo877.saeedcore.data.SubmitOutcome
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

/** Retries durable orders when a connection becomes available, even after restart. */
@HiltWorker
class PendingOrdersWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val repository: OrderRepository,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        val outcomes = repository.drainWaiting()
        if (outcomes.any { it is SubmitOutcome.Queued }) Result.retry() else Result.success()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        Result.retry()
    }
}

object PendingOrdersScheduler {
    private const val WORK_NAME = "send_pending_orders"

    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<PendingOrdersWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        // Append rather than KEEP: a new order queued as a worker is finishing
        // must not be left behind when that worker has already read its queue.
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )
    }
}
