package com.klarfinance.app.core.scan

import android.content.Context
import android.content.pm.PackageManager

/**
 * Device app scoring (BRD/lending-flow.md - referensi pinjol OJK) - dipakai di DUA tempat:
 * registrasi (EngineScoringDomain.applyPinjolAdjustment) dan pengajuan pinjaman >30% dari
 * plafond (LoanReviewRequest, backend LoanInterestPolicy.exceedsReviewThreshold). Sebelum ini
 * ditambahkan, field `pinjolApps`/`bankApps` di request manapun SELALU kosong - app Kotlin ini
 * tidak pernah benar-benar men-scan apapun.
 *
 * Package name di [PINJOL_PACKAGES]/[BANK_PACKAGES] sudah diverifikasi terhadap listing Play
 * Store (2026-09-14) - 12 dari 17 entri sebelumnya salah tebak (mis. Kredivo aslinya
 * com.finaccel.android, bukan com.kredivo.app), yang bikin scan selalu melaporkan "tidak
 * terinstall" meski aplikasinya ada. Kalau ada laporan false-negative lagi, curigai dulu
 * apakah developer app tsb rebrand/ganti package name di Play Store.
 *
 * Pakai `<queries>` di AndroidManifest.xml (bukan `QUERY_ALL_PACKAGES`) - permission itu kena
 * scrutiny lebih ketat di Play Store review untuk app kategori fintech konsumen biasa (App
 * mirip alasan project ini menghindari `SCHEDULE_EXACT_ALARM` untuk LocationScheduler, lihat
 * kotlin-location-capture-worker.md).
 */
object InstalledAppsScanner {

    private val PINJOL_PACKAGES = mapOf(
        "com.finaccel.android" to "Kredivo",
        "io.silvrr.installment" to "Akulaku",
        "com.adakami.dana.kredit.pinjaman" to "AdaKami",
        "com.kreditpintar" to "Kredit Pintar",
        "com.julofinance.juloapp" to "JULO",
        "com.fintopia.idnEasycash.google" to "Easycash",
        "com.indodana.app" to "Indodana",
        "com.loan.cash.credit.easy.kilat.cepat.pinjam.uang.dana.rupiah" to "RupiahCepat",
        "com.pinjamango" to "PinjamanGo",
        "id.maucash.app" to "Maucash",
        "com.cmcm.uangme" to "UangMe",
        "com.yinshan.program.banda" to "AdaPundi",
    )

    private val BANK_PACKAGES = mapOf(
        "com.bca" to "BCA Mobile",
        "com.bca.mybca.omni.android" to "myBCA",
        "id.co.bri.brimo" to "BRImo",
        "id.bmri.livin" to "Livin by Mandiri",
        "src.com.bni" to "BNI Mobile Banking",
        "com.btpn.dc" to "Jenius",
        "net.myinfosys.PermataMobileX" to "PermataMobile",
        "com.gojek.gopay" to "GoPay",
    )

    data class ScanResult(val pinjolApps: List<String>, val bankApps: List<String>)

    /** Cuma cek ADA/TIDAKnya tiap package di [PINJOL_PACKAGES]/[BANK_PACKAGES] lewat
     * `getPackageInfo` (butuh `<queries>` manifest supaya visible di Android 11+/API 30+) -
     * tidak butuh permission runtime apapun, jadi bisa dipanggil langsung tanpa dialog izin. */
    fun scan(context: Context): ScanResult {
        val packageManager = context.packageManager
        return ScanResult(
            pinjolApps = detectInstalled(packageManager, PINJOL_PACKAGES),
            bankApps = detectInstalled(packageManager, BANK_PACKAGES),
        )
    }

    private fun detectInstalled(packageManager: PackageManager, candidates: Map<String, String>): List<String> {
        return candidates.mapNotNull { (packageName, label) ->
            try {
                packageManager.getPackageInfo(packageName, 0)
                label
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        }
    }
}
