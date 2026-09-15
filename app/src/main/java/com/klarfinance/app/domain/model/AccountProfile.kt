package com.klarfinance.app.domain.model

data class AccountProfile(
    val name: String?,
    val phone: String?,
    val role: String?,
    val dob: String?,
    val email: String?,
    val emailVerified: Boolean,
    /** Re-derived from the backend's current status (see AuthRepositoryImpl.getProfile) -
     * lets a screen refresh AccountState on demand (e.g. after an FCM approval push) instead
     * of only at login/register time. */
    val accountState: AccountState,
)
