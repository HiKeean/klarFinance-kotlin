package com.klarfinance.app.domain.repository

import com.klarfinance.app.domain.model.Cached
import com.klarfinance.app.domain.model.LimitSummary
import com.klarfinance.app.domain.model.LoanHistoryItem
import com.klarfinance.app.domain.model.LoanRequestResult
import com.klarfinance.app.domain.model.SavedBankAccount

interface LoanRepository {
    /** GET api/v1/nasabah/loan/limit - fails (Result.failure) if the caller has no active
     * limit yet, same as the backend endpoint (see LoanService#getMyLimitSummary). */
    suspend fun getLimitSummary(): Result<LimitSummary>

    /** GET api/v1/nasabah/loan/bank-accounts - rekening tujuan pencairan yang pernah dipakai
     * (konfirmasi user 2026-09-07), otomatis kesimpen backend tiap Loan bank-transfer berhasil.
     * Kosong (bukan failure) kalau belum pernah ada. */
    suspend fun getSavedBankAccounts(): Result<List<SavedBankAccount>>

    /** POST api/v1/nasabah/loan - lihat LoanRequestResult.reviewRequired, pengajuan bisa langsung
     * cair ATAU ditahan buat review BM tergantung utilisasi plafond setelahnya. */
    suspend fun requestLoan(
        amount: Long,
        tenorMonths: Int,
        bankAccountNumber: String,
        bankCode: String,
        pinjolApps: List<String> = emptyList(),
        bankApps: List<String> = emptyList(),
    ): Result<LoanRequestResult>

    /** GET api/v1/nasabah/loan/history - "History" page: semua Loan nasabah ini, tarik tunai
     * maupun bayar QRIS, terbaru duluan. Kosong (bukan failure) kalau belum pernah pinjam.
     * Network-first, falls back to the last cached list (Room) if the device is offline - see
     * [Cached.isFromCache]. */
    suspend fun getLoanHistory(): Result<Cached<List<LoanHistoryItem>>>

    /** POST api/v1/nasabah/loan/{loanId}/repayment - fitur "Bayar". Minimum nominal ditentukan
     * backend (lihat LoanService#repay): kalau cicilan terdekat sudah jatuh tempo, minimum =
     * nominal cicilan itu; kalau belum, boleh berapa saja (langsung mengurangi pokok). Gagal
     * (Result.failure) dengan pesan dari backend kalau nominal di bawah minimum atau melebihi
     * sisa tagihan - pesan itu sudah user-facing, tinggal ditampilkan apa adanya. Sukses
     * mengembalikan item History yang sudah ter-update. */
    suspend fun repay(loanId: Int, amount: Long): Result<LoanHistoryItem>
}
