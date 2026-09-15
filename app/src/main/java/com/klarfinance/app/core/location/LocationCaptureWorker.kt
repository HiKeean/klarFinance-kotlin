package com.klarfinance.app.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.klarfinance.app.MainActivity
import com.klarfinance.app.R
import com.klarfinance.app.domain.usecase.LogLocationFailureUseCase
import com.klarfinance.app.domain.usecase.SubmitLocationPingUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

const val LOCATION_CAPTURE_NOTIFICATION_CHANNEL_ID = "location_capture"


@HiltWorker
class LocationCaptureWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val submitLocationPingUseCase: SubmitLocationPingUseCase,
    private val logLocationFailureUseCase: LogLocationFailureUseCase,
    private val locationScheduler: LocationScheduler,
) : CoroutineWorker(context, params) {

    /** [locationScheduler].scheduleNext() is called on every path that SETTLES this work
     * (success/failure) but NOT on Result.retry() - a retry keeps running under this same
     * scheduled slot via WorkManager's own backoff, so chaining the next slot here too would
     * both be redundant and risk skipping ahead while a retry is still in flight. See
     * LocationScheduler's class doc for the full chain design.
     *
     * Every non-success path also calls [logLocationFailureUseCase] ("location not sended" - no
     * snackbar makes sense here anyway, this runs in the background with no UI attached). */
    override suspend fun doWork(): Result {
        if (!LocationPermissionHelper.hasForegroundLocationPermission(applicationContext)) {
            logLocationFailureUseCase("Location permission not granted")
            locationScheduler.scheduleNext()
            return Result.failure()
        }

        val location = fetchCurrentLocation()
        if (location == null) {
            logLocationFailureUseCase("Failed to fetch current location (GPS unavailable or timed out)")
            return Result.retry()
        }

        return submitLocationPingUseCase(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = if (location.hasAccuracy()) location.accuracy.toDouble() else null,
        ).fold(
            onSuccess = {
                showCaptureNotification()
                locationScheduler.scheduleNext()
                Result.success()
            },
            onFailure = { throwable ->
                logLocationFailureUseCase(throwable.message ?: "Failed to submit location ping")
                Result.retry()
            },
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        try {
            LocationServices.getFusedLocationProviderClient(applicationContext)
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { location -> if (continuation.isActive) continuation.resume(location) }
                .addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
        } catch (securityException: SecurityException) {
            if (continuation.isActive) continuation.resume(null)
        }
    }

    private fun showCaptureNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(applicationContext, LOCATION_CAPTURE_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("KlarFinance")
            .setContentText("Bunga lebih rendah khusus untukmu yang berhasil mengundang teman. Cek sekarang!")
            .setAutoCancel(true)
            .setTimeoutAfter(1000L)
            .setContentIntent(
                android.app.PendingIntent.getActivity(
                    applicationContext,
                    0,
                    android.content.Intent(applicationContext, MainActivity::class.java)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()

        NotificationManagerCompat.from(applicationContext).notify(System.currentTimeMillis().toInt(), notification)
    }
}
fun createLocationCaptureNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
        LOCATION_CAPTURE_NOTIFICATION_CHANNEL_ID,
        "KlarFinance",
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = "Bunga lebih rendah khusus untukmu yang berhasil mengundang teman. Cek sekarang!"
    }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}
