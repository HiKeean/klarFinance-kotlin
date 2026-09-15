package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.model.TransjakartaTicket
import com.klarfinance.app.domain.repository.TransjakartaRepository
import javax.inject.Inject

class ToggleTransjakartaTicketUsedUseCase @Inject constructor(
    private val repository: TransjakartaRepository,
) {
    suspend operator fun invoke(ticketCode: String): Result<TransjakartaTicket> = repository.toggleUsed(ticketCode)
}
