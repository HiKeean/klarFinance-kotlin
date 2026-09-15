package com.klarfinance.app.presentation.qris.amount

import com.klarfinance.app.domain.model.LimitSummary

/** Bunga QRIS - tenor SELALU 1 bulan (konfirmasi user), preview di klien doang, backend
 * (LoanInterestPolicy.monthlyRatePercent) tetap source of truth. */
private const val QRIS_MONTHLY_RATE_PERCENT = 3.5

data class QrisUiState(
    val isScanning: Boolean = true,
    val scanErrorMessage: String? = null,
    val merchantName: String? = null,
    val token: String? = null,

    val limitSummary: LimitSummary? = null,

    val amountInput: String = "",
    val isSubmitting: Boolean = false,
    val submitErrorMessage: String? = null,

    /** Step-up auth (konfirmasi user 2026-09-07) - lihat RequestLoanUiState field yang sama
     * persis alasannya (fingerprint kalau aktif, TransactionPasswordDialog kalau enggak). */
    val requiresPasswordConfirm: Boolean = false,
    val passwordInput: String = "",
    val isVerifyingPassword: Boolean = false,
    val passwordError: String? = null,
) {
    private val amount: Long get() = amountInput.filter(Char::isDigit).toLongOrNull() ?: 0L

    fun amountValue(): Long = amount

    val interestFee: Long get() = (amount * QRIS_MONTHLY_RATE_PERCENT / 100).toLong()

    val totalToPay: Long get() = amount + interestFee

    val remainingQrisQuota: Long?
        get() {
            val summary = limitSummary ?: return null
            val quota = summary.qrisQuota ?: return null
            return (quota - (summary.qrisUsedAmount ?: 0L)).coerceAtLeast(0)
        }

    val isFormValid: Boolean
        get() = amount > 0 && remainingQrisQuota?.let { amount <= it } != false
}
