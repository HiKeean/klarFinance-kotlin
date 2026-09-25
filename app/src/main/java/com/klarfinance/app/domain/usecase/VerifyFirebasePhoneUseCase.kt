package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.repository.AuthRepository
import javax.inject.Inject

class VerifyFirebasePhoneUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(phone: String, idToken: String): Result<Unit> =
        repository.verifyFirebasePhone(phone.filter(Char::isDigit), idToken)
}
