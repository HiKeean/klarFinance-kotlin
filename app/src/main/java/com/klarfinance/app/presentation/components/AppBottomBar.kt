package com.klarfinance.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private data class BottomNavItem(val label: String, val icon: ImageVector)

private val bottomNavItems = listOf(
    BottomNavItem("Home", Icons.Default.Home),
    BottomNavItem("Loans", Icons.Default.AccountBalance),
    BottomNavItem("History", Icons.Default.History),
    BottomNavItem("Account", Icons.Default.Person),
)

/**
 * Shared bottom nav, used by [com.klarfinance.app.presentation.home.HomeScreen],
 * [com.klarfinance.app.presentation.history.HistoryScreen],
 * [com.klarfinance.app.presentation.account.AccountScreen], and
 * [com.klarfinance.app.presentation.allfeatures.AllFeaturesScreen] ("Loans" tab) - each screen is
 * its own nav destination (not tabs nested under one Scaffold), so this is rendered independently
 * by all four with [activeTab] telling it which one is "selected" (tinted primary, disabled -
 * tapping the tab you're already on is a no-op).
 */
@Composable
fun AppBottomBar(
    hasAccount: Boolean,
    activeTab: String,
    onLoginRequested: () -> Unit,
    onHomeClick: () -> Unit,
    onAccountClick: () -> Unit,
    onLockedTabClick: () -> Unit,
    // Default ke onLockedTabClick - History/Loans belum py halaman real waktu Home/Account pertama
    // ditulis (falls back ke dialog/snackbar generic), sekarang genuinely dipakai buat navigasi
    // ke HistoryScreen/AllFeaturesScreen di call site yang sudah punya konteks buat itu.
    onHistoryClick: () -> Unit = onLockedTabClick,
    onLoansClick: () -> Unit = onLockedTabClick,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        bottomNavItems.forEach { item ->
            val isSelected = item.label == activeTab
            val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            val onClick = when {
                item.label == "Home" -> onHomeClick
                !hasAccount -> onLoginRequested
                item.label == "Account" -> onAccountClick
                item.label == "History" -> onHistoryClick
                item.label == "Loans" -> onLoansClick
                else -> onLockedTabClick
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(enabled = !isSelected, onClick = onClick),
            ) {
                Icon(imageVector = item.icon, contentDescription = item.label, tint = tint)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = item.label, style = MaterialTheme.typography.bodyMedium, color = tint)
            }
        }
    }
}
