package com.klarfinance.app.core.location

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val LOCATION_CAPTURE_WORK_NAME = "location_capture_scheduled"

/** Fixed daily capture times (confirmed by user 2026-09-04) - device local time/timezone, not
 * UTC/server time. */
private val DAILY_CAPTURE_TIMES = listOf(LocalTime.of(2, 0), LocalTime.of(10, 0), LocalTime.of(15, 0), LocalTime.of(17, 0))

/**
 * Enqueues/cancels [LocationCaptureWorker] at fixed clock times (02:00/10:00/15:00) instead of a
 * rolling interval from whenever the toggle was flipped (the previous approach - see git history).
 *
 * WorkManager has no native "run at these exact clock times daily" primitive, so this is built as
 * a SELF-RESCHEDULING CHAIN of [androidx.work.OneTimeWorkRequest]s: [schedule] enqueues ONE run
 * for the nearest upcoming slot, and [LocationCaptureWorker] itself calls [scheduleNext] right
 * before it settles (Result.success()/Result.failure() - deliberately NOT on Result.retry(),
 * which WorkManager already retries under the SAME unique work name via backoff) to enqueue the
 * run for the next slot after that. If a slot's work keeps retrying (e.g. no network for hours),
 * the chain just "catches up" whenever it finally settles rather than losing the schedule.
 *
 * Deliberately NOT using AlarmManager.setExact*() for this - that needs SCHEDULE_EXACT_ALARM
 * (Android 12+ requires user-granted "Alarms & reminders" permission on API 33+, and Play Store
 * restricts this permission to alarm-clock/calendar-style apps, which this isn't) plus a
 * BOOT_COMPLETED receiver + a way to re-check consent synchronously after reboot to re-arm it.
 * WorkManager already persists its own queue across reboot for free and needs no extra
 * permission - the trade-off is the same as before (not exact-to-the-second, subject to
 * Doze/battery deferral), but it no longer drifts based on when the toggle was flipped.
 */
@Singleton
class LocationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun schedule() = enqueueNextSlot()

    /** Called by [LocationCaptureWorker] after it settles (success or hard failure, not retry) to
     * chain the next scheduled slot - see class doc. */
    fun scheduleNext() = enqueueNextSlot()

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(LOCATION_CAPTURE_WORK_NAME)
    }

    private fun enqueueNextSlot() {
        val request = OneTimeWorkRequestBuilder<LocationCaptureWorker>()
            .setInitialDelay(millisUntilNextSlot(), TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            LOCATION_CAPTURE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun millisUntilNextSlot(): Long {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val nextSlot = (DAILY_CAPTURE_TIMES.map { LocalDateTime.of(today, it) } +
            DAILY_CAPTURE_TIMES.map { LocalDateTime.of(today.plusDays(1), it) })
            .first { it.isAfter(now) }
        return Duration.between(now, nextSlot).toMillis()
    }
}
