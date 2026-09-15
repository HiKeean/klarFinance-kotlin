package com.klarfinance.app.data.dto

import kotlinx.serialization.Serializable

/** Mirrors backend LimitSummaryResponse (fin/dto/response) - GET api/v1/nasabah/loan/limit. */
@Serializable
data class LimitSummaryResponseDto(
    val totalLimit: Double? = null,
    val usedLimit: Double? = null,
    val availableLimit: Double? = null,
    val qrisQuota: Double? = null,
    val qrisUsedAmount: Double? = null,
    val hasPendingLoanReview: Boolean = false,
)

/** Mirrors backend LoanRequest (fin/dto/request) - POST api/v1/nasabah/loan. pinjolApps/bankApps
 * cuma genuinely dipakai backend kalau pengajuan ini melewati 30% dari plafond (lihat
 * LoanInterestPolicy.exceedsReviewThreshold) - tetap dikirim selalu, backend yang menentukan
 * relevan atau tidak. */
@Serializable
data class LoanRequestDto(
    val amount: Double,
    val tenorMonths: Int,
    val bankAccountNumber: String,
    val bankCode: String,
    val pinjolApps: List<String> = emptyList(),
    val bankApps: List<String> = emptyList(),
)

/** Mirrors backend LoanResponse (fin/dto/response). Semua field selain reviewRequired/message
 * null kalau reviewRequired=true (pengajuan ditahan sebagai LoanReviewRequest, belum jadi Loan). */
@Serializable
data class LoanResponseDto(
    val loanId: Int? = null,
    val requestedAmount: Double? = null,
    val disbursedAmount: Double? = null,
    val adminFee: Double? = null,
    val totalAmountDue: Double? = null,
    val tenorMonths: Int? = null,
    val installmentAmount: Double? = null,
    val firstDueDate: String? = null,
    val referralDiscountApplied: Double? = null,
    val reviewRequired: Boolean = false,
    val reviewRequestId: Int? = null,
    val message: String? = null,
)

/** Mirrors backend `BankAccountResponse` (fin/dto/response) - GET api/v1/nasabah/loan/bank-accounts. */
@Serializable
data class BankAccountResponseDto(
    val id: Int? = null,
    val bankCode: String? = null,
    val bankAccountNumber: String? = null,
)

/** Mirrors backend `LoanHistoryItemResponse` (fin/dto/response) - GET api/v1/nasabah/loan/history.
 * `type` "LOAN" (tarik tunai) atau "QRIS_PAYMENT" (bayar QRIS), `status` "ACTIVE"/"OVERDUE"/
 * "PAID_OFF" - lihat LoanService#getMyLoanHistory buat cara diturunkannya. */
@Serializable
data class LoanHistoryItemResponseDto(
    val loanId: Int? = null,
    val type: String? = null,
    val merchantName: String? = null,
    val requestedAmount: Double? = null,
    val totalAmountDue: Double? = null,
    val tenorMonths: Int? = null,
    val status: String? = null,
    val paidInstallments: Int? = null,
    val totalInstallments: Int? = null,
    val nextDueDate: String? = null,
    val nextDueAmount: Double? = null,
    val createdAt: String? = null,
    val installments: List<InstallmentItemDto> = emptyList(),
)

/** Mirrors backend `LoanHistoryItemResponse.InstallmentItem` - jadwal LENGKAP semua cicilan
 * satu Loan, urut installmentNumber, dipakai dialog detail jatuh tempo di History. */
@Serializable
data class InstallmentItemDto(
    val installmentNumber: Int? = null,
    val dueDate: String? = null,
    val amount: Double? = null,
    val status: String? = null,
    val paidAt: String? = null,
)

/** Mirrors backend `RepaymentRequest` (fin/dto/request) - POST
 * api/v1/nasabah/loan/{loanId}/repayment. Backend yang nentuin minimum (lihat
 * LoanService#repay) - nominal berapapun boleh dikirim dari sini, error minimum-payment
 * balik sebagai pesan biasa (ApiResponse.message) kalau ditolak. */
@Serializable
data class RepaymentRequestDto(
    val amount: Double,
)
