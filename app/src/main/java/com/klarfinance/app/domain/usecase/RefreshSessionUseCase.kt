package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.model.AccountState
import com.klarfinance.app.domain.repository.AuthRepository
import javax.inject.Inject

class RefreshSessionUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(refreshToken: String): Result<AccountState> = repository.refreshSession(refreshToken)
}
