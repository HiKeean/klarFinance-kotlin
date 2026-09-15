package com.klarfinance.app.presentation.transjakarta.confirm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klarfinance.app.core.theme.KlarTeal
import com.klarfinance.app.domain.model.TransjakartaTicket
import com.klarfinance.app.presentation.components.PrimaryButton
import com.klarfinance.app.presentation.components.TransactionPasswordDialog
import com.klarfinance.app.presentation.transjakarta.TransjakartaPurchaseUiState
import com.klarfinance.app.presentation.transjakarta.TransjakartaPurchaseViewModel
import com.klarfinance.app.presentation.loan.groupThousands

/** Step 2 dari transjakartaGraph - halaman baru (bukan dialog, konfirmasi user) buat rincian
 * harga+bunga + step-up auth (sama pola QrisAmountScreen), lalu sukses -> [onPurchased] (parent
 * yang urus navigate balik ke Home + keluar dari graph). */
@Composable
fun TransjakartaConfirmScreen(
    onBackClick: () -> Unit,
    onPurchased: (List<TransjakartaTicket>) -> Unit,
    viewModel: TransjakartaPurchaseViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity

    LaunchedEffect(uiState.purchasedTickets) {
        uiState.purchasedTickets?.let(onPurchased)
    }

    TransjakartaConfirmContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onSubmitClick = { activity?.let(viewModel::onSubmitClick) },
        onPasswordChange = viewModel::onPasswordInputChange,
        onPasswordConfirm = viewModel::onPasswordConfirm,
        onPasswordDismiss = viewModel::onPasswordConfirmDismiss,
    )
}

@Composable
private fun TransjakartaConfirmContent(
    uiState: TransjakartaPurchaseUiState,
    onBackClick: () -> Unit,
    onSubmitClick: () -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordConfirm: () -> Unit,
    onPasswordDismiss: () -> Unit,
) {
    if (uiState.requiresPasswordConfirm) {
        TransactionPasswordDialog(
            password = uiState.passwordInput,
            onPasswordChange = onPasswordChange,
            onConfirm = onPasswordConfirm,
            onDismiss = onPasswordDismiss,
            isVerifying = uiState.isVerifyingPassword,
            errorMessage = uiState.passwordError,
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Konfirmasi Pembayaran",
                    style = MaterialTheme.typography.titleLarge,
                    color = KlarTeal,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
        },
        // Tombol di bottomBar (BUKAN di bawah Column konten pakai Spacer.weight(1f)) - itu yang
        // bikin bug "tombol kepotong 1/2" (konfirmasi user): Column tanpa scroll + weight-push
        // ke bawah bikin tombol ikut ke-clip begitu konten lebih tinggi dari viewport. bottomBar
        // SELALU dapat jatah tempatnya sendiri dari Scaffold, gak akan pernah ke-potong.
        bottomBar = {
            PrimaryButton(
                text = "Bayar Sekarang",
                loadingText = "Memproses...",
                isLoading = uiState.isSubmitting,
                onClick = onSubmitClick,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp).navigationBarsPadding(),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tiket Transjakarta",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${uiState.qty} tiket",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier.fillMaxWidth().background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(16.dp),
                ).padding(16.dp),
            ) {
                PriceRow(label = "Harga Tiket (${uiState.qty} x Rp${groupThousands(uiState.ticketPrice.toString())})", value = uiState.totalPrincipal)
                Spacer(modifier = Modifier.height(8.dp))
                PriceRow(label = "Bunga (1 bulan)", value = uiState.interestFee)
                Spacer(modifier = Modifier.height(8.dp))
                PriceRow(label = "Total Ditagihkan", value = uiState.totalToPay, emphasized = true)
            }

            Text(
                text = "Ditagihkan bersama pembelian Transjakarta lainnya bulan ini, jatuh tempo " +
                    "tanggal 25 bulan depan sebagai 1 tagihan \"Transportasi\".",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )

            if (uiState.submitErrorMessage != null) {
                Text(
                    text = uiState.submitErrorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PriceRow(label: String, value: Long, emphasized: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal,
        )
        Text(
            text = "Rp${groupThousands(value.toString())}",
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (emphasized) KlarTeal else MaterialTheme.colorScheme.onBackground,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
