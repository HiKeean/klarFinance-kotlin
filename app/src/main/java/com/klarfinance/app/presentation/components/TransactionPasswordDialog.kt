package com.klarfinance.app.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Column

/**
 * Step-up auth dialog untuk konfirmasi transaksi (pinjaman tunai/QRIS) kalau nasabah belum
 * aktifkan fingerprint (konfirmasi user 2026-09-07 - "kalau ga nyalain fingerprint, harus
 * masukin password"). Presentation-only, dipakai bareng oleh RequestLoanViewModel dan
 * QrisViewModel (masing-masing pegang state passwordInput/isVerifyingPassword/passwordError
 * sendiri - lihat komentar di ViewModel-nya kenapa gak diekstrak jadi 1 shared ViewModel).
 */
@Composable
fun TransactionPasswordDialog(
    password: String,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isVerifying: Boolean,
    errorMessage: String?,
) {
    AlertDialog(
        onDismissRequest = { if (!isVerifying) onDismiss() },
        title = { Text("Konfirmasi Transaksi") },
        text = {
            Column {
                Text(
                    "Sidik jari belum diaktifkan - masukkan password akun Anda untuk melanjutkan transaksi ini.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    singleLine = true,
                    enabled = !isVerifying,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = errorMessage != null,
                )
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isVerifying && password.isNotBlank()) {
                Text(if (isVerifying) "Memverifikasi…" else "Konfirmasi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isVerifying) { Text("Batal") }
        },
    )
}
