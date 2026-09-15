package com.klarfinance.app.presentation.allfeatures

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klarfinance.app.core.theme.KlarTeal
import com.klarfinance.app.domain.model.AccountState
import com.klarfinance.app.domain.model.FeatureCategoryKey
import com.klarfinance.app.presentation.components.AppBottomBar
import com.klarfinance.app.presentation.components.SearchField
import kotlinx.coroutines.launch

private data class CatalogFeature(val label: String, val icon: ImageVector)
private data class CatalogCategory(val key: FeatureCategoryKey, val title: String, val features: List<CatalogFeature>)

// Data KHUSUS halaman ini (bukan reuse featureCategories punya HomeScreen) - konfirmasi user:
// katalog lengkap boleh punya lebih banyak item per kategori daripada versi ringkas di
// ExploreFeaturesCard (Home), termasuk kategori "Transfer & terima" yang sudah dihapus dari
// Home tapi tetap muncul di sini. Semua item selain Transjakarta masih placeholder/belum ada
// halaman real (sama seperti ExploreFeaturesCard). [key] dipakai buat deep-link+auto-scroll dari
// panah section Home (lihat [FeatureCategoryKey]), BUKAN untuk cocokin title string.
private val allFeatureCategories = listOf(
    CatalogCategory(
        key = FeatureCategoryKey.TRANSFER,
        title = "Transfer & terima",
        features = listOf(
            CatalogFeature("Transfer gratis", Icons.Default.Send),
            CatalogFeature("Transfer luar negeri", Icons.Default.Public),
            CatalogFeature("Split bill", Icons.AutoMirrored.Filled.CallSplit),
            CatalogFeature("Hadiah", Icons.Default.CardGiftcard),
        ),
    ),
    CatalogCategory(
        key = FeatureCategoryKey.PEMBAYARAN,
        title = "Pembayaran",
        features = listOf(
            CatalogFeature("Tagihan saya", Icons.Default.ReceiptLong),
            CatalogFeature("Top up kartu", Icons.Default.AddCard),
            CatalogFeature("PLN", Icons.Default.Bolt),
            CatalogFeature("BPJS", Icons.Default.Shield),
            CatalogFeature("PDAM", Icons.Default.WaterDrop),
            CatalogFeature("Internet", Icons.Default.Wifi),
            CatalogFeature("eSIM", Icons.Default.SimCard),
            CatalogFeature("Transjakarta", Icons.Default.DirectionsBus),
        ),
    ),
    CatalogCategory(
        key = FeatureCategoryKey.PROMO,
        title = "Promo",
        features = listOf(
            CatalogFeature("Voucher saya", Icons.Default.ConfirmationNumber),
            CatalogFeature("Kartu gosok", Icons.Default.Style),
            CatalogFeature("Rewards", Icons.Default.Star),
            CatalogFeature("Google Play", Icons.Default.PlayArrow),
            CatalogFeature("Belanja", Icons.Default.ShoppingBag),
        ),
    ),
    CatalogCategory(
        key = FeatureCategoryKey.GAMES_HIBURAN,
        title = "Games & hiburan",
        features = listOf(
            CatalogFeature("Ruby Zone", Icons.Default.Diamond),
            CatalogFeature("Treasure Hunt", Icons.Default.Explore),
            CatalogFeature("Noice", Icons.Default.Headphones),
            CatalogFeature("Tiket Bioskop", Icons.Default.LocalMovies),
        ),
    ),
)

/**
 * "Semua fitur" - katalog penuh. Tujuan tab bottom-nav "Loans" (2026-09-14, sebelumnya cuma
 * quick action "More" di Home) DAN section-arrow di Home's ExploreFeaturesCard (auto-scroll ke
 * section terkait via [scrollToCategory]). accountState di-resolve sendiri lewat
 * [AllFeaturesViewModel] (bukan nav arg) - banyak entry point sekarang, gak realistis semuanya
 * ngethread accountState dengan benar.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllFeaturesScreen(
    onHomeClick: () -> Unit,
    onAccountClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onTransjakartaClick: () -> Unit,
    scrollToCategory: FeatureCategoryKey? = null,
    viewModel: AllFeaturesViewModel = hiltViewModel(),
) {
    val accountState by viewModel.accountState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var showPendingDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val sectionRequesters = remember { allFeatureCategories.associate { it.key to BringIntoViewRequester() } }

    // Cuma 2 cabang (bukan 3 kayak HomeScreen.onLockedFeatureClick) - GUEST gak pernah sampai
    // ke screen ini (semua entry point sudah gate di GUEST -> Login sebelum navigate kesini).
    val onLockedFeatureClick: () -> Unit = {
        if (accountState == AccountState.PENDING_APPLICATION) {
            showPendingDialog = true
        } else {
            scope.launch { snackbarHostState.showSnackbar("Fitur ini akan segera hadir") }
        }
    }

    // Transjakarta satu-satunya item yang sudah punya halaman real (sama seperti HomeScreen).
    val onTransjakartaTap: () -> Unit = {
        if (accountState == AccountState.ACTIVE) onTransjakartaClick() else onLockedFeatureClick()
    }

    LaunchedEffect(scrollToCategory) {
        scrollToCategory?.let { key -> sectionRequesters[key]?.bringIntoView() }
    }

    if (showPendingDialog) {
        AlertDialog(
            onDismissRequest = { showPendingDialog = false },
            icon = { Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = KlarTeal) },
            title = { Text("Pengajuan Masih Diproses") },
            text = { Text("Pengajuan plafond kamu masih dalam tahap review, silakan tunggu 2-3 hari kerja.") },
            confirmButton = { TextButton(onClick = { showPendingDialog = false }) { Text("Mengerti") } },
        )
    }

    val trimmedQuery = searchQuery.trim()
    val visibleCategories = if (trimmedQuery.isBlank()) {
        allFeatureCategories
    } else {
        allFeatureCategories
            .map { category -> category.copy(features = category.features.filter { it.label.contains(trimmedQuery, ignoreCase = true) }) }
            .filter { it.features.isNotEmpty() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Sama pola dengan HistoryScreen/AccountScreen - screen tab utama, bukan modal, jadi
            // gak ada panah back lagi (diganti bottom nav, tab Home yang jadi jalan keluarnya).
            Text(
                text = "Loans",
                style = MaterialTheme.typography.titleLarge,
                color = KlarTeal,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            )
        },
        bottomBar = {
            AppBottomBar(
                hasAccount = true,
                activeTab = "Loans",
                onLoginRequested = {},
                onHomeClick = onHomeClick,
                onAccountClick = onAccountClick,
                onLockedTabClick = {},
                onHistoryClick = onHistoryClick,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            SearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Cari layanan",
            )
            Spacer(modifier = Modifier.height(20.dp))

            if (visibleCategories.isEmpty()) {
                Text(
                    text = "Gak ada layanan yang cocok",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }

            visibleCategories.forEachIndexed { index, category ->
                if (index > 0) Spacer(modifier = Modifier.height(24.dp))
                // sectionRequesters selalu punya entry buat setiap key di sini - dibangun dari
                // allFeatureCategories yang sama (visibleCategories cuma filter search-nya,
                // key-nya gak pernah hilang), jadi aman non-null-assert daripada nge-remember
                // fallback di dalam loop (violates Compose call-site stability).
                Column(
                    modifier = Modifier.bringIntoViewRequester(sectionRequesters.getValue(category.key)),
                ) {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FeatureGrid(
                        features = category.features,
                        onClick = onLockedFeatureClick,
                        onTransjakartaClick = onTransjakartaTap,
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** 4 kolom per baris, wrap ke baris baru - beda dari ExploreFeaturesCard punya Home (selalu
 * pas 4 item per kategori) karena di sini kategori bisa punya lebih dari 4 item (Pembayaran
 * misalnya 8). Baris terakhir yang gak penuh diganjel spacer selebar bubble icon biar tetap
 * rata kiri (bukan stretch ngikut SpaceBetween). */
@Composable
private fun FeatureGrid(features: List<CatalogFeature>, onClick: () -> Unit, onTransjakartaClick: () -> Unit) {
    features.chunked(4).forEach { rowFeatures ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            rowFeatures.forEach { feature ->
                val click = if (feature.label == "Transjakarta") onTransjakartaClick else onClick
                FeatureIconAction(label = feature.label, icon = feature.icon, onClick = click)
            }
            repeat(4 - rowFeatures.size) {
                Spacer(modifier = Modifier.width(72.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FeatureIconAction(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
