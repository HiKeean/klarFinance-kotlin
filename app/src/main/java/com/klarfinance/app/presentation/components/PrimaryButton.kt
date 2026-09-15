package com.klarfinance.app.presentation.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.klarfinance.app.core.theme.KlarTeal

/**
 * Tombol CTA utama reusable - dipakai buat semua aksi "submit" (bukan cuma "Beli Tiket"
 * Transjakarta yang jadi trigger permintaan ini). Parameter [height]/[shape] disengaja tetap
 * fleksibel (bukan di-hardcode) karena tiap screen yang udah ada makainya beda-beda (52dp pill
 * di request-loan/QRIS vs 56dp rounded-rect di login/register) - biar retrofit ke screen lama
 * gak ngubah tampilan yang udah ada, murni dedup kode.
 *
 * [loadingText] kalau diisi -> teks tombol ganti jadi itu selagi [isLoading] (pola QRIS/
 * Transjakarta, "Memproses..."). Kalau [loadingText] null -> teks diganti spinner polos (pola
 * Login/CompleteProfile).
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    loadingText: String? = null,
    height: Dp = 52.dp,
    shape: Shape = RoundedCornerShape(14.dp),
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier.fillMaxWidth().height(height),
        shape = shape,
        colors = ButtonDefaults.buttonColors(containerColor = KlarTeal),
    ) {
        if (isLoading && loadingText == null) {
            CircularProgressIndicator(
                modifier = Modifier.height(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isLoading) loadingText ?: text else text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
