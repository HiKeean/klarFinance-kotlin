package com.klarfinance.app.domain.model

/** Hasil scan QR toko (POST /nasabah/qris/transactions/scan) - [token] dibawa ke langkah
 * berikutnya (QrisAmountScreen) buat confirm. */
data class QrisTransactionInfo(
    val token: String,
    val merchantName: String,
)
