package com.klarfinance.app.presentation.call

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.klarfinance.app.core.theme.KlarError
import com.klarfinance.app.core.theme.KlarFinanceTheme
import com.klarfinance.app.core.theme.KlarTeal
import com.klarfinance.app.core.theme.KlarTealDark
import com.klarfinance.app.core.theme.KlarTealLight
import kotlinx.coroutines.delay

sealed interface CallUiState {
    data object Ringing : CallUiState
    data object Connecting : CallUiState
    data class InCall(val startedAtMillis: Long) : CallUiState
    data class Ended(val message: String) : CallUiState
}

private val AcceptGreen = Color(0xFF2E9E5B)

@Composable
fun DeskcallCallScreen(
    callerName: String,
    state: CallUiState,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onHangUp: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KlarTealDark)
            .systemBarsPadding()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        Box(
            modifier = Modifier
                .size(112.dp)
                .background(KlarTealLight, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = KlarTeal, modifier = Modifier.size(56.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(callerName, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(statusText(state), style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f))
        if (state is CallUiState.InCall) {
            Spacer(Modifier.height(4.dp))
            CallTimer(state.startedAtMillis)
        }

        Spacer(Modifier.weight(1f))

        when (state) {
            CallUiState.Ringing -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                CallActionButton(Icons.Filled.CallEnd, "Tolak", KlarError, onDecline)
                CallActionButton(Icons.Filled.Call, "Angkat", AcceptGreen, onAccept)
            }
            CallUiState.Connecting -> CircularProgressIndicator(color = Color.White)
            is CallUiState.InCall -> CallActionButton(Icons.Filled.CallEnd, "Akhiri", KlarError, onHangUp)
            is CallUiState.Ended -> Unit
        }
    }
}

private fun statusText(state: CallUiState): String = when (state) {
    CallUiState.Ringing -> "Panggilan suara masuk"
    CallUiState.Connecting -> "Menyambungkan..."
    is CallUiState.InCall -> "Terhubung"
    is CallUiState.Ended -> state.message
}

@Composable
private fun CallTimer(startedAtMillis: Long) {
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(startedAtMillis) {
        while (true) {
            elapsedSeconds = (System.currentTimeMillis() - startedAtMillis) / 1_000L
            delay(1_000L)
        }
    }
    Text(
        "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60),
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.8f),
    )
}

@Composable
private fun CallActionButton(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(72.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = color, contentColor = Color.White),
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview
@Composable
private fun RingingPreview() {
    KlarFinanceTheme {
        DeskcallCallScreen("KlarFinance", CallUiState.Ringing, {}, {}, {})
    }
}
