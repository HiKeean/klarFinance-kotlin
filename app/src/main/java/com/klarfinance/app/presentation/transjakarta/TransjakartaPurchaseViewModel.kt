package com.klarfinance.app.presentation.transjakarta

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klarfinance.app.core.security.BiometricAuthHelper
import com.klarfinance.app.core.session.SecureTokenStore
import com.klarfinance.app.domain.usecase.GetMyTransjakartaTicketsUseCase
import com.klarfinance.app.domain.usecase.PurchaseTransjakartaTicketUseCase
import com.klarfinance.app.domain.usecase.ToggleTransjakartaTicketUsedUseCase
import com.klarfinance.app.domain.usecase.VerifyPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Scoped ke transjakartaGraph (bukan per-screen) - TransjakartaHomeScreen (qty+riwayat) dan
 * TransjakartaConfirmScreen (bayar) berbagi satu instance, mirror RequestLoanViewModel, biar qty
 * yang dipilih di Home masih ada pas submit di Confirm.
 */
@HiltViewModel
class TransjakartaPurchaseViewModel @Inject constructor(
    private val purchaseTicketUseCase: PurchaseTransjakartaTicketUseCase,
    private val getMyTicketsUseCase: GetMyTransjakartaTicketsUseCase,
    private val toggleUsedUseCase: ToggleTransjakartaTicketUsedUseCase,
    private val verifyPasswordUseCase: VerifyPasswordUseCase,
    private val secureTokenStore: SecureTokenStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransjakartaPurchaseUiState())
    val uiState: StateFlow<TransjakartaPurchaseUiState> = _uiState.asStateFlow()

    init {
        loadTickets()
    }

    fun loadTickets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTickets = true, ticketsErrorMessage = null) }
            getMyTicketsUseCase()
                .onSuccess { cached ->
                    _uiState.update {
                        it.copy(isLoadingTickets = false, tickets = cached.value, isOffline = cached.isFromCache)
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoadingTickets = false, ticketsErrorMessage = throwable.message ?: "Gagal memuat riwayat tiket")
                    }
                }
        }
    }

    fun onQtyChange(qty: Int) {
        if (qty in 1..MAX_TICKET_QTY) _uiState.update { it.copy(qty = qty) }
    }

    /** Sama pola persis QrisViewModel.onSubmitClick - fingerprint kalau aktif, password dialog
     * kalau enggak. [activity] transient, tidak disimpan sebagai field ViewModel. */
    fun onSubmitClick(activity: FragmentActivity) {
        if (_uiState.value.isSubmitting) return

        if (secureTokenStore.isAppLockEnabled()) {
            viewModelScope.launch {
                BiometricAuthHelper.authenticate(
                    activity = activity,
                    title = "Konfirmasi Beli Tiket Transjakarta",
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
        val qty = _uiState.value.qty
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitErrorMessage = null) }
            purchaseTicketUseCase(qty)
                .onSuccess { tickets -> _uiState.update { it.copy(isSubmitting = false, purchasedTickets = tickets) } }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isSubmitting = false, submitErrorMessage = throwable.message ?: "Gagal membeli tiket") }
                }
        }
    }

    /** Dipanggil parent (KlarNavHost) setelah navigate balik ke Home pasca-beli sukses - reset
     * qty+flag, sekalian refresh riwayat biar tiket baru langsung nongol. */
    fun consumePurchasedTickets() {
        _uiState.update { it.copy(purchasedTickets = null, qty = 1) }
        loadTickets()
    }

    /** Toggle used/belum (mockup, konfirmasi user "dari pencet used atau belumnya saja") - gagal
     * dibiarkan senyap (bukan transaksi kritikal, tinggal tap ulang). */
    fun onToggleUsed(ticketCode: String) {
        viewModelScope.launch {
            toggleUsedUseCase(ticketCode).onSuccess { updated ->
                _uiState.update { state ->
                    state.copy(tickets = state.tickets.map { if (it.ticketCode == updated.ticketCode) updated else it })
                }
            }
        }
    }
}
