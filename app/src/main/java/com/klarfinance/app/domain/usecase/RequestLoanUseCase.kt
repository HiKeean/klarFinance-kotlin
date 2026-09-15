package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.model.LoanRequestResult
import com.klarfinance.app.domain.repository.LoanRepository
import javax.inject.Inject

class RequestLoanUseCase @Inject constructor(
    private val repository: LoanRepository,
) {
    suspend operator fun invoke(
        amount: Long,
        tenorMonths: Int,
        bankAccountNumber: String,
        bankCode: String,
        pinjolApps: List<String> = emptyList(),
        bankApps: List<String> = emptyList(),
    ): Result<LoanRequestResult> {
        if (amount <= 0) return Result.failure(IllegalArgumentException("Nominal pinjaman harus lebih dari 0"))
        if (tenorMonths !in setOf(1, 3, 6, 9, 12)) return Result.failure(IllegalArgumentException("Tenor tidak valid"))
        if (bankAccountNumber.isBlank()) return Result.failure(IllegalArgumentException("Nomor rekening wajib diisi"))
        if (bankCode.isBlank()) return Result.failure(IllegalArgumentException("Bank tujuan wajib dipilih"))

        return repository.requestLoan(
            amount = amount,
            tenorMonths = tenorMonths,
            bankAccountNumber = bankAccountNumber.trim(),
            bankCode = bankCode,
            pinjolApps = pinjolApps,
            bankApps = bankApps,
        )
    }
}
