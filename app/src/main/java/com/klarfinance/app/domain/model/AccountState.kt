package com.klarfinance.app.domain.model

/**
 * Where the current device stands relative to an account, driving Home's locked/unlocked
 * behavior. Persisted refresh token (see [com.klarfinance.app.core.session.SecureTokenStore])
 * lets [com.klarfinance.app.presentation.splash.SplashViewModel] re-derive this on cold start
 * for any previously logged-in device - falls back to [GUEST] only when there's no stored
 * token, or redeeming it fails (revoked/expired/offline with no cached fallback).
 */
enum class AccountState {
    /** No account on this device yet - every locked tap routes to Login. */
    GUEST,

    /** Registered (KYC submitted), but the credit application hasn't been decided yet -
     * no active limit to transact with. Locked taps show a "still under review" dialog
     * instead of Login, since the user already has an account. */
    PENDING_APPLICATION,

    /** Has an approved, active limit. Locked taps (features with no real screen yet)
     * show a "coming soon" snackbar instead of Login. */
    ACTIVE,

    ;

    companion object {
        /** Maps the backend's `LoginResponse.accountStatus` string ("ACTIVE"/"PENDING_APPLICATION")
         * to this enum. Defaults to [PENDING_APPLICATION] for a null/unrecognized value rather
         * than [GUEST] - if this is being resolved at all, the phone/password just authenticated
         * successfully, so the device is never actually a guest at this point. */
        fun fromBackend(value: String?): AccountState = when (value) {
            "ACTIVE" -> ACTIVE
            else -> PENDING_APPLICATION
        }
    }
}
