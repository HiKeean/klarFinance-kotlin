package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.repository.AuthRepository
import javax.inject.Inject

class CheckPhoneRegisteredUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(phone: String): Result<Boolean> =
        repository.isPhoneRegistered(phone.filter(Char::isDigit))
}
