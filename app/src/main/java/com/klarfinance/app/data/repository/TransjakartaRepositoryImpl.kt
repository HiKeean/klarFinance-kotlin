package com.klarfinance.app.data.repository

import com.klarfinance.app.core.network.ApiService
import com.klarfinance.app.core.network.NetworkUnavailableException
import com.klarfinance.app.data.dto.PurchaseTicketRequestDto
import com.klarfinance.app.data.dto.TicketDto
import com.klarfinance.app.data.local.TransjakartaTicketDao
import com.klarfinance.app.data.local.TransjakartaTicketEntity
import com.klarfinance.app.domain.model.Cached
import com.klarfinance.app.domain.model.TransjakartaTicket
import com.klarfinance.app.domain.repository.TransjakartaRepository
import javax.inject.Inject

class TransjakartaRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val transjakartaTicketDao: TransjakartaTicketDao,
) : TransjakartaRepository {

    override suspend fun purchaseTickets(qty: Int): Result<List<TransjakartaTicket>> = runCatching {
        val response = apiService.post<List<TicketDto>, PurchaseTicketRequestDto>(
            "api/v1/nasabah/transjakarta/tickets",
            PurchaseTicketRequestDto(qty = qty),
        )
        val tickets = (response.data ?: emptyList()).mapNotNull(::toTicket)
        transjakartaTicketDao.upsertAll(tickets.map(::toEntity))
        tickets
    }

    override suspend fun myTickets(): Result<Cached<List<TransjakartaTicket>>> {
        val networkResult = runCatching {
            val response = apiService.get<List<TicketDto>>("api/v1/nasabah/transjakarta/tickets")
            (response.data ?: emptyList()).mapNotNull(::toTicket)
        }
        networkResult.onSuccess { tickets ->
            transjakartaTicketDao.replaceAll(tickets.map(::toEntity))
            return Result.success(Cached(tickets, isFromCache = false))
        }

        val error = networkResult.exceptionOrNull()
        if (error is NetworkUnavailableException) {
            val cached = transjakartaTicketDao.getAll()
            if (cached.isNotEmpty()) {
                return Result.success(Cached(cached.map(::fromEntity), isFromCache = true))
            }
        }
        return Result.failure(error ?: IllegalStateException("Unknown error"))
    }

    /** Toggling requires connectivity (no offline-first fallback here, same reasoning as
     * LoanRepositoryImpl.repay), but writes the flipped ticket back into the cache so it isn't
     * immediately stale if the device goes offline right after. */
    override suspend fun toggleUsed(ticketCode: String): Result<TransjakartaTicket> = runCatching {
        val response = apiService.post<TicketDto>("api/v1/nasabah/transjakarta/tickets/$ticketCode/toggle-used")
        val ticket = toTicket(response.data) ?: throw IllegalStateException("Unexpected response from server")
        transjakartaTicketDao.upsertOne(toEntity(ticket))
        ticket
    }

    private fun toTicket(dto: TicketDto?): TransjakartaTicket? {
        if (dto == null) return null
        return TransjakartaTicket(
            ticketId = dto.ticketId ?: return null,
            ticketCode = dto.ticketCode ?: return null,
            amount = (dto.amount ?: 0.0).toLong(),
            used = dto.used ?: false,
            purchasedAt = dto.purchasedAt ?: "",
            loanId = dto.loanId ?: return null,
            billingCycle = dto.billingCycle ?: "",
            dueDate = dto.dueDate ?: "",
        )
    }

    private fun toEntity(ticket: TransjakartaTicket): TransjakartaTicketEntity = TransjakartaTicketEntity(
        ticketId = ticket.ticketId,
        ticketCode = ticket.ticketCode,
        amount = ticket.amount,
        used = ticket.used,
        purchasedAt = ticket.purchasedAt,
        loanId = ticket.loanId,
        billingCycle = ticket.billingCycle,
        dueDate = ticket.dueDate,
        cachedAtMillis = System.currentTimeMillis(),
    )

    private fun fromEntity(entity: TransjakartaTicketEntity): TransjakartaTicket = TransjakartaTicket(
        ticketId = entity.ticketId,
        ticketCode = entity.ticketCode,
        amount = entity.amount,
        used = entity.used,
        purchasedAt = entity.purchasedAt,
        loanId = entity.loanId,
        billingCycle = entity.billingCycle,
        dueDate = entity.dueDate,
    )
}
