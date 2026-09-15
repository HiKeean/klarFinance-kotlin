package com.klarfinance.app.domain.model

/** Stable key shared between Home's "Eksplor fitur KlarFinance" card (a few categories, compact)
 * and the full "Semua fitur" catalog (more categories, more items per category) - lets a section
 * header's arrow on Home deep-link to (and auto-scroll to) the SAME section on the full catalog,
 * without matching on display title strings (which already differ in casing/wording between the
 * two independently-maintained lists). */
enum class FeatureCategoryKey {
    TRANSFER,
    PEMBAYARAN,
    PROMO,
    GAMES_HIBURAN,
}
