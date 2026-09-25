package com.klarfinance.app.presentation.otp

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klarfinance.app.core.auth.SmsOtpEvent
import com.klarfinance.app.core.auth.SmsOtpSender
import com.klarfinance.app.core.navigation.Screen
import com.klarfinance.app.domain.model.OtpChannel
import com.klarfinance.app.domain.repository.VerifiedPhoneRepository
import com.klarfinance.app.domain.usecase.CheckPhoneRegisteredUseCase
import com.klarfinance.app.domain.usecase.RequestOtpUseCase
import com.klarfinance.app.domain.usecase.VerifyFirebasePhoneUseCase
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
    private val verifyFirebasePhoneUseCase: VerifyFirebasePhoneUseCase,
    private val smsOtpSender: SmsOtpSender,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val phone: String = checkNotNull(savedStateHandle[Screen.OtpVerification.ARG_PHONE])
    private val initialChannel: OtpChannel = savedStateHandle.get<String>(Screen.OtpVerification.ARG_CHANNEL)
        ?.let { runCatching { OtpChannel.valueOf(it) }.getOrNull() }
        ?: OtpChannel.WHATSAPP

    private val _uiState = MutableStateFlow(
        OtpUiState(phone = phone, channel = initialChannel, smsSendPending = initialChannel == OtpChannel.SMS),
    )
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()

    /** Phone verified AND not registered yet - go to registration. */
    private val _otpVerified = MutableSharedFlow<String>()
    val otpVerified: SharedFlow<String> = _otpVerified.asSharedFlow()

    /** Phone verified AND already has an account - go straight to password entry. */
    private val _needsPasswordLogin = MutableSharedFlow<String>()
    val needsPasswordLogin: SharedFlow<String> = _needsPasswordLogin.asSharedFlow()

    private var countdownJob: Job? = null

    init {
        // Jalur SMS: countdown baru jalan setelah Firebase benar-benar mengirim SMS (SmsOtpEvent.CodeSent).
        if (initialChannel == OtpChannel.WHATSAPP) startCountdown()
    }

    fun onOtpChange(value: String) {
        _uiState.update { it.copy(otp = value, errorMessage = null) }
    }

    fun onVerifyClick() {
        val state = _uiState.value
        if (!state.canVerify) return

        if (state.channel == OtpChannel.SMS) {
            verifySms(state.otp)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            verifyOtpUseCase(state.phone, state.otp)
                .onSuccess { onPhoneVerified() }
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

        if (state.channel == OtpChannel.SMS) {
            _uiState.update { it.copy(otp = "", errorMessage = null, smsSendPending = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            requestOtpUseCase(state.phone)
                .onSuccess { channel ->
                    if (channel == OtpChannel.SMS) {
                        _uiState.update { it.copy(isLoading = false, otp = "", channel = OtpChannel.SMS, smsSendPending = true) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, otp = "") }
                        startCountdown()
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = throwable.message ?: "Failed to resend OTP")
                    }
                }
        }
    }

    /** "Didn't get the code? Send via SMS" - nasabah pindah manual dari WhatsApp ke SMS. */
    fun onSwitchToSmsClick() {
        if (!_uiState.value.canSwitchToSms) return
        countdownJob?.cancel()
        _uiState.update {
            it.copy(channel = OtpChannel.SMS, otp = "", errorMessage = null, smsSendPending = true)
        }
    }

    /** Dipanggil screen saat [OtpUiState.smsSendPending] - Firebase Phone Auth butuh Activity. */
    fun sendSmsCode(activity: Activity) {
        if (!_uiState.value.smsSendPending) return
        _uiState.update { it.copy(smsSendPending = false, isLoading = true, errorMessage = null) }
        smsOtpSender.send(activity, phone) { event ->
            when (event) {
                SmsOtpEvent.CodeSent -> {
                    _uiState.update { it.copy(isLoading = false) }
                    startCountdown()
                }
                is SmsOtpEvent.AutoRetrieved -> {
                    _uiState.update { it.copy(otp = event.code ?: it.otp) }
                    verifySms(event.code)
                }
                is SmsOtpEvent.Failed -> {
                    countdownJob?.cancel()
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = event.message, resendSecondsRemaining = 0)
                    }
                }
            }
        }
    }

    private fun verifySms(code: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            smsOtpSender.confirm(code)
                .mapCatching { idToken -> verifyFirebasePhoneUseCase(phone, idToken).getOrThrow() }
                .onSuccess { onPhoneVerified() }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = throwable.message ?: "Verification failed")
                    }
                }
        }
    }

    private suspend fun onPhoneVerified() {
        verifiedPhoneRepository.markVerified(phone)
        _uiState.update { it.copy(isLoading = false, isVerified = true) }
        val registered = checkPhoneRegisteredUseCase(phone).getOrDefault(false)
        if (registered) _needsPasswordLogin.emit(phone) else _otpVerified.emit(phone)
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
