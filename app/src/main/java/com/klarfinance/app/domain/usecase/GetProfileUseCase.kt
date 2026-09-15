package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.model.AccountProfile
import com.klarfinance.app.domain.model.Cached
import com.klarfinance.app.domain.repository.AuthRepository
import javax.inject.Inject

class GetProfileUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(): Result<Cached<AccountProfile>> = repository.getProfile()
}
