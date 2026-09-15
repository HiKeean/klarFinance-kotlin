package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.model.Cached
import com.klarfinance.app.domain.model.ReferralSummary
import com.klarfinance.app.domain.repository.ReferralRepository
import javax.inject.Inject

class GetReferralSummaryUseCase @Inject constructor(
    private val repository: ReferralRepository,
) {
    suspend operator fun invoke(): Result<Cached<ReferralSummary>> = repository.getSummary()
}
