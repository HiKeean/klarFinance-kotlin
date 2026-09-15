package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.repository.AuthRepository
import javax.inject.Inject

/** Step-up auth generik dipakai TransactionAuthGate sebelum konfirmasi transaksi (pinjaman
 * tunai/QRIS) kalau nasabah belum aktifkan fingerprint - lihat AuthRepository.verifyPassword. */
class VerifyPasswordUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(password: String): Result<Unit> {
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("Masukkan password"))
        }
        return repository.verifyPassword(password)
    }
}
