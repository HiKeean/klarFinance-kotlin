package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.model.LoginResult
import com.klarfinance.app.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(identity: String, password: String): Result<LoginResult> {
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter your password"))
        }
        return repository.login(identity.filter(Char::isDigit), password)
    }
}
