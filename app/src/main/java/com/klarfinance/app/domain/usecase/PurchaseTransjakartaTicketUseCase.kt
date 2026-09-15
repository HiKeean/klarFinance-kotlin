package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.model.TransjakartaTicket
import com.klarfinance.app.domain.repository.TransjakartaRepository
import javax.inject.Inject

class PurchaseTransjakartaTicketUseCase @Inject constructor(
    private val repository: TransjakartaRepository,
) {
    suspend operator fun invoke(qty: Int): Result<List<TransjakartaTicket>> {
        if (qty <= 0) return Result.failure(IllegalArgumentException("Jumlah tiket harus minimal 1"))
        return repository.purchaseTickets(qty)
    }
}
