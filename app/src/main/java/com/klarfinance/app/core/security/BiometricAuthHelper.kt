package com.klarfinance.app.core.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Thin coroutine wrapper around androidx.biometric - used to gate saving/using the refresh
 * token for fingerprint-based session restore (see kotlin-nasabah-app knowledge). Requires a
 * [FragmentActivity] (not a plain ComponentActivity), see [com.klarfinance.app.MainActivity]. */
object BiometricAuthHelper {

    fun isAvailable(activity: FragmentActivity): Boolean {
        val manager = BiometricManager.from(activity)
        return manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    suspend fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
    ): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (continuation.isActive) continuation.resume(Result.success(Unit))
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (continuation.isActive) continuation.resume(Result.failure(IllegalStateException(errString.toString())))
            }

            // Deliberately not resumed - a failed single attempt (wrong finger) should let the
            // user retry within the same prompt, not immediately fail the whole operation.
            override fun onAuthenticationFailed() = Unit
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Batal")
            .build()
        prompt.authenticate(promptInfo)
    }
}
