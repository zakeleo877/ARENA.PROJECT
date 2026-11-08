package com.zakeleo877.saeedcore.designsystem

import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import okio.Path.Companion.toOkioPath

/**
 * One image loader for both apps.
 *
 * The default loader is fine for a demo and wrong for this shop:
 *
 * * The disk cache is 2% of the free space, capped at 250 MB, and it lives in a
 *   directory named after the library version - which the system also counts as
 *   app data. A shop photo set should have a fixed, bounded footprint instead.
 * * There is no memory cache bound either, so scrolling a long catalogue on a
 *   1 GB phone collects bitmaps until the process dies.
 * * The stock client has no connect timeout, so a photo on a dying connection
 *   can hold a request open for the whole default read window.
 *
 * Settings here: 80 MB of disk (about a thousand card photos), an eighth of the
 * app's allowed heap in memory, 8 s to connect and 20 s to read.
 */
object SaeedImageLoader {

    /** Photos are the only thing cached here, so the budget is explicit. */
    private const val DISK_LIMIT_BYTES = 80L * 1024 * 1024

    /**
     * Installs the loader once, at process start. [SingletonImageLoader.setSafe]
     * deliberately does nothing if a loader already exists, so a preview or a
     * test that installed its own is never overwritten.
     */
    fun install(context: Context) {
        val app = context.applicationContext
        SingletonImageLoader.setSafe {
            ImageLoader.Builder(app)
                .crossfade(true)
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizePercent(app, 0.125)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(app.cacheDir.resolve("photos").toOkioPath())
                        .maxSizeBytes(DISK_LIMIT_BYTES)
                        .build()
                }
                .components {
                    // A photo that cannot be fetched in 20 s is not going to
                    // arrive; failing frees the slot for the next card.
                    add(
                        OkHttpNetworkFetcherFactory(
                            callFactory = {
                                okhttp3.OkHttpClient.Builder()
                                    .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
                                    .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                                    .build()
                            },
                        ),
                    )
                }
                .build()
        }
    }
}
