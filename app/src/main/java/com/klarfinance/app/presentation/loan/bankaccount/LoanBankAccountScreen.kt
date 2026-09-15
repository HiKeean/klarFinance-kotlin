package com.klarfinance.app.presentation.loan.bankaccount

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klarfinance.app.core.theme.KlarTeal
import com.klarfinance.app.domain.model.LoanRequestResult
import com.klarfinance.app.domain.model.SavedBankAccount
import com.klarfinance.app.presentation.components.TransactionPasswordDialog
import com.klarfinance.app.presentation.loan.BANK_OPTIONS
import com.klarfinance.app.presentation.loan.RequestLoanViewModel
import com.klarfinance.app.presentation.loan.formatRupiah
import kotlinx.coroutines.flow.collectLatest

/**
 * Langkah 2/terakhir pengajuan pinjaman - rekening tujuan pencairan, lalu submit beneran
 * (POST /nasabah/loan). Nominal+tenor sudah dipilih di LoanAmountScreen sebelumnya (ViewModel
 * yang sama, di-scope ke nav-graph request-loan - lihat KlarNavHost).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanBankAccountScreen(
    onBackClick: () -> Unit,
    onDone: () -> Unit,
    viewModel: RequestLoanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var result by remember { mutableStateOf<LoanRequestResult?>(null) }
    val activity = LocalContext.current as? FragmentActivity

    LaunchedEffect(Unit) {
        viewModel.submitted.collectLatest { result = it }
    }

    result?.let { submitted ->
        if (submitted.reviewRequired) {
            ReviewRequiredDialog(message = submitted.message, onDismiss = onDone)
        } else {
            SuccessDialog(result = submitted, onDismiss = onDone)
        }
    }

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
                title = { Text("Rekening Tujuan") },
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
                    onClick = { activity?.let(viewModel::onSubmitClick) },
                    enabled = uiState.isBankStepValid && !uiState.isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = KlarTeal),
                ) {
                    Text(if (uiState.isSubmitting) "Memproses…" else "Ajukan Sekarang")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text(
                "Pinjaman ${formatRupiah(uiState.amountValue())} - ${uiState.tenorMonths} bulan",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Cicilan per bulan: ${formatRupiah(uiState.installmentAmount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Rekening tujuan pencairan (konfirmasi user 2026-09-07 - "simpan di db"): kalau
            // nasabah sudah pernah pakai rekening sebelumnya, tinggal pilih dari dropdown -
            // TIDAK perlu ketik ulang. Kalau belum pernah ada sama sekali, jatuh balik ke input
            // manual seperti sebelumnya (lihat RequestLoanUiState.isBankStepValid). Ditahan
            // sampai isLoadingSavedBankAccounts selesai biar gak "lompat" dari form manual ke
            // dropdown di tengah nasabah lagi ngetik kalau responsnya lambat.
            if (uiState.isLoadingSavedBankAccounts) {
                Text(
                    "Memuat rekening tersimpan…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (uiState.savedBankAccounts.isNotEmpty()) {
                SavedBankAccountDropdown(
                    accounts = uiState.savedBankAccounts,
                    selectedId = uiState.selectedSavedBankAccountId,
                    onSelected = viewModel::onSavedBankAccountSelected,
                )
            } else {
                OutlinedTextField(
                    value = uiState.bankAccountNumber,
                    onValueChange = viewModel::onBankAccountNumberChange,
                    label = { Text("Nomor Rekening Tujuan") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(16.dp))
                BankDropdown(selectedCode = uiState.bankCode, onSelected = viewModel::onBankCodeSelected)
            }

            if (uiState.submitErrorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(uiState.submitErrorMessage!!, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/**
 * Plain Box + clickable + DropdownMenu (bukan ExposedDropdownMenuBox) - pola yang sama dipakai
 * di CompleteProfileScreen.kt (cascading location dropdown), sudah terbukti jalan di versi
 * Material3 project ini. Cuma 5 opsi, jadi Column biasa tanpa search sudah cukup.
 */
@Composable
private fun BankDropdown(selectedCode: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = BANK_OPTIONS.firstOrNull { it.code == selectedCode }?.label ?: selectedCode

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Bank Tujuan", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (expanded) 1.5.dp else 1.dp,
                    color = if (expanded) KlarTeal else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(14.dp),
                )
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(selectedLabel, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                BANK_OPTIONS.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onSelected(option.code)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

/**
 * Dropdown rekening tujuan yang pernah dipakai (konfirmasi user 2026-09-07 - "simpan di db") -
 * pola sama persis dengan [BankDropdown] di bawah, cuma isinya dari
 * RequestLoanUiState.savedBankAccounts (backend), bukan daftar bank statis.
 */
@Composable
private fun SavedBankAccountDropdown(
    accounts: List<SavedBankAccount>,
    selectedId: Int?,
    onSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = accounts.firstOrNull { it.id == selectedId }
    val selectedLabel = selected?.let { accountLabel(it) } ?: "Pilih rekening tujuan"

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Rekening Tujuan", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (expanded) 1.5.dp else 1.dp,
                    color = if (expanded) KlarTeal else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(14.dp),
                )
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(selectedLabel, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                accounts.forEach { account ->
                    DropdownMenuItem(
                        text = { Text(accountLabel(account)) },
                        onClick = {
                            onSelected(account.id)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

private fun accountLabel(account: SavedBankAccount): String {
    val bankLabel = BANK_OPTIONS.firstOrNull { it.code == account.bankCode }?.label ?: account.bankCode
    return "$bankLabel - ${account.bankAccountNumber}"
}

@Composable
private fun SuccessDialog(result: LoanRequestResult, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pengajuan Berhasil") },
        text = {
            Column {
                Text("Pinjaman Anda telah cair.")
                result.disbursedAmount?.let { Text("Dana diterima: ${formatRupiah(it)}") }
                result.totalAmountDue?.let { Text("Total tagihan: ${formatRupiah(it)}") }
                result.installmentAmount?.let { Text("Cicilan per bulan: ${formatRupiah(it)}") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Selesai", color = KlarTeal) } },
    )
}

@Composable
private fun ReviewRequiredDialog(message: String?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sedang Direview") },
        text = {
            Text(
                message
                    ?: "Pengajuan Anda melebihi 30% dari limit yang tersedia - sedang direview lebih lanjut oleh Branch Manager.",
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Mengerti", color = KlarTeal) } },
    )
}
