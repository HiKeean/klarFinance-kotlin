package com.klarfinance.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klarfinance.app.domain.usecase.GetLoanHistoryUseCase
import com.klarfinance.app.domain.usecase.RepayLoanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getLoanHistoryUseCase: GetLoanHistoryUseCase,
    private val repayLoanUseCase: RepayLoanUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadErrorMessage = null) }
            getLoanHistoryUseCase()
                .onSuccess { cached ->
                    _uiState.update { it.copy(isLoading = false, items = cached.value, isOffline = cached.isFromCache) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, loadErrorMessage = throwable.message ?: "Gagal memuat riwayat")
                    }
                }
        }
    }

    /** Dipanggil begitu halaman Bayar dibuka - cicilan yang kecentang di loan lain (atau sisa
     * dari kunjungan sebelumnya) tidak boleh kebawa. */
    fun resetPaymentSelection() {
        _uiState.update { it.copy(selectedInstallmentNumbers = emptySet(), paymentErrorMessage = null) }
    }

    fun onInstallmentToggled(installmentNumber: Int) {
        _uiState.update { state ->
            val current = state.selectedInstallmentNumbers
            val updated = if (installmentNumber in current) current - installmentNumber else current + installmentNumber
            state.copy(selectedInstallmentNumbers = updated, paymentErrorMessage = null)
        }
    }

    fun submitPayment(loanId: Int) {
        val state = _uiState.value
        if (state.isSubmittingPayment) return
        val amount = state.selectedPaymentTotal(loanId)

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingPayment = true, paymentErrorMessage = null) }
            repayLoanUseCase(loanId, amount)
                .onSuccess { updatedItem ->
                    _uiState.update { current ->
                        current.copy(
                            isSubmittingPayment = false,
                            selectedInstallmentNumbers = emptySet(),
                            items = current.items.map { if (it.loanId == updatedItem.loanId) updatedItem else it },
                            pendingPaymentMessage = "Pembayaran berhasil",
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSubmittingPayment = false,
                            paymentErrorMessage = throwable.message ?: "Gagal memproses pembayaran",
                        )
                    }
                }
        }
    }

    fun consumePaymentMessage() {
        _uiState.update { it.copy(pendingPaymentMessage = null) }
    }
}
