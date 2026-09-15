package com.klarfinance.app.core.session

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the refresh token for every logged-in session (written unconditionally from
 * [com.klarfinance.app.data.repository.AuthRepositoryImpl.login]) - this is what lets the app
 * restore a session after a restart for ALL users, not just ones who opted into fingerprint.
 * [isAppLockEnabled] is a separate opt-in flag ("Sidik Jari" security-checklist item on
 * Account) that gates whether [com.klarfinance.app.presentation.splash.SplashViewModel] must
 * clear a [com.klarfinance.app.core.security.BiometricAuthHelper] prompt before redeeming the
 * stored token on cold start, or can redeem it silently. Everything else ([SessionManager])
 * stays in-memory only.
 */
@Singleton
class SecureTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        try {
            createPrefs(context, masterKey)
        } catch (e: Exception) {
            // The keystore-backed master key can become unusable while the encrypted prefs
            // file on disk survives (lock screen change, device restore, reinstall without
            // clearing data) - decryption then fails with AEADBadTagException and crashes
            // the app on every launch. The file only ever holds a refresh token, so it's safe
            // to drop and start clean; the user just re-authenticates.
            context.deleteSharedPreferences(PREFS_NAME)
            createPrefs(context, masterKey)
        }
    }

    private fun createPrefs(context: Context, masterKey: MasterKey) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun saveRefreshToken(token: String) {
        prefs.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun hasRefreshToken(): Boolean = getRefreshToken() != null

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
    }

    fun isAppLockEnabled(): Boolean = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)

    fun clear() {
        // App-lock preference intentionally NOT cleared here - it's a device/user preference,
        // not session state, and should survive logout so the next login on this device keeps
        // requiring biometric if the user had it on.
        prefs.edit().remove(KEY_REFRESH_TOKEN).apply()
    }

    companion object {
        private const val PREFS_NAME = "klarfinance_secure_prefs"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
    }
}
