package com.zakeleo877.saeedcore

import com.zakeleo877.saeedcore.data.CatalogRepository
import com.zakeleo877.saeedcore.data.RefreshFailure
import com.zakeleo877.saeedcore.data.RefreshResult
import com.zakeleo877.saeedcore.data.local.CacheMetadataEntity
import com.zakeleo877.saeedcore.data.local.CatalogDao
import com.zakeleo877.saeedcore.data.local.ProductEntity
import com.zakeleo877.saeedcore.data.local.SettingEntity
import com.zakeleo877.saeedcore.data.remote.CatalogApi
import com.zakeleo877.saeedcore.data.remote.SupabasePublicConfig
import com.zakeleo877.saeedcore.data.remote.anonymousHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

private class MemoryDao : CatalogDao {
    val products = MutableStateFlow<List<ProductEntity>>(emptyList())
    val settings = MutableStateFlow<List<SettingEntity>>(emptyList())
    val metadata = MutableStateFlow<Map<String, CacheMetadataEntity>>(emptyMap())
    override fun observeProducts(): Flow<List<ProductEntity>> = products
    override fun observeSettings(): Flow<List<SettingEntity>> = settings
    override fun observeMetadata(key: String): Flow<CacheMetadataEntity?> = metadata.map { it[key] }
    override suspend fun hasProducts(): Boolean = products.value.isNotEmpty()
    override suspend fun insertProducts(products: List<ProductEntity>) { this.products.value = products }
    override suspend fun clearProducts() { products.value = emptyList() }
    override suspend fun insertSettings(settings: List<SettingEntity>) { this.settings.value = settings }
    override suspend fun clearSettings() { settings.value = emptyList() }
    override suspend fun updateMetadata(metadata: CacheMetadataEntity) { this.metadata.value += metadata.key to metadata }
}

class CatalogRepositoryTest {
    @Test fun failedRefreshLeavesLastSuccessfulCatalogIntact() = runTest {
        var fail = false
        val client = anonymousHttpClient(MockEngine {
            if (fail) respond("{}", HttpStatusCode.InternalServerError, headersOf(HttpHeaders.ContentType, "application/json"))
            else respond(
                """[{"id":"10000000-0000-4000-8000-000000000001","name":"منتج","price":12.30,"description":"","image_url":"","category":"عام","available":true,"sort_order":0,"created_at":"2026-01-01T00:00:00Z"}]""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"),
            )
        })
        val dao = MemoryDao()
        val repo = CatalogRepository(dao, CatalogApi(client, SupabasePublicConfig("https://project.example", "sb_publishable_fake"))) { 1234L }
        try {
            assertEquals(RefreshResult.Updated, repo.refreshProducts())
            assertEquals(1, repo.observeProducts().first().value.size)
            assertEquals(1234L, repo.observeProducts().first().updatedAt?.toEpochMilli())
            fail = true
            assertEquals(RefreshResult.Failed(RefreshFailure.SERVER), repo.refreshProducts())
            assertEquals(1, repo.observeProducts().first().value.size)
            assertEquals(1234L, repo.observeProducts().first().updatedAt?.toEpochMilli())
        } finally { client.close() }
    }
}
