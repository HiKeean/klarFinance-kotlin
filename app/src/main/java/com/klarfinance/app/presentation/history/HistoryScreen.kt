package com.klarfinance.app.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klarfinance.app.core.theme.KlarTeal
import com.klarfinance.app.domain.model.LoanHistoryItem
import com.klarfinance.app.domain.model.LoanHistoryStatus
import com.klarfinance.app.domain.model.LoanHistoryType
import com.klarfinance.app.domain.model.LoanInstallment
import com.klarfinance.app.presentation.components.AppBottomBar
import com.klarfinance.app.presentation.components.OfflineBanner
import com.klarfinance.app.presentation.components.SearchField
import com.klarfinance.app.presentation.loan.formatRupiah

/**
 * "History" tab - semua Loan nasabah ini, tarik tunai (peminjaman) MAUPUN bayar QRIS
 * (pembayaran), satu list, terbaru duluan. Backend menyimpan keduanya sebagai entity Loan yang
 * sama, dibedakan lewat [LoanHistoryItem.type] (lihat LoanService#getMyLoanHistory) - jadi
 * ditampilkan sebagai satu riwayat gabungan, bukan dua tab terpisah.
 */
@Composable
fun HistoryScreen(
    onHomeClick: () -> Unit,
    onAccountClick: () -> Unit,
    onLoansClick: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    // Item yang lagi dibuka detail jadwal cicilannya - null berarti dialog tertutup. Datanya
    // sudah lengkap dari GET /loan/history sekalian (termasuk seluruh installments), jadi tap
    // sebuah baris gak perlu network call baru, cukup buka dialog dengan data yang sudah ada.
    var selectedItem by remember { mutableStateOf<LoanHistoryItem?>(null) }
    // Filter lokal murni (bukan query ulang ke backend) - GET /loan/history gak punya param
    // search, dan seluruh riwayat sudah kebawa sekali fetch, jadi cukup filter list yang udah
    // ada. Cocok kalau displayTitle-nya (sama teks yang ditampilkan di baris) mengandung query.
    var searchQuery by remember { mutableStateOf("") }
    val filteredItems = uiState.items.filter { it.historyDisplayTitle().contains(searchQuery.trim(), ignoreCase = true) }

    // pendingPaymentMessage adalah StateFlow (bukan event sekali-pakai biasa) supaya nilainya
    // masih ada begitu halaman Bayar pop back kesini dan effect ini mulai collect lagi - lihat
    // catatan di HistoryUiState.pendingPaymentMessage kenapa SharedFlow rawan race di sini.
    LaunchedEffect(uiState.pendingPaymentMessage) {
        uiState.pendingPaymentMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumePaymentMessage()
        }
    }

    selectedItem?.let { item ->
        InstallmentScheduleDialog(item = item, onDismiss = { selectedItem = null })
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Raw Text (bukan TopAppBar asli) butuh statusBarsPadding() manual - edge-to-edge
            // aktif di MainActivity, pola sama dengan AccountScreen/OtpVerificationScreen.
            Text(
                text = "History",
                style = MaterialTheme.typography.titleLarge,
                color = KlarTeal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                fontWeight = FontWeight.Bold,
            )
        },
        bottomBar = {
            AppBottomBar(
                hasAccount = true,
                activeTab = "History",
                onLoginRequested = {},
                onHomeClick = onHomeClick,
                onAccountClick = onAccountClick,
                onLockedTabClick = {},
                onLoansClick = onLoansClick,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                uiState.isLoading -> LoadingState()
                uiState.loadErrorMessage != null -> ErrorState(
                    message = uiState.loadErrorMessage!!,
                    onRetry = viewModel::loadHistory,
                )
                uiState.items.isEmpty() -> EmptyState()
                else -> Column(modifier = Modifier.fillMaxSize()) {
                    SearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Cari transaksi...",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                    if (uiState.isOffline) {
                        OfflineBanner(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                    }
                    if (filteredItems.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Transaksi tidak ditemukan",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        // Riwayat sekarang murni read-only (bener-bener history transaksi,
                        // konfirmasi user 2026-09-14) - gak ada tombol Bayar di sini lagi,
                        // itu sekarang cuma ada di BillsScreen (onBayarClick = null di
                        // HistoryList/HistoryItemCard artinya tombolnya gak dirender).
                        HistoryList(
                            items = filteredItems,
                            onItemClick = { selectedItem = it },
                        )
                    }
                }
            }
        }
    }
}

/** Satu-satunya tempat title per [LoanHistoryType] ditentukan - dipakai [HistoryItemCard],
 * [InstallmentScheduleDialog], DAN filter search (biar konsisten sama apa yang user LIHAT di
 * layar, gak ada logic title kepisah-pisah lagi kayak sebelumnya). */
fun LoanHistoryItem.historyDisplayTitle(): String = when (type) {
    LoanHistoryType.QRIS_PAYMENT -> "QRIS${merchantName?.let { " - $it" } ?: ""}"
    LoanHistoryType.TRANSJAKARTA_BILL -> "Transjakarta"
    LoanHistoryType.LOAN -> "Pinjaman Tunai"
}

/** Icon bubble per [LoanHistoryType], sama alasan reuse dengan [historyDisplayTitle]. */
fun LoanHistoryItem.historyDisplayIcon(): ImageVector = when (type) {
    LoanHistoryType.QRIS_PAYMENT -> Icons.Default.QrCode2
    LoanHistoryType.TRANSJAKARTA_BILL -> Icons.Default.DirectionsBus
    LoanHistoryType.LOAN -> Icons.Default.AccountBalance
}

@Composable
fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = KlarTeal)
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Coba Lagi") }
    }
}

@Composable
fun EmptyState(message: String = "Belum ada riwayat pinjaman atau pembayaran.", icon: ImageVector = Icons.Default.History) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Reusable list of history cards - dipakai [HistoryScreen] (semua item, read-only, [onBayarClick]
 * null) DAN [com.klarfinance.app.presentation.bills.BillsScreen] (cuma item belum lunas, dengan
 * [onBayarClick]). Non-private supaya bisa diimpor dari package `bills`. */
@Composable
fun HistoryList(
    items: List<LoanHistoryItem>,
    onItemClick: (LoanHistoryItem) -> Unit,
    onBayarClick: ((LoanHistoryItem) -> Unit)? = null,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items, key = { it.loanId }) { item ->
            HistoryItemCard(
                item,
                onScheduleClick = { onItemClick(item) },
                onBayarClick = onBayarClick?.let { callback -> { callback(item) } },
            )
        }
    }
}

@Composable
fun HistoryItemCard(item: LoanHistoryItem, onScheduleClick: () -> Unit, onBayarClick: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = item.historyDisplayIcon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.historyDisplayTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Tenor ${item.tenorMonths} bulan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusBadge(status = item.status)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(text = "NOMINAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = formatRupiah(item.requestedAmount),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "CICILAN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "${item.paidInstallments}/${item.totalInstallments} lunas",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        if (item.status != LoanHistoryStatus.PAID_OFF && item.nextDueDate != null && item.nextDueAmount != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Jatuh tempo berikutnya: ${formatDate(item.nextDueDate)} - ${formatRupiah(item.nextDueAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (item.status == LoanHistoryStatus.OVERDUE) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Lihat semua jadwal cicilan",
                style = MaterialTheme.typography.labelMedium,
                color = KlarTeal,
                modifier = Modifier.clickable(onClick = onScheduleClick),
            )
            if (onBayarClick != null && item.status != LoanHistoryStatus.PAID_OFF) {
                Button(
                    onClick = onBayarClick,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KlarTeal),
                ) {
                    Text("Bayar", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: LoanHistoryStatus) {
    val (label, containerColor, contentColor) = when (status) {
        LoanHistoryStatus.ACTIVE -> Triple("Berjalan", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        LoanHistoryStatus.OVERDUE -> Triple("Terlambat", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        LoanHistoryStatus.PAID_OFF -> Triple("Lunas", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = contentColor)
    }
}

/**
 * Jadwal LENGKAP semua cicilan satu Loan (bukan cuma jatuh tempo berikutnya seperti yang
 * ditampilkan di [HistoryItemCard]) - dibuka begitu kartu di History ditap. Datanya sudah ada di
 * [item] (GET /loan/history sekaligus balikin seluruh installments), jadi dialog ini murni
 * presentational, tidak ada network call sendiri.
 */
@Composable
fun InstallmentScheduleDialog(item: LoanHistoryItem, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.historyDisplayTitle(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Total tagihan ${formatRupiah(item.totalAmountDue)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(item.installments, key = { it.installmentNumber }) { installment ->
                    InstallmentRow(installment)
                }
            }
        }
    }
}

@Composable
private fun InstallmentRow(installment: LoanInstallment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (installment.isPaid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (installment.isPaid) KlarTeal else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Cicilan ke-${installment.installmentNumber}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = installment.dueDate?.let { "Jatuh tempo ${formatDate(it)}" } ?: "Jatuh tempo tidak diketahui",
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

/** dueDate dari backend itu ISO LocalDateTime ("2026-10-09T12:00:00") - potong ke tanggal doang
 * (10 karakter pertama) buat tampilan simpel, konsisten sama gimana AuthDto.kt sudah nyimpen
 * tanggal server sebagai String polos di layer ini tanpa parsing java.time. Dipakai juga dari
 * PaymentScreen.kt (package sama). */
internal fun formatDate(isoDateTime: String): String {
    val datePart = isoDateTime.take(10)
    val parts = datePart.split("-")
    if (parts.size != 3) return datePart
    val (year, month, day) = parts
    return "$day/$month/$year"
}
