package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.repository.LocationTrackingRepository
import javax.inject.Inject

class LogLocationFailureUseCase @Inject constructor(
    private val repository: LocationTrackingRepository,
) {
    suspend operator fun invoke(reason: String): Result<Unit> = repository.logFailure(reason)
}
