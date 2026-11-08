package com.zakeleo877.saeedcore.data

import com.zakeleo877.saeedcore.data.local.CatalogDao
import com.zakeleo877.saeedcore.data.local.ProductEntity
import com.zakeleo877.saeedcore.data.local.SettingEntity
import com.zakeleo877.saeedcore.data.remote.ApiFailure
import com.zakeleo877.saeedcore.data.remote.ApiFailureKind
import com.zakeleo877.saeedcore.data.remote.CatalogApi
import com.zakeleo877.saeedcore.model.Product
import com.zakeleo877.saeedcore.model.StoreSettings
import java.io.IOException
import java.time.DateTimeException
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.SerializationException

/** A null timestamp means the app has never obtained a complete server snapshot. */
data class Cached<T>(val value: T, val updatedAt: Instant?)

enum class RefreshFailure { NETWORK, UNAUTHORIZED, SERVER, INVALID_RESPONSE }
sealed interface RefreshResult {
    data object Updated : RefreshResult
    data class Failed(val reason: RefreshFailure) : RefreshResult
}

class CatalogRepository(
    private val dao: CatalogDao,
    private val api: CatalogApi,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    fun observeProducts(): Flow<Cached<List<Product>>> = combine(
        dao.observeProducts(), dao.observeMetadata("products"),
    ) { products, metadata ->
        Cached(products.map(ProductEntity::model), metadata?.updatedAtMillis?.let(Instant::ofEpochMilli))
    }

    fun observeSettings(): Flow<Cached<StoreSettings?>> = combine(
        dao.observeSettings(), dao.observeMetadata("settings"),
    ) { rows, metadata ->
        val settings = rows.associate { it.key to it.value }
        val value = if (listOf("storeName", "whatsapp", "currency", "footerNote").all(settings::containsKey)) {
            StoreSettings(settings.getValue("storeName"), settings.getValue("whatsapp"), settings.getValue("currency"), settings.getValue("footerNote"))
        } else null
        Cached(value, metadata?.updatedAtMillis?.let(Instant::ofEpochMilli))
    }

    /** A failure leaves the previous complete cache untouched for offline browsing. */
    suspend fun refreshProducts(): RefreshResult = refresh {
        val snapshot = api.products().map(ProductEntity::from)
        dao.replaceProducts(snapshot, nowMillis())
    }

    suspend fun refreshSettings(): RefreshResult = refresh {
        val value = api.settings()
        val snapshot = listOf(
            SettingEntity("storeName", value.storeName), SettingEntity("whatsapp", value.whatsapp),
            SettingEntity("currency", value.currency), SettingEntity("footerNote", value.footerNote),
        )
        dao.replaceSettings(snapshot, nowMillis())
    }

    private suspend fun refresh(action: suspend () -> Unit): RefreshResult = try {
        action()
        RefreshResult.Updated
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: ApiFailure) {
        RefreshResult.Failed(when (failure.kind) {
            ApiFailureKind.UNAUTHORIZED -> RefreshFailure.UNAUTHORIZED
            else -> RefreshFailure.SERVER
        })
    } catch (_: IOException) {
        RefreshResult.Failed(RefreshFailure.NETWORK)
    } catch (_: SerializationException) {
        RefreshResult.Failed(RefreshFailure.INVALID_RESPONSE)
    } catch (_: DateTimeException) {
        RefreshResult.Failed(RefreshFailure.INVALID_RESPONSE)
    } catch (_: IllegalArgumentException) {
        RefreshResult.Failed(RefreshFailure.INVALID_RESPONSE)
    }
}
