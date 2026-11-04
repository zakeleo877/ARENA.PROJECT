package com.zakeleo877.saeedcore.data.remote

import java.net.URI
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** This value is public inside an APK. Security still depends on RLS. */
class SupabasePublicConfig(url: String, key: String) {
    val baseUrl: String
    internal val publicKey: String
    internal val legacyAnonJwt: Boolean

    /** The public key is safe to expose; it is still not a secret. */
    val apiKey: String get() = publicKey

    init {
        val parsed = try { URI(url.trim()) } catch (_: Exception) { null }
        require(
            parsed != null && parsed.scheme.equals("https", ignoreCase = true) &&
                !parsed.host.isNullOrBlank() && parsed.userInfo == null &&
                parsed.query == null && parsed.fragment == null &&
                (parsed.path.isNullOrEmpty() || parsed.path == "/"),
        ) { "A valid HTTPS Supabase project URL is required." }
        baseUrl = url.trim().trimEnd('/')
        val candidate = key.trim()
        val publishable = candidate.startsWith("sb_publishable_") && candidate.length > "sb_publishable_".length
        val anonJwt = if (!publishable) isLegacyAnonJwt(candidate) else false
        require(publishable || anonJwt) { "Only a publishable or legacy anon key is allowed." }
        publicKey = candidate
        legacyAnonJwt = anonJwt
    }

    private fun isLegacyAnonJwt(value: String): Boolean = try {
        val parts = value.split('.')
        if (parts.size != 3 || parts.any(String::isBlank)) false else {
            val payload = String(Base64.getUrlDecoder().decode(parts[1]), Charsets.UTF_8)
            Json.parseToJsonElement(payload).jsonObject["role"]?.jsonPrimitive?.contentOrNull == "anon"
        }
    } catch (_: Exception) { false }

    // Never print the public key accidentally in debug logs.
    override fun toString(): String = "SupabasePublicConfig(baseUrl=$baseUrl, publicKey=<redacted>)"
}
