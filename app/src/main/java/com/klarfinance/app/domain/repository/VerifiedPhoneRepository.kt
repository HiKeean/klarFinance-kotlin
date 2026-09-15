package com.klarfinance.app.domain.repository

/**
 * Local-only record of phone numbers that already passed OTP verification, so the
 * user isn't asked to request a fresh OTP if they land back on the Login screen with
 * the same number (e.g. resuming an interrupted register flow). Purely a client-side
 * UX convenience - the backend has no matching concept (verify-otp is single-use and
 * register doesn't require an OTP at all).
 */
interface VerifiedPhoneRepository {
    suspend fun markVerified(phone: String)
    suspend fun isRecentlyVerified(phone: String): Boolean
}
