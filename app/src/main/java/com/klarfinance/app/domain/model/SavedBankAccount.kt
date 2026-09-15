package com.klarfinance.app.domain.model

/** Rekening tujuan pencairan yang pernah dipakai nasabah (konfirmasi user 2026-09-07) - backend
 * otomatis menyimpannya begitu Loan bank-transfer berhasil (bukan langkah "simpan" terpisah),
 * lihat LoanRepository.getSavedBankAccounts. Dipakai isi dropdown "Rekening Tujuan" di
 * RequestLoanViewModel - kalau list ini kosong (belum pernah ada pengajuan berhasil), UI jatuh
 * balik ke input manual seperti biasa. */
data class SavedBankAccount(
    val id: Int,
    val bankCode: String,
    val bankAccountNumber: String,
)
