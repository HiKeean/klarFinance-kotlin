package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.model.QrisTransactionInfo
import com.klarfinance.app.domain.repository.QrisRepository
import javax.inject.Inject

class ScanQrisUseCase @Inject constructor(
    private val repository: QrisRepository,
) {
    suspend operator fun invoke(merchantCode: String): Result<QrisTransactionInfo> {
        if (merchantCode.isBlank()) return Result.failure(IllegalArgumentException("QR tidak valid"))
        return repository.scan(merchantCode.trim())
    }
}
