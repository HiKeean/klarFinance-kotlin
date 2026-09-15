package com.klarfinance.app.presentation.register.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klarfinance.app.core.theme.KlarTeal
import com.klarfinance.app.domain.model.LocationOption
import com.klarfinance.app.presentation.register.RegisterViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfileScreen(
    viewModel: RegisterViewModel,
    onRegistered: () -> Unit,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.registerCompleted.collect { onRegistered() }
    }

    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
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
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Complete Your Profile",
                style = MaterialTheme.typography.headlineSmall,
                color = KlarTeal,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Please provide accurate information to secure your account.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionLabel("PERSONAL DETAILS")
            KlarTextField(
                label = "Full Name (as per ID)",
                value = uiState.fullName,
                onValueChange = viewModel::onFullNameChange,
                placeholder = "Enter your full name",
            )
            Spacer(modifier = Modifier.height(16.dp))
            KlarTextField(
                label = "NIK (16 Digits)",
                value = uiState.nik,
                onValueChange = viewModel::onNikChange,
                placeholder = "16-digit ID number",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Spacer(modifier = Modifier.height(16.dp))
            KlarTextField(
                label = "Date of Birth",
                value = uiState.dob?.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")) ?: "",
                onValueChange = {},
                placeholder = "mm/dd/yyyy",
                trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                onClick = { showDatePicker = true },
            )

            SectionLabel("ACCOUNT SECURITY")
            KlarTextField(
                label = "Email Address",
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                placeholder = "name@example.com",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            Spacer(modifier = Modifier.height(16.dp))
            KlarTextField(
                label = "Secure Password",
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                placeholder = "At least 8 characters",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = viewModel::onTogglePasswordVisibility) {
                        Icon(
                            imageVector = if (uiState.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password visibility",
                        )
                    }
                },
            )

            SectionLabel("RESIDENTIAL ADDRESS")
            KlarTextField(
                label = "Full Street Address",
                value = uiState.address,
                onValueChange = viewModel::onAddressChange,
                placeholder = "Enter your full address",
                singleLine = false,
                minLines = 3,
            )
            Spacer(modifier = Modifier.height(16.dp))
            LocationDropdownField(
                label = "Province",
                placeholder = "Select Province",
                icon = Icons.Default.Public,
                selectedName = uiState.province?.name,
                options = uiState.provinceOptions,
                enabled = true,
                isLoading = uiState.isLoadingRegions && uiState.provinceOptions.isEmpty(),
                disabledHint = null,
                onOptionSelected = viewModel::onProvinceSelected,
            )
            Spacer(modifier = Modifier.height(16.dp))
            LocationDropdownField(
                label = "Regency / City",
                placeholder = "Select Regency",
                icon = Icons.Default.LocationCity,
                selectedName = uiState.regency?.name,
                options = uiState.regencyOptions,
                enabled = uiState.province != null,
                isLoading = uiState.isLoadingRegions && uiState.province != null && uiState.regencyOptions.isEmpty(),
                disabledHint = "Select a province first",
                onOptionSelected = viewModel::onRegencySelected,
            )
            Spacer(modifier = Modifier.height(16.dp))
            LocationDropdownField(
                label = "District",
                placeholder = "Select District",
                icon = Icons.Default.Map,
                selectedName = uiState.district?.name,
                options = uiState.districtOptions,
                enabled = uiState.regency != null,
                isLoading = uiState.isLoadingRegions && uiState.regency != null && uiState.districtOptions.isEmpty(),
                disabledHint = "Select a regency first",
                onOptionSelected = viewModel::onDistrictSelected,
            )
            Spacer(modifier = Modifier.height(16.dp))
            LocationDropdownField(
                label = "Village / Kelurahan",
                placeholder = "Select Village",
                icon = Icons.Default.Home,
                selectedName = uiState.village?.name,
                options = uiState.villageOptions,
                enabled = uiState.district != null,
                isLoading = uiState.isLoadingRegions && uiState.district != null && uiState.villageOptions.isEmpty(),
                disabledHint = "Select a district first",
                onOptionSelected = viewModel::onVillageSelected,
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionLabel("REFERRAL (OPTIONAL)")
            KlarTextField(
                label = "Referral Code",
                value = uiState.referralCode,
                onValueChange = viewModel::onReferralCodeChange,
                placeholder = "Enter a friend's referral code, if you have one",
            )

            uiState.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = viewModel::onSubmitProfileClick,
                enabled = uiState.isProfileFormValid,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KlarTeal),
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Profile", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Your data is secured with bank-grade encryption.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let(viewModel::onDobSelected)
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = KlarTeal,
    )
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun KlarTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder) },
                singleLine = singleLine,
                minLines = minLines,
                enabled = onClick == null,
                keyboardOptions = keyboardOptions,
                visualTransformation = visualTransformation,
                trailingIcon = trailingIcon,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    disabledTextColor = MaterialTheme.colorScheme.onBackground,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LocationDropdownField(
    label: String,
    placeholder: String,
    icon: ImageVector,
    selectedName: String?,
    options: List<LocationOption>,
    enabled: Boolean,
    isLoading: Boolean,
    disabledHint: String?,
    onOptionSelected: (LocationOption) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var fieldWidth by remember { mutableStateOf(0) }
    val chevronRotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "chevronRotation")
    val filteredOptions = remember(options, query) {
        if (query.isBlank()) options else options.filter { it.name.contains(query, ignoreCase = true) }
    }

    fun closeMenu() {
        expanded = false
        query = ""
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { fieldWidth = it.size.width }
                .border(
                    width = if (expanded) 1.5.dp else 1.dp,
                    color = if (expanded) KlarTeal else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(14.dp),
                )
                .clickable(enabled = enabled) { expanded = true }
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selectedName != null) KlarTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = selectedName ?: placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selectedName != null) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = KlarTeal)
                } else {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(chevronRotation),
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = ::closeMenu,
                modifier = Modifier.width(with(LocalDensity.current) { fieldWidth.toDp() }),
            ) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Search $label") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
                if (filteredOptions.isEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (options.isEmpty()) "No options available" else "No matches for \"$query\"",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = {},
                        enabled = false,
                    )
                } else {
                    // Plain Column + verticalScroll on purpose, NOT LazyColumn: DropdownMenu's
                    // expand/collapse animation queries intrinsic height of its content, which
                    // crashes ("SubcomposeLayout... intrinsic measurements... not supported")
                    // if a lazy list is a descendant. Option lists here are short enough
                    // (already filtered, max a few dozen rows) that eager composition is fine.
                    Column(modifier = Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState())) {
                        filteredOptions.forEach { option ->
                            val isSelected = option.name == selectedName
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.name,
                                        color = if (isSelected) KlarTeal else MaterialTheme.colorScheme.onBackground,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                },
                                trailingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, tint = KlarTeal, modifier = Modifier.size(18.dp)) }
                                } else null,
                                colors = MenuDefaults.itemColors(
                                    textColor = if (isSelected) KlarTeal else MaterialTheme.colorScheme.onBackground,
                                ),
                                onClick = {
                                    onOptionSelected(option)
                                    closeMenu()
                                },
                            )
                        }
                    }
                }
            }
        }
        if (!enabled && disabledHint != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = disabledHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

