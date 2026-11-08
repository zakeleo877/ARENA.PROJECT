package com.zakeleo877.saeedcore.di

import android.content.Context
import com.zakeleo877.saeedcore.data.CatalogRepository
import com.zakeleo877.saeedcore.data.local.CatalogDao
import com.zakeleo877.saeedcore.data.local.PendingOrderDao
import com.zakeleo877.saeedcore.data.local.SaeedDatabase
import com.zakeleo877.saeedcore.data.remote.CatalogApi
import com.zakeleo877.saeedcore.data.remote.SupabasePublicConfig
import com.zakeleo877.saeedcore.data.remote.anonymousHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PublicCatalogClient

/** Each app will provide its own SupabasePublicConfig from its public BuildConfig. */
@Module
@InstallIn(SingletonComponent::class)
object CoreModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): SaeedDatabase = SaeedDatabase.create(context)

    @Provides fun catalogDao(database: SaeedDatabase): CatalogDao = database.catalogDao()
    @Provides fun pendingOrderDao(database: SaeedDatabase): PendingOrderDao = database.pendingOrderDao()

    @Provides @Singleton @PublicCatalogClient fun httpClient(): HttpClient = anonymousHttpClient()
    @Provides @Singleton fun catalogApi(@PublicCatalogClient client: HttpClient, config: SupabasePublicConfig): CatalogApi = CatalogApi(client, config)
    @Provides @Singleton fun catalogRepository(dao: CatalogDao, api: CatalogApi): CatalogRepository = CatalogRepository(dao, api)
}
