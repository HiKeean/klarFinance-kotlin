package com.klarfinance.app.core.security

import android.content.Context
import com.scottyab.rootbeer.RootBeer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Deteksi root lokal (RootBeer: su binary, Magisk/SuperSU app, test-keys, system RW, dll).
 * Tanpa busybox check - banyak ROM pabrikan bawaan sudah menyertakan busybox, jadi check itu
 * rawan false positive. Ini lapisan client-side saja (bisa di-bypass pakai Magisk DenyList/Frida),
 * cukup buat showcase; jaminan kuat butuh Play Integrity yang diverifikasi di backend.
 */
class RootDetector @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Baca filesystem + native check - jangan dipanggil di main thread. */
    suspend fun isRooted(): Boolean = withContext(Dispatchers.IO) {
        runCatching { RootBeer(context).isRootedWithoutBusyBoxCheck() }.getOrDefault(false)
    }
}
