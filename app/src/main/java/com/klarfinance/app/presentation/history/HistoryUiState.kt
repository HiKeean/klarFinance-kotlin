package com.klarfinance.app.presentation.history

import com.klarfinance.app.domain.model.LoanHistoryItem

data class HistoryUiState(
    val isLoading: Boolean = true,
    val items: List<LoanHistoryItem> = emptyList(),
    val loadErrorMessage: String? = null,
    /** True when [items] is the last cached list (Room), shown because the network was
     * unreachable - see [com.klarfinance.app.domain.model.Cached]. */
    val isOffline: Boolean = false,

    /** Cicilan (belum lunas) yang dicentang user di halaman Bayar - nominal total di atas
     * halaman itu adalah jumlah [LoanInstallment.amount] dari nomor-nomor ini. Direset tiap
     * halaman Bayar dibuka (lihat [HistoryViewModel.resetPaymentSelection]). */
    val selectedInstallmentNumbers: Set<Int> = emptySet(),
    val isSubmittingPayment: Boolean = false,
    /** Pesan dari backend (mis. "Cicilan sudah jatuh tempo - minimum pembayaran Rp X") - backend
     * yang jadi source of truth validasi, bukan dihitung ulang di klien. */
    val paymentErrorMessage: String? = null,
    /** Snackbar "Pembayaran berhasil" - StateFlow (bukan SharedFlow) supaya nilainya tetap ada
     * begitu halaman Bayar pop back ke History dan History mulai collect lagi (menghindari race
     * kalau dipakai SharedFlow biasa: event bisa emit sebelum History selesai navigasi balik dan
     * mulai collect). Dikonsumsi (di-null-kan lagi) oleh HistoryScreen lewat
     * [HistoryViewModel.consumePaymentMessage] begitu snackbar ditampilkan. */
    val pendingPaymentMessage: String? = null,
) {
    fun findItem(loanId: Int): LoanHistoryItem? = items.find { it.loanId == loanId }

    fun selectedPaymentTotal(loanId: Int): Long =
        findItem(loanId)
            ?.installments
            ?.filter { !it.isPaid && it.installmentNumber in selectedInstallmentNumbers }
            ?.sumOf { it.amount }
            ?: 0L
}
