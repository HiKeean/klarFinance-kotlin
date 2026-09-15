package com.klarfinance.app.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Plain permission-state checks for the location-routine feature (see LocationCaptureWorker) -
 * the actual runtime request flow (which needs an Activity/ActivityResultLauncher) lives in
 * AccountScreen, right next to the consent toggle that triggers it. */
object LocationPermissionHelper {

    fun hasForegroundLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** ACCESS_BACKGROUND_LOCATION only exists as a concept from API 29 (Q) onward - below that,
     * foreground location permission is already usable in the background, so this is trivially
     * true there (nothing extra to request/check). */
    fun hasBackgroundLocationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }
}
