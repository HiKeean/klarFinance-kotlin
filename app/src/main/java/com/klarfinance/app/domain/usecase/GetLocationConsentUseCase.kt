package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.repository.LocationTrackingRepository
import javax.inject.Inject

class GetLocationConsentUseCase @Inject constructor(
    private val repository: LocationTrackingRepository,
) {
    suspend operator fun invoke(): Result<Boolean> = repository.getConsent()
}
