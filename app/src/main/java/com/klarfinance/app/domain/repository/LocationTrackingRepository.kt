package com.klarfinance.app.domain.repository

interface LocationTrackingRepository {
    /** GET api/v1/nasabah/location/consent - whether this nasabah has opted into periodic
     * location capture (see LocationCaptureWorker). */
    suspend fun getConsent(): Result<Boolean>

    /** PATCH api/v1/nasabah/location/consent - the single source of truth for whether pings are
     * even accepted (see backend LocationTrackingService#submitPing, which re-checks this itself
     * rather than trusting the client). */
    suspend fun setConsent(consent: Boolean): Result<Unit>

    /** POST api/v1/nasabah/location/pings - fails (Result.failure) if consent isn't currently on
     * record server-side, regardless of what LocationScheduler last scheduled locally. */
    suspend fun submitPing(latitude: Double, longitude: Double, accuracyMeters: Double?): Result<Unit>

    /** POST api/v1/nasabah/location/failure-log - "location not sended" telemetry, called
     * instead of showing a snackbar whenever location couldn't be turned on/captured (user's
     * explicit call 2026-09-04). Best-effort on the server (see backend
     * LocationFailureLogService) - never blocks/fails the caller's actual flow either way. */
    suspend fun logFailure(reason: String): Result<Unit>
}
