package com.klarfinance.app.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class RequestOtpRequestDto(
    val phone: String,
)

@Serializable
data class VerifyOtpRequestDto(
    val phone: String,
    val otp: String,
)

@Serializable
data class RegisterResponseDataDto(
    val identity: String? = null,
    val applicationStatus: String? = null,
    val suggestedLimit: Double? = null,
    val message: String? = null,
)

@Serializable
data class LoginRequestDto(
    val identity: String,
    val password: String,
)

@Serializable
data class LoginResponseDataDto(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val userProfile: UserProfileDto? = null,
    /** Nasabah-only: "ACTIVE" or "PENDING_APPLICATION" - null for non-nasabah roles. */
    val accountStatus: String? = null,
)

@Serializable
data class UserProfileDto(
    val name: String? = null,
    val identity: String? = null,
    val role: String? = null,
)

/** Mirrors backend `ProfileResponse` - `dob` comes back as an ISO "yyyy-MM-dd" string
 * (Jackson's default `LocalDate` serialization), kept as a plain String here since the
 * Account screen only displays it, no date math needed client-side. */
@Serializable
data class ProfileResponseDto(
    val name: String? = null,
    val dob: String? = null,
    val noHp: String? = null,
    val role: String? = null,
    val email: String? = null,
    val emailVerified: Boolean = false,
    /** Nasabah-only - "ACTIVE"/"PENDING_APPLICATION", null for staff. Same shape as
     * LoginResponseDataDto.accountStatus - lets AccountState be re-derived on demand
     * (see AccountProfile.accountState / AuthRepositoryImpl.getProfile). */
    val accountStatus: String? = null,
)

@Serializable
data class ChangePasswordRequestDto(
    val oldPassword: String,
    val newPassword: String,
)

@Serializable
data class RefreshTokenRequestDto(
    val refreshToken: String,
)

@Serializable
data class FcmTokenRequestDto(
    val fcmToken: String,
)

/** Step-up auth generik sebelum konfirmasi transaksi (pinjaman/QRIS) kalau nasabah belum
 * aktifkan fingerprint - lihat domain.usecase.VerifyPasswordUseCase. */
@Serializable
data class VerifyPasswordRequestDto(
    val password: String,
)
