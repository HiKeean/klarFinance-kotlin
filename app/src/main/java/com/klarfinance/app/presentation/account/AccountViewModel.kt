package com.klarfinance.app.presentation.account

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klarfinance.app.core.location.LocationScheduler
import com.klarfinance.app.core.security.BiometricAuthHelper
import com.klarfinance.app.core.session.SecureTokenStore
import com.klarfinance.app.core.session.SessionManager
import com.klarfinance.app.domain.usecase.ChangePasswordUseCase
import com.klarfinance.app.domain.usecase.GetLocationConsentUseCase
import com.klarfinance.app.domain.usecase.GetProfileUseCase
import com.klarfinance.app.domain.usecase.LogLocationFailureUseCase
import com.klarfinance.app.domain.usecase.LogoutUseCase
import com.klarfinance.app.domain.usecase.SetLocationConsentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val sessionManager: SessionManager,
    private val secureTokenStore: SecureTokenStore,
    private val getLocationConsentUseCase: GetLocationConsentUseCase,
    private val setLocationConsentUseCase: SetLocationConsentUseCase,
    private val logLocationFailureUseCase: LogLocationFailureUseCase,
    private val locationScheduler: LocationScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState(isFingerprintEnabled = secureTokenStore.isAppLockEnabled()))
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    private val _loggedOut = MutableSharedFlow<Unit>()
    val loggedOut: SharedFlow<Unit> = _loggedOut.asSharedFlow()

    /** One-off feedback for the fingerprint flow (unavailable hardware, prompt cancelled/
     * failed) - shown as a snackbar by AccountScreen, same host as the "coming soon" ones. */
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        loadProfile()
        loadLocationConsent()
    }

    private fun loadLocationConsent() {
        viewModelScope.launch {
            getLocationConsentUseCase().onSuccess { consentGiven ->
                _uiState.update { it.copy(isLocationConsentGiven = consentGiven) }
            }
        }
    }

    /** Called by AccountScreen only AFTER the location permission flow (foreground, and
     * best-effort background - see LocationPermissionHelper) has already resolved to "at least
     * foreground granted" - this method never itself requests permission, that needs an
     * Activity/ActivityResultLauncher which lives in the Composable, not here. */
    fun onLocationConsentEnabled() {
        val state = _uiState.value
        if (state.isLocationConsentGiven || state.isUpdatingLocationConsent) return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingLocationConsent = true) }
            setLocationConsentUseCase(true)
                .onSuccess {
                    locationScheduler.schedule()
                    _uiState.update { it.copy(isUpdatingLocationConsent = false, isLocationConsentGiven = true) }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isUpdatingLocationConsent = false) }
                    // No snackbar here on purpose (user's explicit call 2026-09-04) - log-only,
                    // see LogLocationFailureUseCase.
                    logLocationFailureUseCase(throwable.message ?: "Failed to enable location consent")
                }
        }
    }

    fun onLocationConsentDisabled() {
        val state = _uiState.value
        if (!state.isLocationConsentGiven || state.isUpdatingLocationConsent) return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingLocationConsent = true) }
            setLocationConsentUseCase(false)
                .onSuccess {
                    locationScheduler.cancel()
                    _uiState.update { it.copy(isUpdatingLocationConsent = false, isLocationConsentGiven = false) }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isUpdatingLocationConsent = false) }
                    _snackbarMessage.emit(throwable.message ?: "Gagal menonaktifkan lokasi")
                }
        }
    }

    /** No snackbar here on purpose (user's explicit call 2026-09-04) - log-only, see
     * LogLocationFailureUseCase. */
    fun onLocationPermissionDenied() {
        viewModelScope.launch {
            logLocationFailureUseCase("Location permission denied by user")
        }
    }

    /** "Sidik Jari" checklist item - the refresh token itself is ALWAYS persisted (since login,
     * see AuthRepositoryImpl) regardless of this setting; this only flips the app-lock flag in
     * [SecureTokenStore] that decides whether [com.klarfinance.app.presentation.splash.SplashViewModel]
     * must clear a biometric prompt before redeeming it on cold start. The prompt here is just to
     * prove the sensor actually works before relying on it later. */
    fun onEnableFingerprintClick(activity: FragmentActivity) {
        val state = _uiState.value
        if (state.isFingerprintEnabled || state.isEnablingFingerprint) return

        if (sessionManager.refreshToken == null) {
            viewModelScope.launch { _snackbarMessage.emit("Sesi login tidak lengkap - coba login ulang dulu") }
            return
        }
        if (!BiometricAuthHelper.isAvailable(activity)) {
            viewModelScope.launch { _snackbarMessage.emit("Sidik jari tidak tersedia di perangkat ini") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isEnablingFingerprint = true) }
            BiometricAuthHelper.authenticate(
                activity = activity,
                title = "Aktifkan Sidik Jari",
                subtitle = "Verifikasi sidik jari untuk menyimpan sesi login kamu",
            ).onSuccess {
                secureTokenStore.setAppLockEnabled(true)
                _uiState.update { it.copy(isEnablingFingerprint = false, isFingerprintEnabled = true) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isEnablingFingerprint = false) }
                _snackbarMessage.emit(throwable.message ?: "Gagal mengaktifkan sidik jari")
            }
        }
    }

    fun onLogoutClick() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true) }
            logoutUseCase()
            // Local session is always cleared by the use case regardless of server result
            // (see AuthRepositoryImpl.logout) - the user's intent to log out always wins.
            _uiState.update { it.copy(isLoggingOut = false) }
            _loggedOut.emit(Unit)
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadErrorMessage = null) }
            getProfileUseCase()
                .onSuccess { cached ->
                    _uiState.update { it.copy(isLoading = false, profile = cached.value, isOffline = cached.isFromCache) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, loadErrorMessage = throwable.message ?: "Gagal memuat akun")
                    }
                }
        }
    }

    fun onToggleSecurityChecklist() {
        _uiState.update { it.copy(isSecurityChecklistExpanded = !it.isSecurityChecklistExpanded) }
    }

    fun onToggleChangePassword() {
        _uiState.update {
            it.copy(
                isChangePasswordExpanded = !it.isChangePasswordExpanded,
                oldPassword = "",
                newPassword = "",
                confirmPassword = "",
                changePasswordError = null,
                changePasswordSuccess = false,
            )
        }
    }

    fun onOldPasswordChange(value: String) =
        _uiState.update { it.copy(oldPassword = value, changePasswordError = null) }

    fun onNewPasswordChange(value: String) =
        _uiState.update { it.copy(newPassword = value, changePasswordError = null) }

    fun onConfirmPasswordChange(value: String) =
        _uiState.update { it.copy(confirmPassword = value, changePasswordError = null) }

    fun onSubmitChangePassword() {
        val state = _uiState.value
        if (state.newPassword != state.confirmPassword) {
            _uiState.update { it.copy(changePasswordError = "Konfirmasi password tidak cocok") }
            return
        }
        if (!state.isChangePasswordFormValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isChangingPassword = true, changePasswordError = null) }
            changePasswordUseCase(state.oldPassword, state.newPassword)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isChangingPassword = false,
                            changePasswordSuccess = true,
                            isChangePasswordExpanded = false,
                            oldPassword = "",
                            newPassword = "",
                            confirmPassword = "",
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isChangingPassword = false,
                            changePasswordError = throwable.message ?: "Gagal mengubah password",
                        )
                    }
                }
        }
    }
}
