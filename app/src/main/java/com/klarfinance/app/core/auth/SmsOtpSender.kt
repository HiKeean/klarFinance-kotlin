package com.klarfinance.app.core.auth

import android.app.Activity

sealed interface SmsOtpEvent {
    data object CodeSent : SmsOtpEvent
    /** Android membaca SMS-nya otomatis (atau verifikasi instan) - [code] bisa null untuk instan. */
    data class AutoRetrieved(val code: String?) : SmsOtpEvent
    data class Failed(val message: String) : SmsOtpEvent
}

/**
 * OTP via SMS (Firebase Phone Auth) - fallback kalau WhatsApp gagal/kena banned. Hasil akhirnya
 * Firebase ID token yang diverifikasi backend di `auth/verify-firebase-phone`, bukan kode OTP.
 * Stateful (menyimpan verificationId per alur) - jangan di-scope Singleton.
 */
interface SmsOtpSender {
    /** [activity] cuma dipakai selama panggilan ini (reCAPTCHA/Play Integrity), tidak disimpan. */
    fun send(activity: Activity, phone: String, onEvent: (SmsOtpEvent) -> Unit)

    /** Tukar [code] (atau credential auto-retrieve kalau null) jadi Firebase ID token. */
    suspend fun confirm(code: String?): Result<String>
}
