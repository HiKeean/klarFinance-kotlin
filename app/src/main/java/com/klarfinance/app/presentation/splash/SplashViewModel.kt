package com.klarfinance.app.presentation.splash

import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klarfinance.app.core.network.NetworkUnavailableException
import com.klarfinance.app.core.security.BiometricAuthHelper
import com.klarfinance.app.core.session.SecureTokenStore
import com.klarfinance.app.domain.model.AccountState
import com.klarfinance.app.domain.usecase.GetProfileUseCase
import com.klarfinance.app.domain.usecase.RefreshSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SPLASH_DELAY_MS = 1500L
private const val TAG = "SplashViewModel"

/**
 * Session restore on cold start: every logged-in user has a refresh token persisted in
 * [SecureTokenStore] (written unconditionally by AuthRepositoryImpl.login), so this redeems it
 * for a fresh session instead of always landing on [AccountState.GUEST]. If the user opted into
 * "Sidik Jari" ([SecureTokenStore.isAppLockEnabled]) a biometric prompt gates the redemption;
 * otherwise it happens silently. No stored token, app-lock enabled but biometric unavailable/
 * cancelled, or a genuinely REJECTED refresh (revoked/expired/already redeemed) all fall back
 * to GUEST. Being offline is handled separately and deliberately does NOT log the user out -
 * see [resolveSession].
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val secureTokenStore: SecureTokenStore,
    private val refreshSessionUseCase: RefreshSessionUseCase,
    private val getProfileUseCase: GetProfileUseCase,
) : ViewModel() {

    private val _resolved = MutableSharedFlow<AccountState>()
    val resolved: SharedFlow<AccountState> = _resolved.asSharedFlow()

    fun resolveSession(activity: FragmentActivity) {
        viewModelScope.launch {
            delay(SPLASH_DELAY_MS)

            val storedRefreshToken = secureTokenStore.getRefreshToken()
            if (storedRefreshToken == null) {
                Log.w(TAG, "No stored refresh token - falling back to GUEST")
                _resolved.emit(AccountState.GUEST)
                return@launch
            }

            if (secureTokenStore.isAppLockEnabled()) {
                if (!BiometricAuthHelper.isAvailable(activity)) {
                    Log.w(TAG, "App-lock enabled but biometric hardware unavailable/not enrolled - falling back to GUEST")
                    _resolved.emit(AccountState.GUEST)
                    return@launch
                }

                val biometricResult = BiometricAuthHelper.authenticate(
                    activity = activity,
                    title = "Masuk ke KlarFinance",
                    subtitle = "Gunakan sidik jari untuk melanjutkan sesi kamu",
                )
                if (biometricResult.isFailure) {
                    Log.w(TAG, "Biometric prompt failed/cancelled", biometricResult.exceptionOrNull())
                    _resolved.emit(AccountState.GUEST)
                    return@launch
                }
            }

            refreshSessionUseCase(storedRefreshToken)
                .onSuccess { accountState -> _resolved.emit(accountState) }
                .onFailure { throwable ->
                    if (throwable is NetworkUnavailableException) {
                        // Offline - the token itself might be perfectly fine, we just can't
                        // reach the server to redeem it right now. Do NOT clear it and do NOT
                        // force GUEST (that would look exactly like an auto-logout to the
                        // user) - fall back to the last cached profile instead (getProfile
                        // already does its own network-then-cache fallback, so this hits the
                        // same NetworkUnavailableException and resolves from Room).
                        Log.w(TAG, "No connectivity - keeping session, resolving from cached profile", throwable)
                        val cachedAccountState = getProfileUseCase().getOrNull()?.value?.accountState
                        _resolved.emit(cachedAccountState ?: AccountState.GUEST)
                    } else {
                        // Genuinely rejected (revoked/expired/already redeemed elsewhere) -
                        // stop trying so this doesn't prompt biometric forever on a dead token.
                        Log.e(TAG, "Refresh token redemption failed - clearing stored token", throwable)
                        secureTokenStore.clear()
                        _resolved.emit(AccountState.GUEST)
                    }
                }
        }
    }
}
