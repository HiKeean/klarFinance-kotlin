package com.klarfinance.app.core.ocr

/** Best-effort hasil baca KTP - null di salah satu/kedua field kalau parser tidak yakin, biarkan
 * user isi manual (lihat CompleteProfileScreen, field-nya tetap bisa diedit). */
data class KtpOcrResult(val nik: String?, val name: String?)

/**
 * Heuristik pembacaan layout KTP Indonesia dari teks mentah ML Kit Text Recognition (konfirmasi
 * user 2026-09-06 - auto-fill NIK/Nama pas registrasi). KTP TIDAK punya format teks yang seragam
 * antar hasil scan/foto (kualitas cahaya, kemiringan, font OCR-B yang suka kebaca ber-spasi per
 * digit) - jadi ini best-effort, BUKAN parser yang menjamin selalu benar. Dipisah dari
 * [KtpTextRecognizer] biar gampang di-unit-test tanpa dependency ML Kit/Android.
 */
object KtpOcrParser {

    fun parse(rawText: String): KtpOcrResult {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        return KtpOcrResult(nik = extractNik(lines), name = extractName(lines))
    }

    /** NIK selalu 16 digit. Coba baris yang eksplisit berlabel "NIK" dulu, baru fallback ke baris
     * manapun yang isinya (setelah dibuang spasi) persis 16 digit - font KTP sering bikin ML Kit
     * baca NIK dengan spasi antar digit. */
    private fun extractNik(lines: List<String>): String? {
        lines.firstOrNull { it.contains("NIK", ignoreCase = true) }
            ?.let { line -> line.filter(Char::isDigit) }
            ?.takeIf { it.length == 16 }
            ?.let { return it }

        return lines
            .map { it.filter(Char::isDigit) }
            .firstOrNull { it.length == 16 }
    }

    /** Baris "Nama" bisa berformat "Nama : BUDI SANTOSO", "Nama BUDI SANTOSO" (colon tidak
     * kebaca), atau "Nama" sendirian di satu baris dengan nilainya di baris berikutnya. */
    private fun extractName(lines: List<String>): String? {
        val labelIndex = lines.indexOfFirst { it.contains("Nama", ignoreCase = true) }
        if (labelIndex == -1) return null
        val labelLine = lines[labelIndex]

        val afterColon = labelLine.substringAfter(":", "").trim()
        if (afterColon.isNotBlank()) return afterColon

        val afterLabel = labelLine.replace(Regex("(?i)nama"), "").trim(' ', ':', '-')
        if (afterLabel.isNotBlank()) return afterLabel

        return lines.drop(labelIndex + 1).firstOrNull { it.isNotBlank() }
    }
}
