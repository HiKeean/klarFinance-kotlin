package com.klarfinance.app.core.auth

import android.app.Activity
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val SMS_TIMEOUT_SECONDS = 60L

class FirebaseSmsOtpSender @Inject constructor() : SmsOtpSender {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var autoCredential: PhoneAuthCredential? = null

    override fun send(activity: Activity, phone: String, onEvent: (SmsOtpEvent) -> Unit) {
        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+" + phone.filter(Char::isDigit))
            .setTimeout(SMS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    autoCredential = credential
                    onEvent(SmsOtpEvent.AutoRetrieved(credential.smsCode))
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    onEvent(SmsOtpEvent.Failed(sendErrorMessage(e)))
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id
                    resendToken = token
                    onEvent(SmsOtpEvent.CodeSent)
                }
            })
        resendToken?.let(builder::setForceResendingToken)
        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    override suspend fun confirm(code: String?): Result<String> = runCatching {
        val credential = if (code == null) {
            autoCredential ?: throw IllegalStateException("Enter the 6-digit code")
        } else {
            val id = verificationId ?: throw IllegalStateException("SMS code has not been sent yet")
            PhoneAuthProvider.getCredential(id, code)
        }
        try {
            val user = auth.signInWithCredential(credential).awaitResult().user
                ?: throw IllegalStateException("SMS verification failed")
            user.getIdToken(true).awaitResult().token
                ?: throw IllegalStateException("SMS verification failed")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            throw IllegalArgumentException("Invalid code", e)
        } finally {
            // Sesi Firebase cuma dipakai buat dapat ID token sekali - login app tetap lewat backend.
            auth.signOut()
        }
    }

    private fun sendErrorMessage(e: FirebaseException): String = when (e) {
        is FirebaseTooManyRequestsException -> "Too many SMS requests. Please try again later"
        is FirebaseAuthInvalidCredentialsException -> "Invalid phone number"
        else -> "Failed to send SMS code"
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        val exception = task.exception
        if (exception != null) continuation.resumeWithException(exception) else continuation.resume(task.result)
    }
}
