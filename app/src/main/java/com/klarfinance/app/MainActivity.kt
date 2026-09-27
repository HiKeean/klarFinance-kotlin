package com.klarfinance.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.klarfinance.app.core.navigation.KlarNavHost
import com.klarfinance.app.core.security.RootDetector
import com.klarfinance.app.core.security.RootedDeviceDialog
import com.klarfinance.app.core.theme.KlarFinanceTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** FragmentActivity (not plain ComponentActivity) because BiometricPrompt requires one - see
 * core/security/BiometricAuthHelper.kt, used for the fingerprint-gated session restore. */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var rootDetector: RootDetector

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way - push
            notification (FCM) is a bonus feature, app works fine without it if user denies. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            KlarFinanceTheme {
                // null = masih dicek (sepersekian detik) - NavHost belum dirender biar session
                // restore/request API gak sempat jalan di perangkat root.
                val rooted by produceState<Boolean?>(initialValue = null) { value = rootDetector.isRooted() }
                var debugBypass by remember { mutableStateOf(false) }
                when {
                    rooted == null -> Unit
                    rooted == true && !BuildConfig.DEBUG -> RootedDeviceDialog(onExit = ::finishAffinity, onContinue = null)
                    else -> {
                        KlarNavHost()
                        if (rooted == true && !debugBypass) {
                            RootedDeviceDialog(onExit = ::finishAffinity, onContinue = { debugBypass = true })
                        }
                    }
                }
            }
        }
    }

    /** POST_NOTIFICATIONS is a runtime (dangerous) permission since Android 13 (API 33) - the
     * manifest declaration alone doesn't grant it, unlike pre-33 where notifications just worked.
     * Below 33 this is a no-op (permission doesn't exist as a concept there). */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
