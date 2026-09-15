package com.klarfinance.app.presentation.otp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klarfinance.app.core.navigation.Screen
import com.klarfinance.app.domain.repository.VerifiedPhoneRepository
import com.klarfinance.app.domain.usecase.CheckPhoneRegisteredUseCase
import com.klarfinance.app.domain.usecase.RequestOtpUseCase
import com.klarfinance.app.domain.usecase.VerifyOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val RESEND_COOLDOWN_SECONDS = 60

@HiltViewModel
class OtpViewModel @Inject constructor(
    private val verifyOtpUseCase: VerifyOtpUseCase,
    private val requestOtpUseCase: RequestOtpUseCase,
    private val verifiedPhoneRepository: VerifiedPhoneRepository,
    private val checkPhoneRegisteredUseCase: CheckPhoneRegisteredUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val phone: String = checkNotNull(savedStateHandle[Screen.OtpVerification.ARG_PHONE])

    private val _uiState = MutableStateFlow(OtpUiState(phone = phone))
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()

    /** Phone verified AND not registered yet - go to registration. */
    private val _otpVerified = MutableSharedFlow<String>()
    val otpVerified: SharedFlow<String> = _otpVerified.asSharedFlow()

    /** Phone verified AND already has an account - go straight to password entry. */
    private val _needsPasswordLogin = MutableSharedFlow<String>()
    val needsPasswordLogin: SharedFlow<String> = _needsPasswordLogin.asSharedFlow()

    private var countdownJob: Job? = null

    init {
        startCountdown()
    }

    fun onOtpChange(value: String) {
        _uiState.update { it.copy(otp = value, errorMessage = null) }
    }

    fun onVerifyClick() {
        val state = _uiState.value
        if (!state.canVerify) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            verifyOtpUseCase(state.phone, state.otp)
                .onSuccess {
                    verifiedPhoneRepository.markVerified(state.phone)
                    _uiState.update { it.copy(isLoading = false, isVerified = true) }
                    val registered = checkPhoneRegisteredUseCase(state.phone).getOrDefault(false)
                    if (registered) _needsPasswordLogin.emit(state.phone) else _otpVerified.emit(state.phone)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = throwable.message ?: "Verification failed")
                    }
                }
        }
    }

    fun onResendClick() {
        val state = _uiState.value
        if (!state.canResend) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            requestOtpUseCase(state.phone)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, otp = "") }
                    startCountdown()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = throwable.message ?: "Failed to resend OTP")
                    }
                }
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        _uiState.update { it.copy(resendSecondsRemaining = RESEND_COOLDOWN_SECONDS) }
        countdownJob = viewModelScope.launch {
            while (_uiState.value.resendSecondsRemaining > 0) {
                delay(1000)
                _uiState.update { it.copy(resendSecondsRemaining = it.resendSecondsRemaining - 1) }
            }
        }
    }
}
