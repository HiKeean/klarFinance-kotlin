package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.model.LoanHistoryItem
import com.klarfinance.app.domain.repository.LoanRepository
import javax.inject.Inject

class RepayLoanUseCase @Inject constructor(
    private val repository: LoanRepository,
) {
    suspend operator fun invoke(loanId: Int, amount: Long): Result<LoanHistoryItem> {
        if (amount <= 0) return Result.failure(IllegalArgumentException("Nominal pembayaran harus lebih dari 0"))
        return repository.repay(loanId, amount)
    }
}
