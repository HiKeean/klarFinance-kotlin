package com.klarfinance.app.data.repository

import com.klarfinance.app.core.network.ApiService
import com.klarfinance.app.data.dto.LocationConsentRequestDto
import com.klarfinance.app.data.dto.LocationConsentResponseDto
import com.klarfinance.app.data.dto.LocationFailureLogRequestDto
import com.klarfinance.app.data.dto.LocationPingRequestDto
import com.klarfinance.app.domain.repository.LocationTrackingRepository
import java.time.LocalDateTime
import javax.inject.Inject

class LocationTrackingRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
) : LocationTrackingRepository {

    override suspend fun getConsent(): Result<Boolean> = runCatching {
        apiService.get<LocationConsentResponseDto>("api/v1/nasabah/location/consent").data?.consentGiven ?: false
    }

    override suspend fun setConsent(consent: Boolean): Result<Unit> = runCatching {
        apiService.patch<Unit, LocationConsentRequestDto>(
            "api/v1/nasabah/location/consent",
            LocationConsentRequestDto(consent),
        )
        Unit
    }

    override suspend fun submitPing(latitude: Double, longitude: Double, accuracyMeters: Double?): Result<Unit> =
        runCatching {
            apiService.post<Unit, LocationPingRequestDto>(
                "api/v1/nasabah/location/pings",
                LocationPingRequestDto(
                    latitude = latitude,
                    longitude = longitude,
                    accuracyMeters = accuracyMeters,
                    capturedAt = LocalDateTime.now().toString(),
                ),
            )
            Unit
        }

    override suspend fun logFailure(reason: String): Result<Unit> = runCatching {
        apiService.post<Unit, LocationFailureLogRequestDto>(
            "api/v1/nasabah/location/failure-log",
            LocationFailureLogRequestDto(reason),
        )
        Unit
    }
}
