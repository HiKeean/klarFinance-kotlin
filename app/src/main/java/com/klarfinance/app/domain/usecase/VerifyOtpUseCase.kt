package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.repository.AuthRepository
import javax.inject.Inject

class VerifyOtpUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(phone: String, otp: String): Result<Unit> {
        if (otp.length != 6 || !otp.all(Char::isDigit)) {
            return Result.failure(IllegalArgumentException("Enter the 6-digit code"))
        }
        return repository.verifyOtp(phone.filter(Char::isDigit), otp)
    }
}
