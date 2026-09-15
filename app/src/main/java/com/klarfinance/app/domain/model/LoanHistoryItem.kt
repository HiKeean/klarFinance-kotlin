package com.klarfinance.app.domain.model

/** One row of the "History" page - covers tarik tunai (LOAN), bayar QRIS (QRIS_PAYMENT), DAN
 * tagihan Transjakarta (TRANSJAKARTA_BILL), since backend stores all three as the same Loan
 * entity, only distinguished by Drawdown.channel (see LoanService#toHistoryItem). Client-side
 * gap fixed 2026-09-14 - backend already emitted "TRANSJAKARTA_BILL" but this enum didn't know
 * about it yet, so every Transjakarta bill was silently falling into the LOAN fallback and
 * showing up as "Pinjaman Tunai" in History/Bills. */
enum class LoanHistoryType {
    LOAN,
    QRIS_PAYMENT,
    TRANSJAKARTA_BILL,
    ;

    companion object {
        fun fromBackend(value: String?): LoanHistoryType =
            entries.find { it.name == value } ?: LOAN
    }
}

enum class LoanHistoryStatus {
    ACTIVE,
    OVERDUE,
    PAID_OFF,
    ;

    companion object {
        fun fromBackend(value: String?): LoanHistoryStatus =
            entries.find { it.name == value } ?: ACTIVE
    }
}

data class LoanHistoryItem(
    val loanId: Int,
    val type: LoanHistoryType,
    /** Terisi cuma kalau [type]=QRIS_PAYMENT. */
    val merchantName: String?,
    val requestedAmount: Long,
    val totalAmountDue: Long,
    val tenorMonths: Int,
    val status: LoanHistoryStatus,
    val paidInstallments: Int,
    val totalInstallments: Int,
    /** Null kalau [status]=PAID_OFF. */
    val nextDueDate: String?,
    val nextDueAmount: Long?,
    val createdAt: String?,
    /** Jadwal LENGKAP semua cicilan (bukan cuma yang berikutnya), urut installmentNumber - buat
     * dialog detail pas item History di-tap. */
    val installments: List<LoanInstallment>,
)

data class LoanInstallment(
    val installmentNumber: Int,
    val dueDate: String?,
    val amount: Long,
    /** Backend cuma punya dua nilai per cicilan (UNPAID/PAID, lihat InstallmentStatus) - beda
     * dari [LoanHistoryStatus] di level Loan yang juga punya OVERDUE, jadi disimpan sebagai
     * boolean sederhana di sini, bukan reuse enum yang sama. */
    val isPaid: Boolean,
    val paidAt: String?,
)
