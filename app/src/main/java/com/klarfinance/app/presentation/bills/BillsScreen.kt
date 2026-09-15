package com.klarfinance.app.presentation.bills

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klarfinance.app.core.theme.KlarTeal
import com.klarfinance.app.domain.model.LoanHistoryItem
import com.klarfinance.app.domain.model.LoanHistoryStatus
import com.klarfinance.app.presentation.components.OfflineBanner
import com.klarfinance.app.presentation.history.EmptyState
import com.klarfinance.app.presentation.history.ErrorState
import com.klarfinance.app.presentation.history.HistoryList
import com.klarfinance.app.presentation.history.HistoryViewModel
import com.klarfinance.app.presentation.history.InstallmentScheduleDialog
import com.klarfinance.app.presentation.history.LoadingState

/**
 * "Bills" quick action dari Home - tagihan yang MASIH BELUM lunas doang, dengan tombol Bayar
 * langsung di tiap baris (konfirmasi user 2026-09-14: History jadi murni read-only, aksi bayar
 * dipindah kesini). Berbagi [HistoryViewModel] yang sama dengan History/Payment (satu
 * HistoryGraph) - gak perlu network call baru, tinggal filter `uiState.items` yang sudah dimuat.
 */
@Composable
fun BillsScreen(
    onBackClick: () -> Unit,
    onBayarClick: (LoanHistoryItem) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val unpaidItems = uiState.items.filter { it.status != LoanHistoryStatus.PAID_OFF }
    var selectedItem by remember { mutableStateOf<LoanHistoryItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    selectedItem?.let { item ->
        InstallmentScheduleDialog(item = item, onDismiss = { selectedItem = null })
    }

    // Payment (PaymentScreen) selalu dipush dari sini, BUKAN dari HistoryScreen (lihat
    // KlarNavHost.historyGraph - cuma Bills yang navigate ke Screen.Payment) - jadi di sinilah
    // pendingPaymentMessage (HistoryViewModel, di-share lewat HistoryGraph) harus dikonsumsi,
    // sama pola dengan HistoryScreen. Sebelum fix ini flag itu gak pernah ke-null-kan lagi
    // setelah bayar dari Bills, jadi begitu PaymentScreen dibuka lagi buat cicilan lain,
    // LaunchedEffect di sana langsung ngelihat pendingPaymentMessage masih ada dan langsung
    // nge-pop balik sebelum nasabah sempat apa-apa (bug report 2026-09-14: "gak bisa bayar lagi
    // kalau gak ke Home/back dulu" - itu cuma "fix" karena pop ke Home/back menghancurkan
    // instance HistoryViewModel yang sama, bikin flagnya balik null lagi).
    LaunchedEffect(uiState.pendingPaymentMessage) {
        uiState.pendingPaymentMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumePaymentMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Bills",
                    style = MaterialTheme.typography.titleLarge,
                    color = KlarTeal,
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> LoadingState()
                uiState.loadErrorMessage != null -> ErrorState(
                    message = uiState.loadErrorMessage!!,
                    onRetry = viewModel::loadHistory,
                )
                unpaidItems.isEmpty() -> EmptyState(
                    message = "Gak ada tagihan - semua cicilan kamu sudah lunas!",
                    icon = Icons.Default.CheckCircle,
                )
                else -> Column(modifier = Modifier.fillMaxSize()) {
                    if (uiState.isOffline) {
                        OfflineBanner(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                    }
                    HistoryList(
                        items = unpaidItems,
                        onItemClick = { selectedItem = it },
                        onBayarClick = onBayarClick,
                    )
                }
            }
        }
    }
}
