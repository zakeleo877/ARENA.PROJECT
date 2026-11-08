package com.zakeleo877.saeedadmin.di

import android.content.Context
import com.zakeleo877.saeedcore.data.remote.AdminApi
import com.zakeleo877.saeedcore.data.remote.AuthManager
import com.zakeleo877.saeedcore.data.remote.SessionVault
import com.zakeleo877.saeedcore.data.remote.SupabasePublicConfig
import com.zakeleo877.saeedadmin.BuildConfig
import com.zakeleo877.saeedadmin.data.AdminRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object AdminModule {
    @Provides
    @Singleton
    fun supabaseConfig(): SupabasePublicConfig = SupabasePublicConfig(
        url = BuildConfig.SUPABASE_URL,
        key = BuildConfig.SUPABASE_KEY,
    )

    @Provides
    @Singleton
    fun httpClient(): HttpClient = HttpClient(OkHttp.create()) {
        expectSuccess = false
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 20_000
        }
    }

    @Provides
    @Singleton
    fun authManager(client: HttpClient, config: SupabasePublicConfig): AuthManager =
        AuthManager(client, config)

    @Provides
    @Singleton
    fun adminApi(client: HttpClient, config: SupabasePublicConfig): AdminApi = AdminApi(client, config)

    @Provides
    @Singleton
    fun sessionVault(@ApplicationContext context: Context): SessionVault = SessionVault(context)

    @Provides
    @Singleton
    fun adminRepository(
        client: HttpClient,
        config: SupabasePublicConfig,
        api: AdminApi,
        authManager: AuthManager,
        vault: SessionVault,
    ): AdminRepository = AdminRepository(client, config, api, authManager, vault)
}
