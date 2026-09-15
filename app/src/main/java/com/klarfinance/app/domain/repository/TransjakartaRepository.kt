package com.klarfinance.app.domain.repository

import com.klarfinance.app.domain.model.Cached
import com.klarfinance.app.domain.model.TransjakartaTicket

interface TransjakartaRepository {
    /** POST api/v1/nasabah/transjakarta/tickets - beli qty tiket sekaligus, balik semua tiket
     * yang baru dibuat. */
    suspend fun purchaseTickets(qty: Int): Result<List<TransjakartaTicket>>

    /** GET api/v1/nasabah/transjakarta/tickets - rincian semua tiket yang pernah dibeli.
     * Network-first, falls back to the last cached list (Room) if the device is offline - the
     * QR itself renders purely from [TransjakartaTicket.ticketCode] with no network call (see
     * QrCodeImage), so a cached ticket list is enough to show a previously-bought ticket's QR
     * without connectivity. See [Cached.isFromCache]. */
    suspend fun myTickets(): Result<Cached<List<TransjakartaTicket>>>

    /** POST api/v1/nasabah/transjakarta/tickets/{ticketCode}/toggle-used - mockup, ditandai
     * manual sama nasabah. */
    suspend fun toggleUsed(ticketCode: String): Result<TransjakartaTicket>
}
