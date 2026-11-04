package com.zakeleo877.saeedcore.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import java.util.Base64
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

private val authJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class AuthRequest(val email: String, val password: String)

@Serializable
private data class AuthTokenWire(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long,
)

@Serializable
private data class UserWire(
    val id: String,
    val email: String? = null,
)

@Serializable
private data class AuthSessionWire(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    val user: UserWire,
)

/** Session state of the signed-in admin. */
data class AdminSession(
    val userId: String,
    val email: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMillis: Long,
)

sealed interface AuthResult {
    data class Success(val session: AdminSession) : AuthResult
    data class Failure(val reason: AuthError) : AuthResult
}

enum class AuthError {
    INVALID_CREDENTIALS,
    EMAIL_NOT_CONFIRMED,
    SIGNUP_DISABLED,
    RATE_LIMITED,
    NETWORK,
    SERVER,
}

/**
 * Minimal Supabase Auth client for the admin app.
 * It deliberately does not depend on the Supabase SDK so the customer package
 * never ships auth, storage or realtime code.
 */
class AuthManager(
    private val client: HttpClient,
    private val config: SupabasePublicConfig,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    suspend fun signIn(email: String, password: String): AuthResult {
        if (email.isBlank() || password.isBlank()) {
            return AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
        }
        return try {
            val response = client.post("${config.baseUrl}/auth/v1/token?grant_type=password") {
                publicHeaders()
                contentType(ContentType.Application.Json)
                setBody(AuthRequest(email.trim(), password))
            }
            if (response.status.value == 400 || response.status.value == 401) {
                AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
            } else if (response.status.value == 429) {
                AuthResult.Failure(AuthError.RATE_LIMITED)
            } else if (!response.status.isSuccess()) {
                AuthResult.Failure(AuthError.SERVER)
            } else {
                AuthResult.Success(session(response.body<AuthTokenWire>(), email.trim()))
            }
        } catch (_: java.io.IOException) {
            AuthResult.Failure(AuthError.NETWORK)
        } catch (_: Exception) {
            AuthResult.Failure(AuthError.SERVER)
        }
    }

    /** Refreshes the access token using the refresh token. */
    suspend fun refresh(session: AdminSession): AuthResult = try {
        val response = client.post("${config.baseUrl}/auth/v1/token?grant_type=refresh_token") {
            publicHeaders()
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(session.refreshToken))
        }
        if (!response.status.isSuccess()) {
            AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
        } else {
            val token = response.body<AuthTokenWire>()
            AuthResult.Success(
                session.copy(
                    accessToken = token.accessToken,
                    refreshToken = token.refreshToken,
                    expiresAtMillis = nowMillis() + token.expiresIn * 1_000L,
                ),
            )
        }
    } catch (_: java.io.IOException) {
        AuthResult.Failure(AuthError.NETWORK)
    } catch (_: Exception) {
        AuthResult.Failure(AuthError.SERVER)
    }

    private fun session(token: AuthTokenWire, email: String): AdminSession {
        val userId = subjectOf(token.accessToken)
            ?: throw IllegalStateException("The access token does not contain a subject.")
        return AdminSession(
            userId = userId,
            email = email,
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
            expiresAtMillis = nowMillis() + token.expiresIn * 1_000L,
        )
    }

    private fun io.ktor.client.request.HttpRequestBuilder.publicHeaders() {
        header("apikey", config.publicKey)
        header(HttpHeaders.Accept, ContentType.Application.Json)
    }

    /** Reads the `sub` claim without verifying the signature (the server does that). */
    private fun subjectOf(accessToken: String): String? = try {
        val payload = accessToken.split('.').getOrNull(1) ?: return null
        val decoded = String(Base64.getUrlDecoder().decode(payload), Charsets.UTF_8)
        val element: JsonElement = Json.parseToJsonElement(decoded)
        (element as? JsonObject)?.get("sub")?.jsonPrimitive?.contentOrNullSafe()
    } catch (_: Exception) {
        null
    }

    private fun JsonPrimitive.contentOrNullSafe(): String? =
        if (this is JsonPrimitive && this.isString) this.content else null
}

@Serializable
private data class RefreshRequest(@SerialName("refresh_token") val refreshToken: String)
