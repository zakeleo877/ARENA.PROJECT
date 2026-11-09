package com.zakeleo877.saeedadmin.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zakeleo877.saeedadmin.data.AdminRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Polls for new orders using the admin's own restored session. WorkManager's
 * 15-minute interval is a minimum, not a guarantee of immediate notification.
 */
@HiltWorker
class OrdersRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val repository: AdminRepository,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = notificationPollMutex.withLock { poll() }

    private suspend fun poll(): Result {
        if (!OrderNotificationScheduler.isEnabled(applicationContext)) return Result.success()
        // Do not consume unseen numbers while the OS or channel blocks posts.
        if (!NewOrderNotifier.canNotify(applicationContext)) return Result.success()
        val session = repository.sessionHolder.session.value ?: run {
            if (!repository.restoreSession()) return Result.success()
            repository.sessionHolder.session.value ?: return Result.success()
        }

        val store = LastSeenOrderStore(applicationContext)
        // Fetch older unseen numbers first. If there are over 100, another page
        // is processed without skipping the older orders.
        repeat(5) {
            val lastSeen = store.lastSeenNumber()
            val numbers = try {
                repository.newOrderNumbersSince(session, lastSeen)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return Result.retry()
            }
            if (numbers.isEmpty()) return Result.success()
            val postedAll = postAndRecordNumbers(
                numbers = numbers,
                lastSeen = lastSeen,
                post = { number ->
                    OrderNotificationScheduler.isEnabled(applicationContext) &&
                        repository.sessionHolder.session.value?.userId == session.userId &&
                        NewOrderNotifier.notifyNewOrder(applicationContext, number)
                },
                record = store::setLastSeenNumber,
            )
            if (!postedAll || numbers.size < 100) return Result.success()
        }
        return Result.retry()
    }
}

// The immediate and periodic workers may run together in the same app process.
private val notificationPollMutex = Mutex()

/** Advance the cursor only after each notification has actually been posted. */
internal fun postAndRecordNumbers(
    numbers: List<Long>,
    lastSeen: Long,
    post: (Long) -> Boolean,
    record: (Long) -> Unit,
): Boolean {
    var cursor = lastSeen
    for (number in numbers.sorted()) {
        if (number <= cursor) continue
        if (!post(number)) return false
        record(number)
        cursor = number
    }
    return true
}
