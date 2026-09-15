package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.model.LimitSummary
import com.klarfinance.app.domain.repository.LoanRepository
import javax.inject.Inject

class GetLimitSummaryUseCase @Inject constructor(
    private val repository: LoanRepository,
) {
    suspend operator fun invoke(): Result<LimitSummary> = repository.getLimitSummary()
}
