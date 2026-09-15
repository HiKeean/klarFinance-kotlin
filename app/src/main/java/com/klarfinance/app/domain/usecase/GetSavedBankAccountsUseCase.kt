package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.model.SavedBankAccount
import com.klarfinance.app.domain.repository.LoanRepository
import javax.inject.Inject

class GetSavedBankAccountsUseCase @Inject constructor(
    private val repository: LoanRepository,
) {
    suspend operator fun invoke(): Result<List<SavedBankAccount>> = repository.getSavedBankAccounts()
}
