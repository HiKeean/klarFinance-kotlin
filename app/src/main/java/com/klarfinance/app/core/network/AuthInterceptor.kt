package com.klarfinance.app.core.network

import com.klarfinance.app.core.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Attaches `Authorization: Bearer <token>` when a session exists - kept separate from
 * [HmacInterceptor] (different concern: HMAC signs every request regardless of login state,
 * this only matters once a session exists). No-op for calls made before any login - public
 * endpoints ignore the header if it's simply absent.
 */
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessionManager.accessToken ?: return chain.proceed(chain.request())
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}
