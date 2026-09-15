package com.klarfinance.app.presentation.loan

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klarfinance.app.core.scan.InstalledAppsScanner
import com.klarfinance.app.core.security.BiometricAuthHelper
import com.klarfinance.app.core.session.SecureTokenStore
import com.klarfinance.app.domain.model.LoanRequestResult
import com.klarfinance.app.domain.usecase.GetLimitSummaryUseCase
import com.klarfinance.app.domain.usecase.GetSavedBankAccountsUseCase
import com.klarfinance.app.domain.usecase.RequestLoanUseCase
import com.klarfinance.app.domain.usecase.VerifyPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class RequestLoanViewModel @Inject constructor(
    private val getLimitSummaryUseCase: GetLimitSummaryUseCase,
    private val getSavedBankAccountsUseCase: GetSavedBankAccountsUseCase,
    private val requestLoanUseCase: RequestLoanUseCase,
    private val verifyPasswordUseCase: VerifyPasswordUseCase,
    private val secureTokenStore: SecureTokenStore,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RequestLoanUiState())
    val uiState: StateFlow<RequestLoanUiState> = _uiState.asStateFlow()

    private val _submitted = MutableSharedFlow<LoanRequestResult>()
    val submitted: SharedFlow<LoanRequestResult> = _submitted.asSharedFlow()

    init {
        loadLimit()
        loadSavedBankAccounts()
    }

    private fun loadLimit() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLimit = true, loadErrorMessage = null) }
            getLimitSummaryUseCase()
                .onSuccess { summary -> _uiState.update { it.copy(isLoadingLimit = false, limitSummary = summary) } }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoadingLimit = false, loadErrorMessage = throwable.message ?: "Gagal memuat data limit")
                    }
                }
        }
    }

    /** Rekening tujuan pencairan yang pernah dipakai (konfirmasi user 2026-09-07) - isi dropdown
     * kalau ada, jatuh balik ke input manual kalau kosong (lihat RequestLoanUiState.isBankStepValid).
     * Kegagalan load di sini TIDAK fatal (beda dari loadLimit) - list cuma tetap kosong, nasabah
     * masih bisa lanjut isi manual seperti sebelum fitur ini ada. */
    private fun loadSavedBankAccounts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSavedBankAccounts = true) }
            getSavedBankAccountsUseCase()
                .onSuccess { accounts ->
                    _uiState.update {
                        it.copy(
                            isLoadingSavedBankAccounts = false,
                            savedBankAccounts = accounts,
                            selectedSavedBankAccountId = accounts.firstOrNull()?.id,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingSavedBankAccounts = false) }
                }
        }
    }

    /** Live-format jadi "10.000"/"100.000" (konfirmasi user 2026-09-07) selagi user ngetik -
     * amountInput menyimpan string SUDAH diformat (bukan digit mentah), lihat
     * RequestLoanUiState.amount yang strip titiknya balik pas parse ke Long. */
    fun onAmountChange(value: String) =
        _uiState.update { it.copy(amountInput = groupThousands(value.filter(Char::isDigit)), submitErrorMessage = null) }

    /** Tap salah satu dari 3 kartu preset (dihitung dari plafond nasabah, lihat
     * RequestLoanUiState.amountPresets) - isi langsung field nominal, sama seperti user ngetik
     * manual. */
    fun onAmountPresetSelected(amount: Long) = _uiState.update { it.copy(amountInput = groupThousands(amount.toString())) }

    fun onTenorSelected(tenorMonths: Int) = _uiState.update { it.copy(tenorMonths = tenorMonths, submitErrorMessage = null) }

    /** Pilih salah satu rekening tersimpan dari dropdown (konfirmasi user 2026-09-07) - cuma
     * relevan kalau [RequestLoanUiState.savedBankAccounts] non-kosong. */
    fun onSavedBankAccountSelected(id: Int) =
        _uiState.update { it.copy(selectedSavedBankAccountId = id, submitErrorMessage = null) }

    fun onBankAccountNumberChange(value: String) =
        _uiState.update { it.copy(bankAccountNumber = value, submitErrorMessage = null) }

    fun onBankCodeSelected(bankCode: String) = _uiState.update { it.copy(bankCode = bankCode) }

    /** Step-up auth dulu (konfirmasi user 2026-09-07) sebelum benar-benar submit - fingerprint
     * kalau nasabah sudah aktifkan (SecureTokenStore.isAppLockEnabled(), sama sinyal yang dipakai
     * AccountScreen "Sidik Jari"), kalau enggak munculin TransactionPasswordDialog dulu (lihat
     * onPasswordConfirm). [activity] cuma dipakai transient di sini (BiometricPrompt butuh
     * FragmentActivity) - TIDAK disimpan sebagai field ViewModel, sama pola dengan
     * AccountViewModel.onEnableFingerprintClick/SplashViewModel. */
    fun onSubmitClick(activity: FragmentActivity) {
        val state = _uiState.value
        if (!state.isAmountStepValid || !state.isBankStepValid || state.isSubmitting) return

        if (secureTokenStore.isAppLockEnabled()) {
            viewModelScope.launch {
                BiometricAuthHelper.authenticate(
                    activity = activity,
                    title = "Konfirmasi Pengajuan Pinjaman",
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
        val (resolvedBankCode, resolvedBankAccountNumber) = state.resolvedBankAccount ?: run {
            _uiState.update { it.copy(submitErrorMessage = "Pilih atau isi rekening tujuan dulu") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitErrorMessage = null) }

            // Kalau proyeksi utilisasi > 30%, backend WAJIB dapat data scan pinjol/bank
            // terbaru (konfirmasi user - "scan ulang", bukan pakai data lama) supaya BM
            // yang review nanti lihat kondisi terkini, bukan cuma yang dulu direkam pas
            // registrasi. Scan-nya cepat (baca PackageManager lokal, bukan network) jadi
            // aman dijalankan sinkron sebelum submit tanpa loading state terpisah.
            val (pinjolApps, bankApps) = if (state.willRequireReview) {
                withContext(Dispatchers.IO) { InstalledAppsScanner.scan(appContext) }
            } else {
                InstalledAppsScanner.ScanResult(emptyList(), emptyList())
            }

            requestLoanUseCase(
                amount = state.amountValue(),
                tenorMonths = state.tenorMonths,
                bankAccountNumber = resolvedBankAccountNumber,
                bankCode = resolvedBankCode,
                pinjolApps = pinjolApps,
                bankApps = bankApps,
            )
                .onSuccess { result ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    _submitted.emit(result)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isSubmitting = false, submitErrorMessage = throwable.message ?: "Gagal mengajukan pinjaman")
                    }
                }
        }
    }
}
