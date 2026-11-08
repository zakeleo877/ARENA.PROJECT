package com.zakeleo877.saeedadmin

import android.app.Application
import com.zakeleo877.saeedadmin.notifications.NewOrderNotifier
import com.zakeleo877.saeedcore.designsystem.SaeedImageLoader
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SaeedAdminApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NewOrderNotifier.createChannel(this)
        // The same bounded loader as the shop: a 56dp thumbnail must not decode
        // a 4000px photo.
        SaeedImageLoader.install(this)
    }
}
