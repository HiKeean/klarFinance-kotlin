package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.model.QrisConfirmResult
import com.klarfinance.app.domain.repository.QrisRepository
import javax.inject.Inject

class ConfirmQrisUseCase @Inject constructor(
    private val repository: QrisRepository,
) {
    suspend operator fun invoke(token: String, amount: Long): Result<QrisConfirmResult> {
        if (amount <= 0) return Result.failure(IllegalArgumentException("Nominal harus lebih dari 0"))
        return repository.confirm(token, amount)
    }
}
