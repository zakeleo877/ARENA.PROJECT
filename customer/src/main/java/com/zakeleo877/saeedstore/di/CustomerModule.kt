package com.zakeleo877.saeedstore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.zakeleo877.saeedcore.data.CartPersistence
import com.zakeleo877.saeedcore.data.CartStore
import com.zakeleo877.saeedcore.data.OrderRepository
import com.zakeleo877.saeedcore.data.local.PendingOrderDao
import com.zakeleo877.saeedcore.data.remote.CatalogApi
import com.zakeleo877.saeedcore.data.remote.SupabasePublicConfig
import com.zakeleo877.saeedcore.network.NetworkMonitor
import com.zakeleo877.saeedstore.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Module
@InstallIn(SingletonComponent::class)
object CustomerModule {
    @Provides
    @Singleton
    fun supabaseConfig(): SupabasePublicConfig = SupabasePublicConfig(
        url = BuildConfig.SUPABASE_URL,
        key = BuildConfig.SUPABASE_KEY,
    )

    @Provides
    @Singleton
    fun networkMonitor(@ApplicationContext context: Context): NetworkMonitor = NetworkMonitor(context)

    /**
     * The basket's file.
     *
     * `preferencesDataStore` is deliberately not used: it is a property delegate
     * that must be declared once at the top level, and a module that can be
     * built twice would create two DataStores over the same file.
     */
    @Provides
    @Singleton
    fun cartDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { context.preferencesDataStoreFile("cart") },
        )

    /**
     * The basket, restored before anyone can see it.
     *
     * The restore and the mirroring run off the main thread: the first frame
     * must not wait for a disk read. The restore is applied before `bind`, so
     * the first write mirrors the stored basket rather than overwriting it with
     * the empty list the process started from.
     */
    @Provides
    @Singleton
    fun cartStore(dataStore: DataStore<Preferences>): CartStore {
        val store = CartStore()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val persistence = CartPersistence(dataStore, scope)
        scope.launch {
            store.restoreAll(persistence.load())
            persistence.bind(store)
        }
        return store
    }

    @Provides
    @Singleton
    fun orderRepository(api: CatalogApi, pendingDao: PendingOrderDao): OrderRepository =
        OrderRepository(api, pendingDao)
}
