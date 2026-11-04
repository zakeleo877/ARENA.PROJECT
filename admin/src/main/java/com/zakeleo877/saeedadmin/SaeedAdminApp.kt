package com.zakeleo877.saeedadmin

import android.app.Application
import com.zakeleo877.saeedadmin.notifications.NewOrderNotifier
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SaeedAdminApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NewOrderNotifier.createChannel(this)
    }
}
