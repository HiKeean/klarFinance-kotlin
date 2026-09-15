package com.klarfinance.app.presentation.transjakarta

import com.klarfinance.app.domain.model.TransjakartaTicket

/** Tarif flat Transjakarta - preview klien doang, backend (TransjakartaTicketService.TICKET_PRICE)
 * tetap source of truth. Berlaku buat rute standar - TIDAK berlaku buat rute jauh (mis.
 * Bogor-PIK2, Blok M-PIK2), lihat disclaimer di TransjakartaHomeScreen. */
private const val TICKET_PRICE = 3500L
const val MAX_TICKET_QTY = 10

/** Preview bunga di klien - sama pola kayak QrisUiState, backend (LoanInterestPolicy) tetap
 * source of truth beneran. */
private const val MONTHLY_RATE_PERCENT = 3.5

/** Shared graph-scoped state - Home (qty+riwayat) dan Confirm (bayar) berbagi satu ViewModel yang
 * sama (lihat TransjakartaPurchaseViewModel + transjakartaGraph di KlarNavHost), mirror pola
 * RequestLoanViewModel. */
data class TransjakartaPurchaseUiState(
    // Beli tiket
    val qty: Int = 1,

    // Riwayat (used/belum, tap buat toggle - mockup, konfirmasi user)
    val tickets: List<TransjakartaTicket> = emptyList(),
    val isLoadingTickets: Boolean = true,
    val ticketsErrorMessage: String? = null,
    /** True when [tickets] is the last cached list (Room), shown because the network was
     * unreachable - see [com.klarfinance.app.domain.model.Cached]. QR still renders fine
     * offline (QrCodeImage generates purely from ticketCode, no network call). */
    val isOffline: Boolean = false,

    // Konfirmasi bayar
    val isSubmitting: Boolean = false,
    val submitErrorMessage: String? = null,

    /** Step-up auth (sama pola RequestLoanUiState/QrisUiState). */
    val requiresPasswordConfirm: Boolean = false,
    val passwordInput: String = "",
    val isVerifyingPassword: Boolean = false,
    val passwordError: String? = null,

    val purchasedTickets: List<TransjakartaTicket>? = null,
) {
    val ticketPrice: Long get() = TICKET_PRICE
    val totalPrincipal: Long get() = TICKET_PRICE * qty
    val interestFee: Long get() = (totalPrincipal * MONTHLY_RATE_PERCENT / 100).toLong()
    val totalToPay: Long get() = totalPrincipal + interestFee
}
