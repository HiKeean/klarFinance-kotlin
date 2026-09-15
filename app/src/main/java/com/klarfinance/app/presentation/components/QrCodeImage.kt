package com.klarfinance.app.presentation.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Render QR code dari teks apapun - murni GENERATE, beda dari mlkit.barcode.scanning yang
 * dipakai buat DECODE QR (QrisScanScreen), gak butuh izin kamera. Reusable - pertama dipakai
 * buat tampilin tiket Transjakarta mockup ("QRIS ala-ala", isinya cuma ticketCode - konfirmasi
 * user, bukan payload QRIS EMVCo beneran), aman dipakai ulang fitur lain ke depan.
 */
@Composable
fun QrCodeImage(content: String, modifier: Modifier = Modifier, sizeDp: Int = 240) {
    val bitmap = remember(content, sizeDp) { generateQrBitmap(content, sizeDp) }
    Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR code", modifier = modifier.size(sizeDp.dp))
}

private fun generateQrBitmap(content: String, sizePx: Int): Bitmap {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
    for (x in 0 until sizePx) {
        for (y in 0 until sizePx) {
            bitmap.setPixel(x, y, if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
        }
    }
    return bitmap
}
