package com.zakeleo877.saeedadmin

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.zakeleo877.saeedadmin.notifications.NewOrderNotifier
import com.zakeleo877.saeedadmin.notifications.OrderNotificationScheduler
import com.zakeleo877.saeedcore.designsystem.SaeedImageLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SaeedAdminApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
    override fun onCreate() {
        super.onCreate()
        NewOrderNotifier.createChannel(this)
        if (OrderNotificationScheduler.isEnabled(this)) OrderNotificationScheduler.start(this)
        // The same bounded loader as the shop: a 56dp thumbnail must not decode
        // a 4000px photo.
        SaeedImageLoader.install(this)
    }
}
