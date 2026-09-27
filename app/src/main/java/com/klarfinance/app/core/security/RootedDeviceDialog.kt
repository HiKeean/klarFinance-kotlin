package com.klarfinance.app.core.security

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogProperties
import com.klarfinance.app.core.theme.KlarTeal

/**
 * [onContinue] null = blokir total (release): dialog gak bisa ditutup, satu-satunya aksi keluar app.
 * Non-null = debug build, boleh lanjut biar tetap bisa dites di emulator/HP root.
 */
@Composable
fun RootedDeviceDialog(onExit: () -> Unit, onContinue: (() -> Unit)?) {
    AlertDialog(
        onDismissRequest = { onContinue?.invoke() },
        properties = DialogProperties(
            dismissOnBackPress = onContinue != null,
            dismissOnClickOutside = onContinue != null,
        ),
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Perangkat Terdeteksi Root") },
        text = {
            Text(
                "Demi keamanan akun dan data keuangan Anda, KlarFinance tidak dapat digunakan di perangkat yang sudah di-root.",
                textAlign = TextAlign.Center,
            )
        },
        confirmButton = {
            TextButton(onClick = onExit) { Text("Tutup Aplikasi", color = KlarTeal) }
        },
        dismissButton = onContinue?.let {
            { TextButton(onClick = it) { Text("Lanjut (debug)") } }
        },
    )
}
