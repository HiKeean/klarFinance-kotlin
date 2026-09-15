package com.klarfinance.app.domain.model

data class TransjakartaTicket(
    val ticketId: Int,
    /** Isi QR mockup yang ditampilkan ke nasabah - lihat presentation/transjakarta. */
    val ticketCode: String,
    val amount: Long,
    /** Mockup - ditandai manual sama nasabah sendiri lewat tap (toggle), gak ada validasi gate
     * beneran. */
    val used: Boolean,
    val purchasedAt: String,
    val loanId: Int,
    val billingCycle: String,
    val dueDate: String,
)
