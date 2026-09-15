package com.klarfinance.app.domain.model

/** Hasil konfirmasi bayar QRIS (POST /nasabah/qris/transactions/{token}/confirm) - beda dari
 * LoanRequestResult (pengajuan pinjaman tunai), QRIS gak pernah punya status "reviewRequired" -
 * kalau gagal (kuota/plafond), backend selalu balikin error biasa (lihat ConfirmQrisUseCase). */
data class QrisConfirmResult(
    val loanId: Int,
    val merchantName: String,
    val totalAmountDue: Long,
    val installmentAmount: Long,
)
