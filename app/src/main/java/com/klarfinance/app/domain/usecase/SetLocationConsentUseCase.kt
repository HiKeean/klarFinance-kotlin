package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.repository.LocationTrackingRepository
import javax.inject.Inject

class SetLocationConsentUseCase @Inject constructor(
    private val repository: LocationTrackingRepository,
) {
    suspend operator fun invoke(consent: Boolean): Result<Unit> = repository.setConsent(consent)
}
