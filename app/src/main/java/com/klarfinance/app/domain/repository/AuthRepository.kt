package com.klarfinance.app.domain.repository

import com.klarfinance.app.domain.model.AccountProfile
import com.klarfinance.app.domain.model.AccountState
import com.klarfinance.app.domain.model.Cached
import com.klarfinance.app.domain.model.LoginResult
import com.klarfinance.app.domain.model.OtpChannel
import com.klarfinance.app.domain.model.RegisterResult
import okhttp3.MultipartBody

interface AuthRepository {
    suspend fun requestOtp(phone: String): Result<OtpChannel>
    suspend fun verifyOtp(phone: String, otp: String): Result<Unit>
    suspend fun verifyFirebasePhone(phone: String, idToken: String): Result<Unit>
    suspend fun isPhoneRegistered(phone: String): Result<Boolean>
    suspend fun login(identity: String, password: String): Result<LoginResult>

    /** Network-first, falls back to the last cached profile (Room) if the device is offline -
     * see [Cached.isFromCache]. */
    suspend fun getProfile(): Result<Cached<AccountProfile>>
    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit>

    /** Step-up auth generik sebelum konfirmasi transaksi (pinjaman tunai/QRIS) kalau nasabah
     * belum aktifkan fingerprint (konfirmasi user 2026-09-07) - lihat VerifyPasswordUseCase. */
    suspend fun verifyPassword(password: String): Result<Unit>

    suspend fun logout(): Result<Unit>

    /** Redeems a stored refresh token for a fresh session - used by the fingerprint-gated
     * cold-start restore (see [com.klarfinance.app.core.session.SecureTokenStore]). */
    suspend fun refreshSession(refreshToken: String): Result<AccountState>

    /** Registers/updates the FCM token for push notification (approve/reject) - server no-ops
     * for non-nasabah roles, see backend AuthenticationInternalService#updateFcmToken. Called
     * after login/register (see AuthRepositoryImpl.login) and from
     * [com.klarfinance.app.core.notification.KlarFirebaseMessagingService.onNewToken]. */
    suspend fun updateFcmToken(token: String): Result<Unit>

    suspend fun register(
        phone: String,
        nik: String,
        name: String,
        email: String,
        address: String,
        dob: String,
        villageId: Long,
        password: String,
        referralCode: String = "",
        fotoKtp: MultipartBody.Part,
        fotoKyc: MultipartBody.Part,
        pinjolApps: List<String> = emptyList(),
        bankApps: List<String> = emptyList(),
    ): Result<RegisterResult>
}
