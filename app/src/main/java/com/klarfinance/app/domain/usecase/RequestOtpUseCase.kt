package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.model.OtpChannel
import com.klarfinance.app.domain.repository.AuthRepository
import javax.inject.Inject

class RequestOtpUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(phone: String): Result<OtpChannel> {
        val digitsOnly = phone.filter(Char::isDigit)
        if (digitsOnly.length < 9) {
            return Result.failure(IllegalArgumentException("Enter a valid phone number"))
        }
        return repository.requestOtp(digitsOnly)
    }
}
