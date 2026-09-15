package com.klarfinance.app.presentation.qris.amount

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klarfinance.app.core.navigation.Screen
import com.klarfinance.app.core.security.BiometricAuthHelper
import com.klarfinance.app.core.session.SecureTokenStore
import com.klarfinance.app.domain.model.QrisConfirmResult
import com.klarfinance.app.domain.usecase.ConfirmQrisUseCase
import com.klarfinance.app.domain.usecase.GetLimitSummaryUseCase
import com.klarfinance.app.domain.usecase.ScanQrisUseCase
import com.klarfinance.app.domain.usecase.VerifyPasswordUseCase
import com.klarfinance.app.presentation.loan.groupThousands
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

/**
 * Scoped per QrisAmountScreen (bukan graph-shared seperti RequestLoanViewModel) - cuma satu
 * data yang perlu "dibawa" dari langkah scan (merchantCode, dari QR mentah) dan itu cukup lewat
 * nav arg biasa (SavedStateHandle), gak perlu ViewModel dibagi lintas 2 layar.
 */
@HiltViewModel
class QrisViewModel @Inject constructor(
    private val scanQrisUseCase: ScanQrisUseCase,
    private val confirmQrisUseCase: ConfirmQrisUseCase,
    private val getLimitSummaryUseCase: GetLimitSummaryUseCase,
    private val verifyPasswordUseCase: VerifyPasswordUseCase,
    private val secureTokenStore: SecureTokenStore,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val merchantCode: String = checkNotNull(savedStateHandle[Screen.QrisAmount.ARG_MERCHANT_CODE])

    private val _uiState = MutableStateFlow(QrisUiState())
    val uiState: StateFlow<QrisUiState> = _uiState.asStateFlow()

    private val _submitted = MutableSharedFlow<QrisConfirmResult>()
    val submitted: SharedFlow<QrisConfirmResult> = _submitted.asSharedFlow()

    init {
        scan()
        loadLimit()
    }

    private fun scan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanErrorMessage = null) }
            scanQrisUseCase(merchantCode)
                .onSuccess { info ->
                    _uiState.update { it.copy(isScanning = false, token = info.token, merchantName = info.merchantName) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isScanning = false, scanErrorMessage = throwable.message ?: "QR tidak valid")
                    }
                }
        }
    }

    private fun loadLimit() {
        viewModelScope.launch {
            getLimitSummaryUseCase().onSuccess { summary -> _uiState.update { it.copy(limitSummary = summary) } }
            // Kegagalan load limit gak diperlakukan fatal di sini - remainingQrisQuota cuma jadi
            // null (preview kuota gak tampil), backend tetap validasi ulang beneran pas confirm.
        }
    }

    /** Live-format jadi "10.000"/"100.000" (konfirmasi user 2026-09-07) selagi user ngetik -
     * amountInput menyimpan string SUDAH diformat, lihat QrisUiState.amount yang strip
     * titiknya balik pas parse ke Long. */
    fun onAmountChange(value: String) =
        _uiState.update { it.copy(amountInput = groupThousands(value.filter(Char::isDigit)), submitErrorMessage = null) }

    /** Step-up auth dulu (konfirmasi user 2026-09-07) - lihat RequestLoanViewModel.onSubmitClick
     * buat penjelasan lengkap pola yang sama persis (fingerprint vs TransactionPasswordDialog).
     * [activity] transient, tidak disimpan sebagai field ViewModel. */
    fun onSubmitClick(activity: FragmentActivity) {
        val state = _uiState.value
        if (state.token == null || !state.isFormValid || state.isSubmitting) return

        if (secureTokenStore.isAppLockEnabled()) {
            viewModelScope.launch {
                BiometricAuthHelper.authenticate(
                    activity = activity,
                    title = "Konfirmasi Pembayaran QRIS",
                    subtitle = "Verifikasi sidik jari untuk melanjutkan transaksi ini",
                ).onSuccess {
                    performSubmit()
                }.onFailure { throwable ->
                    _uiState.update { it.copy(submitErrorMessage = throwable.message ?: "Verifikasi sidik jari gagal") }
                }
            }
        } else {
            _uiState.update { it.copy(requiresPasswordConfirm = true, passwordInput = "", passwordError = null) }
        }
    }

    fun onPasswordInputChange(value: String) = _uiState.update { it.copy(passwordInput = value, passwordError = null) }

    fun onPasswordConfirmDismiss() {
        if (_uiState.value.isVerifyingPassword) return
        _uiState.update { it.copy(requiresPasswordConfirm = false, passwordInput = "", passwordError = null) }
    }

    fun onPasswordConfirm() {
        val state = _uiState.value
        if (state.passwordInput.isBlank() || state.isVerifyingPassword) return

        viewModelScope.launch {
            _uiState.update { it.copy(isVerifyingPassword = true, passwordError = null) }
            verifyPasswordUseCase(state.passwordInput)
                .onSuccess {
                    _uiState.update {
                        it.copy(isVerifyingPassword = false, requiresPasswordConfirm = false, passwordInput = "")
                    }
                    performSubmit()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isVerifyingPassword = false, passwordError = throwable.message ?: "Password salah")
                    }
                }
        }
    }

    private fun performSubmit() {
        val state = _uiState.value
        val token = state.token ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitErrorMessage = null) }
            confirmQrisUseCase(token, state.amountValue())
                .onSuccess { result ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    _submitted.emit(result)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isSubmitting = false, submitErrorMessage = throwable.message ?: "Gagal memproses pembayaran")
                    }
                }
        }
    }
}
