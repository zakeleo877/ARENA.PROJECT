package com.zakeleo877.saeedcore

import com.zakeleo877.saeedcore.data.remote.AdminSession
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * When an admin session must be refreshed.
 *
 * The access token lasts an hour. The refresh call existed but nothing invoked
 * it, and the expiry field was written and never read: an hour after signing in,
 * every request failed with the same 401 as "you are not an admin", and the only
 * cure on the screen was signing in again.
 *
 * The margin matters. Refreshing exactly at expiry still leaves a request that
 * was already in flight holding a dead token, so the decision is made a minute
 * early.
 */
class SessionExpiryTest {

    private fun session(expiresAt: Long) = AdminSession(
        userId = "u1",
        email = "admin@example.test",
        accessToken = "token",
        refreshToken = "refresh",
        expiresAtMillis = expiresAt,
    )

    @Test
    fun `a fresh session is not refreshed`() {
        assertFalse(session(expiresAt = 3_600_000L).isExpiring(nowMillis = 0L))
    }

    @Test
    fun `a session inside the last minute is refreshed`() {
        val expiry = 3_600_000L
        assertFalse(session(expiry).isExpiring(expiry - 61_000L))
        assertTrue(session(expiry).isExpiring(expiry - 60_000L))
        assertTrue(session(expiry).isExpiring(expiry - 1L))
    }

    @Test
    fun `an expired session is refreshed rather than used`() {
        assertTrue(session(expiresAt = 1_000L).isExpiring(nowMillis = 5_000L))
    }

    @Test
    fun `the truth is told at the exact boundary`() {
        // Equal is expiring: sending a token that dies this second is a wasted
        // request, and a failed write has no way back.
        val expiry = 10_000L
        assertTrue(session(expiry).isExpiring(nowMillis = expiry - 60_000L))
    }
}
