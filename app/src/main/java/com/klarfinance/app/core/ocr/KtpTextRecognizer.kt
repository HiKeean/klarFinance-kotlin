package com.klarfinance.app.core.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * OCR KTP on-device (ML Kit Text Recognition, model Latin dibundle di APK - tidak butuh koneksi
 * internet maupun download model terpisah lewat Play Services) - konfirmasi user 2026-09-06:
 * foto KTP (data pribadi/NIK) sengaja TIDAK dikirim ke server manapun buat OCR, semua diproses
 * lokal di HP nasabah. Lihat [KtpOcrParser] buat logic baca layoutnya.
 */
object KtpTextRecognizer {

    suspend fun recognize(context: Context, uri: Uri): KtpOcrResult {
        val rawText = runCatching { recognizeRawText(context, uri) }.getOrNull() ?: return KtpOcrResult(null, null)
        return KtpOcrParser.parse(rawText)
    }

    private suspend fun recognizeRawText(context: Context, uri: Uri): String {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { result -> continuation.resume(result.text) }
                .addOnFailureListener { exception -> continuation.resumeWithException(exception) }
        }
    }
}
