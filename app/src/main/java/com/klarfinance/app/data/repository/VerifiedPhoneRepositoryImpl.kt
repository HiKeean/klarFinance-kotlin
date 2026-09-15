package com.klarfinance.app.data.repository

import com.klarfinance.app.data.local.VerifiedPhoneDao
import com.klarfinance.app.data.local.VerifiedPhoneEntity
import com.klarfinance.app.domain.repository.VerifiedPhoneRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class VerifiedPhoneRepositoryImpl @Inject constructor(
    private val dao: VerifiedPhoneDao,
) : VerifiedPhoneRepository {

    override suspend fun markVerified(phone: String) {
        dao.upsert(VerifiedPhoneEntity(phone = phone, verifiedAtMillis = System.currentTimeMillis()))
    }

    override suspend fun isRecentlyVerified(phone: String): Boolean {
        val entity = dao.findByPhone(phone) ?: return false
        val age = System.currentTimeMillis() - entity.verifiedAtMillis
        return age in 0 until VERIFICATION_VALIDITY_MILLIS
    }

    private companion object {
        // Not backend-enforced - verify-otp is single-use server-side (deleted from Redis
        // right after) and register doesn't take an OTP at all, so there's no server truth
        // to validate this window against. Purely a local UX cutoff so a "verified" mark
        // doesn't stay valid forever on a shared/lost device. 24h is a guess, not confirmed
        // with the user - adjust if they want a different window.
        val VERIFICATION_VALIDITY_MILLIS = TimeUnit.HOURS.toMillis(24)
    }
}
