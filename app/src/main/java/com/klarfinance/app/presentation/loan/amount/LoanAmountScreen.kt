package com.klarfinance.app.presentation.loan.amount

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klarfinance.app.core.theme.KlarTeal
import com.klarfinance.app.presentation.loan.POPULAR_TENOR
import com.klarfinance.app.presentation.loan.RequestLoanUiState
import com.klarfinance.app.presentation.loan.RequestLoanViewModel
import com.klarfinance.app.presentation.loan.TENOR_OPTIONS
import com.klarfinance.app.presentation.loan.formatRupiah
import com.klarfinance.app.presentation.loan.formatRupiahCompact

/**
 * Langkah 1 dari 2 pengajuan pinjaman - nominal + tenor (lihat LoanBankAccountScreen buat
 * langkah 2/rekening tujuan + submit sebenarnya). Layout mengikuti referensi desain user
 * ("Masukkan detail pinjaman") - TAPI 3 kartu preset nominal di sini dihitung dari plafond
 * TERSEDIA nasabah masing-masing (RequestLoanUiState.amountPresets), bukan angka universal
 * seperti di referensi. Tenor sengaja cuma 1/3/6/9/12 (referensi sempat nunjukin "2 bulan" juga)
 * - backend tidak punya rate resmi buat tenor itu (LoanInterestPolicy), submit dengan tenor di
 * luar daftar ini selalu ditolak.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanAmountScreen(
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit,
    viewModel: RequestLoanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Masukkan detail pinjaman") },
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
                    onClick = onContinueClick,
                    enabled = uiState.isAmountStepValid && !uiState.isLoadingLimit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = KlarTeal),
                ) {
                    Text("Lanjut")
                }
            }
        },
    ) { padding ->
        if (uiState.isLoadingLimit) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = KlarTeal)
            }
        } else if (uiState.loadErrorMessage != null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                Text(uiState.loadErrorMessage!!, color = MaterialTheme.colorScheme.error)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                AmountField(uiState = uiState, onAmountChange = viewModel::onAmountChange)

                uiState.limitSummary?.let { limit ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Limit tersedia: ${formatRupiah(limit.availableLimit)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (uiState.amountPresets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        uiState.amountPresets.forEach { preset ->
                            AmountPresetCard(
                                amount = preset,
                                selected = uiState.amountValue() == preset,
                                recommended = preset == uiState.recommendedAmount,
                                onClick = { viewModel.onAmountPresetSelected(preset) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
                Text("Pilih tenor:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                TENOR_OPTIONS.chunked(2).forEach { rowTenors ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    ) {
                        rowTenors.forEach { tenor ->
                            TenorCard(
                                tenorMonths = tenor,
                                selected = uiState.tenorMonths == tenor,
                                onClick = { viewModel.onTenorSelected(tenor) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowTenors.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }

                if (uiState.amountValue() > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LoanCalculationSummary(uiState = uiState)
                }
            }
        }
    }
}

/** Muncul begitu nominal terisi - rincian nominal pinjaman, biaya admin, bunga (mengikuti tenor
 * yang dipilih), dan nominal yang benar-benar cair (konfirmasi user 2026-09-06, mirror
 * LoanService.createLoan/finalizeLoan di backend - lihat RequestLoanUiState.adminFee/totalInterest/
 * netAmountReceived). */
@Composable
private fun LoanCalculationSummary(uiState: RequestLoanUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(16.dp),
    ) {
        SummaryRow("Nominal Pinjaman", formatRupiah(uiState.amountValue()))
        Spacer(modifier = Modifier.height(8.dp))
        SummaryRow("Biaya Admin (1%)", "-${formatRupiah(uiState.adminFee)}")
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(12.dp))
        SummaryRow("Nominal Diterima", formatRupiah(uiState.netAmountReceived), emphasize = true)
        Spacer(modifier = Modifier.height(16.dp))
        SummaryRow("Bunga (${uiState.tenorMonths} bulan) ${uiState.monthlyRatePercentLabel}", formatRupiah(uiState.totalInterest))
        Spacer(modifier = Modifier.height(8.dp))
        SummaryRow("Cicilan per Bulan", formatRupiah(uiState.installmentAmount), emphasize = true)
    }
}

@Composable
private fun SummaryRow(label: String, value: String, emphasize: Boolean = false) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = if (emphasize) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
            color = if (emphasize) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (emphasize) KlarTeal else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AmountField(uiState: RequestLoanUiState, onAmountChange: (String) -> Unit) {
    Column {
        Text(
            "Nominal pinjaman *",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextField(
            // Cursor dipaksa selalu di akhir teks (bukan posisi asli tempat user ngetik) - amountInput
            // di-reformat ulang PENUH tiap keystroke (groupThousands nyisipin "." buat ribuan), jadi
            // panjang string berubah-ubah dan Compose gak bisa nebak posisi cursor yang benar dari situ
            // (itu yang bikin cursor "maju sendiri" pas ngetik nol banyak). Aman dipaksa ke akhir karena
            // nominal Rupiah selalu diketik kiri-ke-kanan, gak pernah edit di tengah.
            value = TextFieldValue(text = uiState.amountInput, selection = TextRange(uiState.amountInput.length)),
            onValueChange = { onAmountChange(it.text) },
            textStyle = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
            prefix = { Text("Rp ", style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)) },
            placeholder = { Text("0", style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = KlarTeal,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AmountPresetCard(
    amount: Long,
    selected: Boolean,
    recommended: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) KlarTeal else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(16.dp),
                )
                .clickable(onClick = onClick)
                .padding(vertical = 16.dp, horizontal = 8.dp),
        ) {
            Icon(Icons.Default.Payments, contentDescription = null, tint = KlarTeal.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                formatRupiahCompact(amount),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
        if (recommended) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-8).dp)
                    .clip(RoundedCornerShape(50))
                    .background(KlarTeal)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text("REC", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TenorCard(tenorMonths: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (selected) KlarTeal.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)
                .border(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) KlarTeal else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(14.dp),
                )
                .clickable(onClick = onClick)
                .padding(vertical = 16.dp),
        ) {
            Text("$tenorMonths bulan", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
        if (tenorMonths == POPULAR_TENOR) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-8).dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.secondary)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text("POPULER", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
