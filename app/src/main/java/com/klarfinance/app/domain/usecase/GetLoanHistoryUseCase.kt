package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.model.Cached
import com.klarfinance.app.domain.model.LoanHistoryItem
import com.klarfinance.app.domain.repository.LoanRepository
import javax.inject.Inject

class GetLoanHistoryUseCase @Inject constructor(
    private val repository: LoanRepository,
) {
    suspend operator fun invoke(): Result<Cached<List<LoanHistoryItem>>> = repository.getLoanHistory()
}
