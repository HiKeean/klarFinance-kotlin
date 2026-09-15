package com.klarfinance.app.data.dto

import kotlinx.serialization.Serializable

/** Mirrors backend LocationConsentResponse (geo/dto/response). */
@Serializable
data class LocationConsentResponseDto(
    val consentGiven: Boolean = false,
)

/** Mirrors backend LocationConsentRequest (geo/dto/request). */
@Serializable
data class LocationConsentRequestDto(
    val consent: Boolean,
)

/** Mirrors backend LocationPingRequest (geo/dto/request). capturedAt is ISO-8601
 * ("yyyy-MM-dd'T'HH:mm:ss") - matches how kotlinx.serialization encodes a plain string, decoded
 * server-side into LocalDateTime by Jackson's default ISO parsing. */
@Serializable
data class LocationPingRequestDto(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double? = null,
    val capturedAt: String,
)

/** Mirrors backend LocationFailureLogRequest (geo/dto/request) - POST
 * api/v1/nasabah/location/failure-log. Server-side writes this to a dedicated Redis (1-day TTL,
 * see backend LocationFailureLogService) instead of the client showing a snackbar - see
 * AccountViewModel.onLocationPermissionDenied/onLocationConsentEnabled and
 * LocationCaptureWorker. */
@Serializable
data class LocationFailureLogRequestDto(
    val reason: String,
)
