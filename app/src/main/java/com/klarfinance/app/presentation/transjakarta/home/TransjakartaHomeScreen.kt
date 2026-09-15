package com.klarfinance.app.presentation.transjakarta.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klarfinance.app.core.theme.KlarTeal
import com.klarfinance.app.domain.model.TransjakartaTicket
import com.klarfinance.app.presentation.components.OfflineBanner
import com.klarfinance.app.presentation.components.PrimaryButton
import com.klarfinance.app.presentation.components.QrCodeImage
import com.klarfinance.app.presentation.history.formatDate
import com.klarfinance.app.presentation.loan.groupThousands
import com.klarfinance.app.presentation.transjakarta.MAX_TICKET_QTY
import com.klarfinance.app.presentation.transjakarta.TransjakartaPurchaseUiState
import com.klarfinance.app.presentation.transjakarta.TransjakartaPurchaseViewModel

/**
 * Step 1 dari transjakartaGraph - beli tiket generik (qty, gak ada pilih halte/rute - GTFS+maps
 * dibuang, konfirmasi user) + riwayat tiket (used/belum, tap badge buat toggle - MOCKUP, gak ada
 * validasi gate beneran, konfirmasi user "dari pencet used atau belumnya saja").
 */
@Composable
fun TransjakartaHomeScreen(
    onBackClick: () -> Unit,
    onBuyClick: () -> Unit,
    viewModel: TransjakartaPurchaseViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TransjakartaHomeContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onQtyChange = viewModel::onQtyChange,
        onBuyClick = onBuyClick,
        onToggleUsed = viewModel::onToggleUsed,
    )
}

@Composable
private fun TransjakartaHomeContent(
    uiState: TransjakartaPurchaseUiState,
    onBackClick: () -> Unit,
    onQtyChange: (Int) -> Unit,
    onBuyClick: () -> Unit,
    onToggleUsed: (String) -> Unit,
) {
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
                    text = "Transjakarta",
                    style = MaterialTheme.typography.titleLarge,
                    color = KlarTeal,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BuyTicketCard(qty = uiState.qty, onQtyChange = onQtyChange, onBuyClick = onBuyClick)
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = "Riwayat Tiket",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (uiState.isOffline && uiState.tickets.isNotEmpty()) {
                item {
                    OfflineBanner()
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            when {
                uiState.isLoadingTickets -> item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = KlarTeal)
                    }
                }

                uiState.ticketsErrorMessage != null -> item {
                    Text(
                        uiState.ticketsErrorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }

                uiState.tickets.isEmpty() -> item {
                    Text(
                        "Belum ada tiket yang dibeli",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }

                else -> items(uiState.tickets, key = { it.ticketId }) { ticket ->
                    TicketRow(ticket = ticket, onToggleUsed = { onToggleUsed(ticket.ticketCode) })
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun BuyTicketCard(qty: Int, onQtyChange: (Int) -> Unit, onBuyClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Text(
            text = "Beli Tiket Transjakarta",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Rp3.500 / tiket",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Jumlah", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            Row(verticalAlignment = Alignment.CenterVertically) {
                QtyButton(icon = Icons.Default.Remove, enabled = qty > 1, onClick = { onQtyChange(qty - 1) })
                Text(
                    text = qty.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center,
                )
                QtyButton(icon = Icons.Default.Add, enabled = qty < MAX_TICKET_QTY, onClick = { onQtyChange(qty + 1) })
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Catatan: tidak berlaku untuk rute jauh (mis. Bogor - PIK 2, Blok M - PIK 2).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))
        PrimaryButton(text = "Bayar (Rp${groupThousands((qty * 3500L).toString())})", onClick = onBuyClick)
    }
}

@Composable
private fun QtyButton(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (enabled) KlarTeal.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) KlarTeal else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun TicketRow(ticket: TransjakartaTicket, onToggleUsed: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { expanded = !expanded }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tiket Transjakarta",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "${formatDate(ticket.purchasedAt)} · Rp${groupThousands(ticket.amount.toString())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            UsedBadge(used = ticket.used, onClick = onToggleUsed)
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                QrCodeImage(content = ticket.ticketCode, sizeDp = 180)
            }
        }
    }
}

@Composable
private fun UsedBadge(used: Boolean, onClick: () -> Unit) {
    val color = if (used) Color(0xFF2E7D32) else Color(0xFFB26A00)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector = if (used) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (used) "Sudah Digunakan" else "Belum Digunakan",
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}
