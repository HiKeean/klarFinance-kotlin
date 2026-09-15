package com.klarfinance.app.presentation.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klarfinance.app.core.theme.KlarTeal
import com.klarfinance.app.domain.model.LoanInstallment
import com.klarfinance.app.presentation.loan.formatRupiah

/**
 * Halaman "Bayar" - beda dari [InstallmentScheduleDialog] (cuma lihat jadwal, di HistoryScreen.kt)
 * yang sifatnya read-only. Di sini SEMUA cicilan loan ditampilkan, user centang cicilan mana saja
 * yang mau dibayar (bebas, gak harus urut), nominal total di atas naik sesuai yang dicentang, lalu
 * itu yang dikirim ke [RepayLoanUseCase]. Backend tetap yang jadi source of truth soal validasi
 * minimum/maksimum pembayaran (LoanService#repay) - layar ini cuma menjumlahkan nominal cicilan
 * yang dipilih user.
 *
 * ViewModel di-share dengan HistoryScreen (parent nav graph [Screen.HistoryGraph]), jadi datanya
 * (termasuk seluruh installments) sudah ada dari GET /loan/history, tidak perlu network call baru.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    loanId: Int,
    onBackClick: () -> Unit,
    viewModel: HistoryViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val item = uiState.findItem(loanId)

    // Cicilan yang kecentang gak boleh kebawa dari kunjungan/loan lain sebelumnya - direset
    // begitu layar ini dibuka, dan dibersihkan lagi begitu ditinggalkan (balik ke History).
    DisposableEffect(loanId) {
        viewModel.resetPaymentSelection()
        onDispose { viewModel.resetPaymentSelection() }
    }

    // Pembayaran sukses -> pop balik ke History; History yang nampilin snackbar-nya (lihat
    // HistoryScreen.kt, uiState.pendingPaymentMessage adalah StateFlow supaya nilainya gak
    // hilang walau di-collect belakangan pas History kembali aktif).
    LaunchedEffect(uiState.pendingPaymentMessage) {
        if (uiState.pendingPaymentMessage != null) onBackClick()
    }

    val selectedTotal = uiState.selectedPaymentTotal(loanId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bayar") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
        bottomBar = {
            Box(modifier = Modifier.padding(20.dp)) {
                Button(
                    onClick = { viewModel.submitPayment(loanId) },
                    enabled = selectedTotal > 0 && !uiState.isSubmittingPayment,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = KlarTeal),
                ) {
                    if (uiState.isSubmittingPayment) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Bayar Sekarang")
                    }
                }
            }
        },
    ) { padding ->
        if (item == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = KlarTeal)
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TotalHeader(amount = selectedTotal)

            if (uiState.paymentErrorMessage != null) {
                Text(
                    text = uiState.paymentErrorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(item.installments, key = { it.installmentNumber }) { installment ->
                    SelectableInstallmentRow(
                        installment = installment,
                        selected = installment.installmentNumber in uiState.selectedInstallmentNumbers,
                        onToggle = { viewModel.onInstallmentToggled(installment.installmentNumber) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalHeader(amount: Long) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(
            text = "Total yang akan dibayar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatRupiah(amount),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = KlarTeal,
        )
    }
}

@Composable
private fun SelectableInstallmentRow(installment: LoanInstallment, selected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !installment.isPaid, onClick = onToggle)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = installment.isPaid || selected,
            onCheckedChange = { onToggle() },
            enabled = !installment.isPaid,
            colors = CheckboxDefaults.colors(checkedColor = KlarTeal),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Cicilan ke-${installment.installmentNumber}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = if (installment.isPaid) {
                    "Lunas"
                } else {
                    installment.dueDate?.let { "Jatuh tempo ${formatDate(it)}" } ?: "Jatuh tempo tidak diketahui"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = formatRupiah(installment.amount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
