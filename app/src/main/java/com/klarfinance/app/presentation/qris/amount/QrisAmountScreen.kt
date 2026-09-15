package com.klarfinance.app.presentation.qris.amount

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klarfinance.app.core.theme.KlarTeal
import com.klarfinance.app.domain.model.QrisConfirmResult
import com.klarfinance.app.presentation.components.TransactionPasswordDialog
import com.klarfinance.app.presentation.loan.formatRupiah
import kotlinx.coroutines.flow.collectLatest

/**
 * Langkah setelah scan QR (lihat QrisScanScreen) - validasi merchant (via ScanQrisUseCase, jalan
 * otomatis di ViewModel.init dari merchantCode hasil decode), masukin nominal, preview bunga,
 * konfirmasi bayar. Tenor SELALU 1 bulan (konfirmasi user) - tidak ada pilihan tenor seperti
 * LoanAmountScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrisAmountScreen(
    onBackClick: () -> Unit,
    onDone: () -> Unit,
    viewModel: QrisViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var result by remember { mutableStateOf<QrisConfirmResult?>(null) }
    val activity = LocalContext.current as? FragmentActivity

    LaunchedEffect(Unit) {
        viewModel.submitted.collectLatest { result = it }
    }

    result?.let { SuccessDialog(result = it, onDismiss = onDone) }

    if (uiState.requiresPasswordConfirm) {
        TransactionPasswordDialog(
            password = uiState.passwordInput,
            onPasswordChange = viewModel::onPasswordInputChange,
            onConfirm = viewModel::onPasswordConfirm,
            onDismiss = viewModel::onPasswordConfirmDismiss,
            isVerifying = uiState.isVerifyingPassword,
            errorMessage = uiState.passwordError,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bayar QRIS") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
        bottomBar = {
            if (!uiState.isScanning && uiState.scanErrorMessage == null) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Button(
                        onClick = { activity?.let(viewModel::onSubmitClick) },
                        enabled = uiState.isFormValid && !uiState.isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = KlarTeal),
                    ) {
                        Text(if (uiState.isSubmitting) "Memproses…" else "Bayar Sekarang")
                    }
                }
            }
        },
    ) { padding ->
        if (uiState.isScanning) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(80.dp))
                CircularProgressIndicator(color = KlarTeal)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Memvalidasi QR…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (uiState.scanErrorMessage != null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                Text(uiState.scanErrorMessage!!, color = MaterialTheme.colorScheme.error)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storefront, contentDescription = null, tint = KlarTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(uiState.merchantName ?: "-", style = MaterialTheme.typography.titleMedium)
                }

                uiState.remainingQrisQuota?.let { remaining ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Sisa kuota QRIS: ${formatRupiah(remaining)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = uiState.amountInput,
                    onValueChange = viewModel::onAmountChange,
                    label = { Text("Nominal Bayar") },
                    prefix = { Text("Rp ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )

                if (uiState.amountValue() > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SummaryRow(label = "Bunga (1 bulan)", value = formatRupiah(uiState.interestFee))
                    SummaryRow(label = "Total Bayar", value = formatRupiah(uiState.totalToPay), strong = true)
                }

                if (uiState.submitErrorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(uiState.submitErrorMessage!!, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, strong: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun SuccessDialog(result: QrisConfirmResult, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pembayaran Berhasil") },
        text = {
            Column {
                Text("Pembayaran ke ${result.merchantName} berhasil.")
                Text("Total tagihan: ${formatRupiah(result.totalAmountDue)}")
                Text("Jatuh tempo 1 bulan dari sekarang.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Selesai", color = KlarTeal) } },
    )
}
