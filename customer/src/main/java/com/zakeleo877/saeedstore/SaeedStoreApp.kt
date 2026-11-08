package com.zakeleo877.saeedstore

import android.app.Application
import com.zakeleo877.saeedcore.designsystem.SaeedImageLoader
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SaeedStoreApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Bounded caches and a real timeout, before the first card is drawn.
        SaeedImageLoader.install(this)
    }
}
