package com.klarfinance.app.presentation.referral

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klarfinance.app.core.theme.KlarTeal
import com.klarfinance.app.core.theme.KlarTealDark
import com.klarfinance.app.domain.model.ReferralSummary
import com.klarfinance.app.presentation.components.OfflineBanner

/**
 * "Ajak Teman" - kode referral: 1 orang diundang berhasil pinjam >= Rp1jt -> referrer (bukan yang
 * diundang) dapat diskon Rp100rb di pinjaman berikutnya, diterapkan otomatis oleh backend saat
 * request loan (lihat ReferralService di backend, dan RegisterViewModel.onReferralCodeChange
 * untuk sisi "masukin kode orang lain" saat daftar).
 */
@Composable
fun ReferralScreen(
    onBackClick: () -> Unit,
    viewModel: ReferralViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val onShareClick: (String) -> Unit = { code ->
        val shareText = "Yuk daftar KlarFinance pakai kode referral aku: $code — kamu bisa dapat pinjaman, " +
            "dan aku dapat diskon biaya Rp100.000 kalau kamu berhasil pinjam minimal Rp1.000.000!"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Bagikan kode referral"))
    }

    val onCopyClick: (String) -> Unit = { code ->
        clipboardManager.setText(AnnotatedString(code))
    }

    ReferralContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onRetryClick = viewModel::loadSummary,
        onShareClick = onShareClick,
        onCopyClick = onCopyClick,
    )
}

@Composable
private fun ReferralContent(
    uiState: ReferralUiState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onShareClick: (String) -> Unit,
    onCopyClick: (String) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                    text = "Ajak Teman",
                    style = MaterialTheme.typography.titleLarge,
                    color = KlarTeal,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
        },
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = KlarTeal) }

            uiState.loadErrorMessage != null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.loadErrorMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetryClick, colors = ButtonDefaults.buttonColors(containerColor = KlarTeal)) {
                        Text("Coba Lagi")
                    }
                }
            }

            uiState.summary != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                if (uiState.isOffline) {
                    OfflineBanner()
                    Spacer(modifier = Modifier.height(12.dp))
                }
                ReferralHeroCard(summary = uiState.summary)
                Spacer(modifier = Modifier.height(20.dp))
                ReferralCodeCard(
                    code = uiState.summary.referralCode,
                    onShareClick = { onShareClick(uiState.summary.referralCode) },
                    onCopyClick = { onCopyClick(uiState.summary.referralCode) },
                )
                Spacer(modifier = Modifier.height(20.dp))
                ReferralStatsCard(summary = uiState.summary)
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun ReferralHeroCard(summary: ReferralSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(KlarTealDark, KlarTeal)))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Ajak 1 Orang, Dapat Diskon Rp${formatRupiah(summary.rewardAmount)}",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Bagikan kode referral kamu. Begitu temanmu daftar dan berhasil pinjam " +
                "minimal Rp${formatRupiah(summary.minimumLoanAmount)}, kamu langsung dapat diskon biaya " +
                "Rp${formatRupiah(summary.rewardAmount)} di pinjaman kamu berikutnya.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ReferralCodeCard(code: String, onShareClick: () -> Unit, onCopyClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
    ) {
        Text(
            text = "Kode Referral Kamu",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onCopyClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = code.ifBlank { "-" },
                style = MaterialTheme.typography.headlineSmall,
                color = KlarTeal,
                fontWeight = FontWeight.Bold,
            )
            Icon(Icons.Default.ContentCopy, contentDescription = "Salin kode", tint = KlarTeal)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onShareClick,
            enabled = code.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = KlarTeal),
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Bagikan Kode", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ReferralStatsCard(summary: ReferralSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.HowToReg, contentDescription = null, tint = KlarTeal, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Status Undangan",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatItem(label = "Diundang", value = summary.totalInvited.toString())
            StatItem(label = "Berhasil Pinjam", value = summary.totalQualified.toString())
            StatItem(label = "Diskon Tersedia", value = "Rp${formatRupiah(summary.availableDiscountTotal)}")
        }
        if (summary.availableRewardsCount > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(KlarTeal.copy(alpha = 0.1f))
                    .padding(12.dp),
            ) {
                Text(
                    text = "Kamu punya ${summary.availableRewardsCount} diskon yang otomatis " +
                        "dipakai di pinjaman kamu berikutnya.",
                    style = MaterialTheme.typography.bodySmall,
                    color = KlarTeal,
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = KlarTeal, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatRupiah(amount: Long): String =
    amount.toString().reversed().chunked(3).joinToString(".").reversed()
