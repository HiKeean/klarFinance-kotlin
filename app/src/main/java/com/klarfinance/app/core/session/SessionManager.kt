package com.klarfinance.app.core.session

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the current login session in memory only - there is no persistence yet (see
 * kotlin-nasabah-app knowledge on the missing session/token store), so this resets to null
 * on process death/app restart, same as [com.klarfinance.app.domain.model.AccountState].
 * Populated by [com.klarfinance.app.data.repository.AuthRepositoryImpl.login], read by
 * [com.klarfinance.app.core.network.AuthInterceptor].
 */
@Singleton
class SessionManager @Inject constructor() {
    @Volatile
    var accessToken: String? = null
        private set

    /** In-memory only, same as [accessToken] - the persisted copy (only when fingerprint is
     * enabled) lives in [SecureTokenStore] instead, kept separate on purpose. */
    @Volatile
    var refreshToken: String? = null
        private set

    fun save(accessToken: String, refreshToken: String? = null) {
        this.accessToken = accessToken
        if (refreshToken != null) this.refreshToken = refreshToken
    }

    fun clear() {
        accessToken = null
        refreshToken = null
    }
}
