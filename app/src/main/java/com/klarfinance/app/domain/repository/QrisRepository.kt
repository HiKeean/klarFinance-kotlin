package com.klarfinance.app.domain.repository

import com.klarfinance.app.domain.model.QrisConfirmResult
import com.klarfinance.app.domain.model.QrisTransactionInfo

interface QrisRepository {
    /** POST api/v1/nasabah/qris/transactions/scan - merchantCode dari hasil decode QR. */
    suspend fun scan(merchantCode: String): Result<QrisTransactionInfo>

    /** POST api/v1/nasabah/qris/transactions/{token}/confirm. */
    suspend fun confirm(token: String, amount: Long): Result<QrisConfirmResult>
}
