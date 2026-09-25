package com.klarfinance.app.presentation.otp

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klarfinance.app.core.theme.KlarTeal
import com.klarfinance.app.domain.model.OtpChannel
import com.klarfinance.app.presentation.components.OtpInputField

@Composable
fun OtpVerificationScreen(
    onBackClick: () -> Unit,
    onVerified: (phone: String) -> Unit,
    onNeedsPasswordLogin: (phone: String) -> Unit,
    viewModel: OtpViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findActivity()

    LaunchedEffect(uiState.smsSendPending) {
        if (uiState.smsSendPending && activity != null) viewModel.sendSmsCode(activity)
    }
    LaunchedEffect(Unit) {
        viewModel.otpVerified.collect { phone -> onVerified(phone) }
    }
    LaunchedEffect(Unit) {
        viewModel.needsPasswordLogin.collect { phone -> onNeedsPasswordLogin(phone) }
    }

    OtpVerificationContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onOtpChange = viewModel::onOtpChange,
        onVerifyClick = viewModel::onVerifyClick,
        onResendClick = viewModel::onResendClick,
        onSwitchToSmsClick = viewModel::onSwitchToSmsClick,
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun OtpVerificationContent(
    uiState: OtpUiState,
    onBackClick: () -> Unit,
    onOtpChange: (String) -> Unit,
    onVerifyClick: () -> Unit,
    onResendClick: () -> Unit,
    onSwitchToSmsClick: () -> Unit,
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
                    text = "KlarFinance",
                    style = MaterialTheme.typography.titleLarge,
                    color = KlarTeal,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                IconButton(onClick = { /* profile not yet implemented */ }) {
                    Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = KlarTeal)
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Verify Your Number",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (uiState.channel == OtpChannel.SMS) {
                    "Enter the 6-digit code sent via SMS"
                } else {
                    "Enter the 6-digit code sent to your WhatsApp"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            OtpInputField(
                value = uiState.otp,
                onValueChange = onOtpChange,
            )

            Spacer(modifier = Modifier.height(20.dp))

            ResendCodeRow(
                secondsRemaining = uiState.resendSecondsRemaining,
                canResend = uiState.canResend,
                onResendClick = onResendClick,
            )

            if (uiState.canSwitchToSms) {
                TextButton(onClick = onSwitchToSmsClick) {
                    Text(
                        "Didn't get the code? Send via SMS",
                        color = KlarTeal,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            uiState.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }

            if (uiState.isVerified) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = KlarTeal)
                    Text(
                        text = "  Number verified",
                        style = MaterialTheme.typography.bodyLarge,
                        color = KlarTeal,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onVerifyClick,
                enabled = uiState.canVerify && !uiState.isVerified,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KlarTeal),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = if (uiState.isVerified) "Verified" else "Verify",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ResendCodeRow(
    secondsRemaining: Int,
    canResend: Boolean,
    onResendClick: () -> Unit,
) {
    if (canResend) {
        TextButton(onClick = onResendClick) {
            Text("Resend Code", color = KlarTeal, style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        val minutes = secondsRemaining / 60
        val seconds = secondsRemaining % 60
        Text(
            text = "Resend Code  %02d:%02d".format(minutes, seconds),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
