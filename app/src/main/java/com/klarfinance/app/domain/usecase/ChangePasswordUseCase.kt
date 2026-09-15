package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.repository.AuthRepository
import javax.inject.Inject

class ChangePasswordUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(oldPassword: String, newPassword: String): Result<Unit> {
        if (oldPassword.isBlank()) {
            return Result.failure(IllegalArgumentException("Masukkan password lama"))
        }
        if (newPassword.length < 8) {
            return Result.failure(IllegalArgumentException("Password baru minimal 8 karakter"))
        }
        return repository.changePassword(oldPassword, newPassword)
    }
}
