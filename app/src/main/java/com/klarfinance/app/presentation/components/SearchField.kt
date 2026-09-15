package com.klarfinance.app.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Search bar reusable dipakai lintas fitur (bukan cuma Transjakarta - konfirmasi user "search
 * itu di kotlin gua harus reusable component ya", dan lebih umum "kalau bisa semuanya reusable
 * component", lihat kotlin-reusable-components knowledge) - taruh di sini, bukan lokal per-screen,
 * setiap kali butuh search-as-you-type ke depan.
 *
 * Beda dari `KlarTextField` (private, `CompleteProfileScreen.kt`) yang berupa form field
 * ber-label - ini murni search bar (ikon kaca pembesar, tanpa label, tombol clear muncul begitu
 * ada isi).
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Cari...",
) {
    val shape = RoundedCornerShape(14.dp)
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = shape),
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = shape,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Hapus")
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}
