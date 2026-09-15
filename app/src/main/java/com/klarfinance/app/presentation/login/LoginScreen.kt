package com.klarfinance.app.presentation.login

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klarfinance.app.core.theme.KlarTeal

private val availableCountryCodes = listOf("+62", "+65", "+60", "+1")

@Composable
fun LoginScreen(
    onOtpRequested: (phone: String) -> Unit,
    onAlreadyVerified: (phone: String) -> Unit,
    onNeedsPasswordLogin: (phone: String) -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.otpRequested.collect { phone -> onOtpRequested(phone) }
    }
    LaunchedEffect(Unit) {
        viewModel.alreadyVerified.collect { phone -> onAlreadyVerified(phone) }
    }
    LaunchedEffect(Unit) {
        viewModel.needsPasswordLogin.collect { phone -> onNeedsPasswordLogin(phone) }
    }

    LoginContent(
        uiState = uiState,
        onCountryCodeChange = viewModel::onCountryCodeChange,
        onPhoneNumberChange = viewModel::onPhoneNumberChange,
        onContinueClick = viewModel::onContinueClick,
    )
}

@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onCountryCodeChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onContinueClick: () -> Unit,
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            Text(
                text = "Hi, KlarFams",
                style = MaterialTheme.typography.headlineLarge,
                color = KlarTeal,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enter your phone number to continue to your secure vault.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(32.dp))

            PhoneNumberField(
                countryCode = uiState.countryCode,
                phoneNumber = uiState.phoneNumber,
                onCountryCodeChange = onCountryCodeChange,
                onPhoneNumberChange = onPhoneNumberChange,
            )

            uiState.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onContinueClick,
                enabled = uiState.isContinueEnabled,
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
                    Text("Continue →", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "By continuing, you agree to our Terms and Privacy Policy.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PhoneNumberField(
    countryCode: String,
    phoneNumber: String,
    onCountryCodeChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
    ) {
        Box(
            modifier = Modifier
                .padding(start = 16.dp)
                .clickable { isDropdownExpanded = true },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(countryCode, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select country code")
            }
            DropdownMenu(expanded = isDropdownExpanded, onDismissRequest = { isDropdownExpanded = false }) {
                availableCountryCodes.forEach { code ->
                    DropdownMenuItem(
                        text = { Text(code) },
                        onClick = {
                            onCountryCodeChange(code)
                            isDropdownExpanded = false
                        },
                    )
                }
            }
        }

        TextField(
            value = phoneNumber,
            onValueChange = onPhoneNumberChange,
            placeholder = { Text("Phone Number") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}
