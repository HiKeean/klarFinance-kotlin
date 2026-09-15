package com.klarfinance.app.presentation.loan

/** Thousand-separator grouping tanpa prefix "Rp" - dipakai formatRupiah di bawah DAN buat live-
 * format field input nominal (LoanAmountScreen/QrisAmountScreen) selagi user ngetik, konfirmasi
 * user 2026-09-07 ("angka nominalnya jadiin Rupiah Format, jadi 10.000 atau 100.000"). */
fun groupThousands(digits: String): String = digits.reversed().chunked(3).joinToString(".").reversed()

/** Same grouping approach as HomeScreen.formatRupiah - kept as a small local duplicate rather
 * than a shared core util, consistent with how this codebase treats trivial one-off formatters. */
fun formatRupiah(amount: Long): String = "Rp ${groupThousands(amount.toString())}"

/** Kompak buat kartu preset kecil (referensi desain: "Rp16.0k") - >=1jt dibulatkan 1 desimal
 * jutaan ("Rp1.5jt"), sisanya ribuan ("Rp250rb"). */
fun formatRupiahCompact(amount: Long): String = when {
    amount >= 1_000_000 -> "Rp${"%.1f".format(amount / 1_000_000.0)}jt"
    amount >= 1_000 -> "Rp${amount / 1_000}rb"
    else -> "Rp$amount"
}
