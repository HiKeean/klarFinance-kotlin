package com.klarfinance.app.data.dto

import kotlinx.serialization.Serializable

/** Mirrors backend QrisScanRequest - POST api/v1/nasabah/qris/transactions/scan. */
@Serializable
data class QrisScanRequestDto(val merchantCode: String)

/** Mirrors backend QrisScanResponse. expiresAt dibiarkan String (ISO-8601 dari Jackson) - cukup
 * buat ditampilkan, gak perlu di-parse jadi objek waktu di sisi Kotlin. */
@Serializable
data class QrisScanResponseDto(
    val token: String? = null,
    val merchantName: String? = null,
    val expiresAt: String? = null,
)

/** Mirrors backend QrisConfirmRequest - POST api/v1/nasabah/qris/transactions/{token}/confirm. */
@Serializable
data class QrisConfirmRequestDto(val amount: Double)

/** Mirrors backend QrisConfirmResponse. */
@Serializable
data class QrisConfirmResponseDto(
    val loanId: Int? = null,
    val merchantName: String? = null,
    val requestedAmount: Double? = null,
    val totalAmountDue: Double? = null,
    val installmentAmount: Double? = null,
    val dueDate: String? = null,
)
