package com.klarfinance.app.domain.model

/** Hasil submit pengajuan pinjaman (POST /nasabah/loan). [reviewRequired]=true berarti pengajuan
 * melebihi 30% dari plafond dan ditahan sebagai LoanReviewRequest sampai BM memutuskan - belum
 * cair sama sekali, field cicilan di bawah semuanya null. */
data class LoanRequestResult(
    val loanId: Int?,
    /** Nominal yang benar-benar cair ke rekening nasabah setelah dipotong biaya admin - null
     * kalau [reviewRequired]=true. */
    val disbursedAmount: Long?,
    val adminFee: Long?,
    val totalAmountDue: Long?,
    val installmentAmount: Long?,
    val tenorMonths: Int?,
    val reviewRequired: Boolean,
    val reviewRequestId: Int?,
    val message: String?,
)
