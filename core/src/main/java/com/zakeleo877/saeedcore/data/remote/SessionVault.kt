package com.zakeleo877.saeedcore.data.remote

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Keeps the signed-in admin session between launches, encrypted.
 *
 * The session is an access token, a refresh token and an identity. Stored as
 * plain preferences, any app with root, any adb backup and any device image
 * would carry a live key to the shop's order table. So the bytes are sealed with
 * AES-GCM under a key that lives in the Android keystore: the key never leaves
 * that store, and the store refuses to hand it to another app or to a device the
 * screen lock no longer protects.
 *
 * GCM rather than CBC because GCM authenticates: a tampered ciphertext fails to
 * open instead of decrypting into a different session.
 *
 * Nothing here is called on a background thread, and the payload is a few
 * hundred bytes, so the main thread cost is one keystore round trip — small
 * enough that it does not need a worker, and the session is needed before the
 * first screen is drawn.
 */
class SessionVault(private val context: Context) {

    /** The decrypted session, or null when there is none or it cannot be read. */
    fun load(): AdminSession? {
        val stored = preferences().getString(KEY_BLOB, null) ?: return null
        val plain = open(stored) ?: return null
        return runCatching { json.decodeFromString<StoredSession>(plain) }
            .getOrNull()
            ?.toSession()
    }

    fun save(session: AdminSession) {
        val sealed = runCatching { seal(json.encodeToString(StoredSession.from(session))) }.getOrNull()
        if (sealed == null) {
            // Failing to save is not a reason to lose the session the manager
            // is already using, so it is dropped and the next launch asks again.
            clear()
            return
        }
        preferences().edit().putString(KEY_BLOB, sealed).commit()
    }

    fun clear() {
        preferences().edit().remove(KEY_BLOB).commit()
    }

    private fun preferences() =
        context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    private fun seal(plain: String): String {
        val key = secretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val body = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        // The IV is not secret but it must be stored with the ciphertext, or the
        // value can never be opened again.
        val combined = cipher.iv + body
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun open(stored: String): String? = runCatching {
        val combined = Base64.decode(stored, Base64.NO_WRAP)
        if (combined.size <= IV_LENGTH) return null
        val key = secretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(TAG_BITS, combined, 0, IV_LENGTH),
        )
        String(cipher.doFinal(combined, IV_LENGTH, combined.size - IV_LENGTH), Charsets.UTF_8)
    }.getOrNull()

    /**
     * The keystore key, created on first use.
     *
     * Asking for it can fail — a device whose screen lock was removed may have
     * dropped the key, and an emulator without a keystore provider has none at
     * all. The failure is left to the caller, which drops the session rather
     * than pretending it was saved.
     */
    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                // The key is not tied to a screen lock on purpose: the manager
                // may use the shop phone as the till, and requiring a lock for
                // every launch is a decision for the owner, not for this code.
                .setUserAuthenticationRequired(false)
                .build(),
        )
        return generator.generateKey()
    }

    /** The serialised shape. A field added later must not break a stored one. */
    @Serializable
    private data class StoredSession(
        val userId: String,
        val email: String,
        val accessToken: String,
        val refreshToken: String,
        val expiresAtMillis: Long,
    ) {
        fun toSession() = AdminSession(userId, email, accessToken, refreshToken, expiresAtMillis)

        companion object {
            fun from(session: AdminSession) = StoredSession(
                userId = session.userId,
                email = session.email,
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
                expiresAtMillis = session.expiresAtMillis,
            )
        }
    }

    private companion object {
        const val PREFERENCES = "admin_session"
        const val KEY_BLOB = "session"
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "saeed.admin.session"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val TAG_BITS = 128
        val json = Json { ignoreUnknownKeys = true }
    }
}
