package com.klarfinance.app.data.dto

import kotlinx.serialization.Serializable

/** Mirrors backend PurchaseTicketRequest - POST api/v1/nasabah/transjakarta/tickets. */
@Serializable
data class PurchaseTicketRequestDto(val qty: Int)

/** Mirrors backend TicketResponse. purchasedAt/dueDate dibiarkan String (ISO-8601 dari Jackson,
 * sama pola kayak QrisScanResponseDto.expiresAt) - cukup buat ditampilkan. */
@Serializable
data class TicketDto(
    val ticketId: Int? = null,
    val ticketCode: String? = null,
    val amount: Double? = null,
    val used: Boolean? = null,
    val purchasedAt: String? = null,
    val loanId: Int? = null,
    val billingCycle: String? = null,
    val dueDate: String? = null,
)
