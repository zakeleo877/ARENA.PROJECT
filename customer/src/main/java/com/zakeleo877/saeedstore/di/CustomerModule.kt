package com.zakeleo877.saeedstore.di

import android.content.Context
import com.zakeleo877.saeedcore.data.CartStore
import com.zakeleo877.saeedcore.data.OrderRepository
import com.zakeleo877.saeedcore.data.local.PendingOrderDao
import com.zakeleo877.saeedcore.data.remote.CatalogApi
import com.zakeleo877.saeedcore.data.remote.SupabasePublicConfig
import com.zakeleo877.saeedcore.network.NetworkMonitor
import com.zakeleo877.saeedstore.BuildConfig
import com.zakeleo877.saeedstore.ui.CustomerViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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

    @Provides
    @Singleton
    fun cartStore(): CartStore = CartStore()

    @Provides
    @Singleton
    fun orderRepository(api: CatalogApi, pendingDao: PendingOrderDao): OrderRepository =
        OrderRepository(api, pendingDao)

    @Provides
    @Singleton
    fun customerViewModel(
        dao: com.zakeleo877.saeedcore.data.local.CatalogDao,
        api: CatalogApi,
        orderRepository: OrderRepository,
        cart: CartStore,
        monitor: NetworkMonitor,
    ): CustomerViewModel = CustomerViewModel(dao, api, orderRepository, cart) { monitor.isOnline() }
}
