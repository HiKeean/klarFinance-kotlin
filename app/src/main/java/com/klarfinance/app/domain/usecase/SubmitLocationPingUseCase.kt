package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.repository.LocationTrackingRepository
import javax.inject.Inject

class SubmitLocationPingUseCase @Inject constructor(
    private val repository: LocationTrackingRepository,
) {
    suspend operator fun invoke(latitude: Double, longitude: Double, accuracyMeters: Double?): Result<Unit> =
        repository.submitPing(latitude, longitude, accuracyMeters)
}
