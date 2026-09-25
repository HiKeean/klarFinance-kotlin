package com.klarfinance.app.presentation.account

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klarfinance.app.core.location.LocationPermissionHelper
import com.klarfinance.app.core.theme.KlarTeal
import com.klarfinance.app.core.theme.KlarTealDark
import com.klarfinance.app.domain.model.AccountProfile
import com.klarfinance.app.presentation.components.AppBottomBar
import com.klarfinance.app.presentation.components.OfflineBanner
import kotlinx.coroutines.launch

/**
 * "Pengaturan & Keamanan" - restyled after a GoPay settings-page reference the user shared,
 * rebranded to KlarFinance (teal theme instead of GoPay's dark/green, "klar" instead of
 * "gopay", the app's own "K" logo mark instead of GoPay's). A few reference sections were
 * deliberately dropped rather than reskinned as-is - see the knowledge note for why (mainly:
 * they'd require fabricating data/partnerships KlarFinance doesn't have).
 */
@Composable
fun AccountScreen(
    onHomeClick: () -> Unit,
    onReferralClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onLoansClick: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as? FragmentActivity
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loggedOut.collect { onLoggedOut() }
    }
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    val onComingSoonClick: () -> Unit = {
        scope.launch { snackbarHostState.showSnackbar("Fitur ini akan segera hadir") }
    }

    // Background location (Q+) is requested as its OWN isolated call, separate from
    // foreground - combining them in one request always auto-denies background on API 30+, see
    // LocationPermissionHelper. Denying background here still leaves foreground granted, which
    // is enough to turn the toggle on (best-effort - see AccountViewModel.onLocationConsentEnabled).
    val backgroundLocationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.onLocationConsentEnabled() }

    val foregroundLocationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!granted) {
            viewModel.onLocationPermissionDenied()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !LocationPermissionHelper.hasBackgroundLocationPermission(context)
        ) {
            backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            viewModel.onLocationConsentEnabled()
        }
    }

    val onLocationConsentToggle: (Boolean) -> Unit = { enabled ->
        if (!enabled) {
            viewModel.onLocationConsentDisabled()
        } else if (LocationPermissionHelper.hasForegroundLocationPermission(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                !LocationPermissionHelper.hasBackgroundLocationPermission(context)
            ) {
                backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                viewModel.onLocationConsentEnabled()
            }
        } else {
            foregroundLocationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }

    AccountContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onHomeClick = onHomeClick,
        onReferralClick = onReferralClick,
        onHistoryClick = onHistoryClick,
        onLoansClick = onLoansClick,
        onRetryClick = viewModel::loadProfile,
        onToggleSecurityChecklist = viewModel::onToggleSecurityChecklist,
        onEnableFingerprintClick = { activity?.let(viewModel::onEnableFingerprintClick) },
        onToggleChangePassword = viewModel::onToggleChangePassword,
        onOldPasswordChange = viewModel::onOldPasswordChange,
        onNewPasswordChange = viewModel::onNewPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onSubmitChangePassword = viewModel::onSubmitChangePassword,
        onComingSoonClick = onComingSoonClick,
        onLocationConsentToggle = onLocationConsentToggle,
        onLogoutClick = viewModel::onLogoutClick,
    )
}

@Composable
private fun AccountContent(
    uiState: AccountUiState,
    snackbarHostState: SnackbarHostState,
    onHomeClick: () -> Unit,
    onReferralClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onLoansClick: () -> Unit,
    onRetryClick: () -> Unit,
    onToggleSecurityChecklist: () -> Unit,
    onEnableFingerprintClick: () -> Unit,
    onToggleChangePassword: () -> Unit,
    onOldPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmitChangePassword: () -> Unit,
    onComingSoonClick: () -> Unit,
    onLocationConsentToggle: (Boolean) -> Unit,
    onLogoutClick: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // No back arrow here on purpose - this screen keeps the persistent bottom nav
            // (its Home tab is how you leave), see AppBottomBar below. statusBarsPadding()
            // keeps the title clear of the status bar/camera cutout - a plain Row placed
            // directly in Scaffold's topBar slot doesn't get that automatically the way a
            // real TopAppBar would (this app enables edge-to-edge in MainActivity).
            Text(
                text = "Pengaturan & Keamanan",
                style = MaterialTheme.typography.titleLarge,
                color = KlarTeal,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center,
            )
        },
        bottomBar = {
            AppBottomBar(
                hasAccount = true,
                activeTab = "Account",
                onLoginRequested = {},
                onHomeClick = onHomeClick,
                onAccountClick = {},
                onLockedTabClick = onComingSoonClick,
                onHistoryClick = onHistoryClick,
                onLoansClick = onLoansClick,
            )
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

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                if (uiState.isOffline) {
                    OfflineBanner()
                    Spacer(modifier = Modifier.height(12.dp))
                }
                uiState.profile?.let { profile ->
                    ProfileHeaderCard(profile = profile, onEditClick = onComingSoonClick)
                    Spacer(modifier = Modifier.height(16.dp))
                    SecurityStatusCard(
                        profile = profile,
                        isChecklistExpanded = uiState.isSecurityChecklistExpanded,
                        isFingerprintEnabled = uiState.isFingerprintEnabled,
                        isEnablingFingerprint = uiState.isEnablingFingerprint,
                        onToggleChecklist = onToggleSecurityChecklist,
                        onVerifyEmailClick = onComingSoonClick,
                        onEnableFingerprintClick = onEnableFingerprintClick,
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Pengaturan Keamanan Lainnya",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(12.dp))
                SettingsListCard(
                    uiState = uiState,
                    onToggleChangePassword = onToggleChangePassword,
                    onOldPasswordChange = onOldPasswordChange,
                    onNewPasswordChange = onNewPasswordChange,
                    onConfirmPasswordChange = onConfirmPasswordChange,
                    onSubmitChangePassword = onSubmitChangePassword,
                    onReferralClick = onReferralClick,
                    onComingSoonClick = onComingSoonClick,
                )

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Lokasi & Privasi",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(12.dp))
                LocationConsentCard(
                    isEnabled = uiState.isLocationConsentGiven,
                    isUpdating = uiState.isUpdatingLocationConsent,
                    onToggle = onLocationConsentToggle,
                )

                Spacer(modifier = Modifier.height(16.dp))
                KlarAmanBanner(onPelajariClick = onComingSoonClick)

                Spacer(modifier = Modifier.height(16.dp))
                PrivacyCard(onClick = onComingSoonClick)

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Versi 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))
                LogoutButton(isLoggingOut = uiState.isLoggingOut, onClick = onLogoutClick)
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun ProfileHeaderCard(profile: AccountProfile, onEditClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name?.takeIf(String::isNotBlank) ?: "-",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = profile.phone?.let { "+$it" } ?: "-",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                profile.email?.takeIf(String::isNotBlank)?.let { email ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (profile.emailVerified) {
                            Spacer(modifier = Modifier.size(4.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Terverifikasi",
                                tint = KlarTeal,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.clickable(onClick = onEditClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, tint = KlarTeal, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(4.dp))
            Text(text = "Ganti Profil", style = MaterialTheme.typography.bodyMedium, color = KlarTeal, fontWeight = FontWeight.Medium)
        }
    }
}

private data class SecurityChecklistItem(
    val label: String,
    val isDone: Boolean,
    val actionLabel: String? = null,
    val isActionLoading: Boolean = false,
    val onActionClick: (() -> Unit)? = null,
)

/** Real checklist (not a fabricated percentage, unlike the "80%" in the reference screenshot)
 * - phone/password/KYC are always true by the time a nasabah account exists at all (all
 * gated at register), email verification reflects the actual backend flag (currently always
 * false until an email-verification flow exists to flip it), and "Sidik Jari" reflects
 * whether a refresh token is actually stored behind a biometric prompt (see
 * kotlin-nasabah-app knowledge) - so this honestly shows partial progress until those are
 * done, rather than claiming 100% it can't back up. */
@Composable
private fun SecurityStatusCard(
    profile: AccountProfile,
    isChecklistExpanded: Boolean,
    isFingerprintEnabled: Boolean,
    isEnablingFingerprint: Boolean,
    onToggleChecklist: () -> Unit,
    onVerifyEmailClick: () -> Unit,
    onEnableFingerprintClick: () -> Unit,
) {
    val checklist = listOf(
        SecurityChecklistItem("Nomor HP terverifikasi", true),
        SecurityChecklistItem("Password sudah diatur", true),
        SecurityChecklistItem("KYC (KTP & Selfie) terverifikasi", true),
        SecurityChecklistItem("Email terverifikasi", profile.emailVerified),
        SecurityChecklistItem(
            label = "Sidik jari aktif",
            isDone = isFingerprintEnabled,
            actionLabel = "Aktifkan",
            isActionLoading = isEnablingFingerprint,
            onActionClick = onEnableFingerprintClick,
        ),
    )
    val completedCount = checklist.count { it.isDone }
    val totalCount = checklist.size
    val progress = completedCount.toFloat() / totalCount
    val isFullyProtected = completedCount == totalCount

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(KlarTealDark, KlarTeal))),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(88.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = if (isFullyProtected) "Akunmu sudah aman terproteksi" else "Ayo Lengkapi Proteksimu",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp),
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                color = KlarTeal,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleChecklist),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$completedCount/$totalCount langkah tuntas",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = if (isFullyProtected) {
                            "Mantap, kamu sepenuhnya terlindungi!"
                        } else {
                            "Yuk lengkapi lagi biar makin aman."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = if (isChecklistExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isChecklistExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                checklist.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (item.isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (item.isDone) KlarTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.size(10.dp))
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        if (!item.isDone && item.actionLabel != null && item.onActionClick != null) {
                            if (item.isActionLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = KlarTeal, strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = item.actionLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = KlarTeal,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable(onClick = item.onActionClick),
                                )
                            }
                        }
                    }
                }

                if (!isFullyProtected) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onVerifyEmailClick,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KlarTeal),
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Verifikasi Email Sekarang", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsListCard(
    uiState: AccountUiState,
    onToggleChangePassword: () -> Unit,
    onOldPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmitChangePassword: () -> Unit,
    onReferralClick: () -> Unit,
    onComingSoonClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        SettingsRow(
            icon = Icons.Default.CardGiftcard,
            title = "Ajak Teman",
            subtitle = "Bagikan kode referral, dapat diskon biaya Rp100.000",
            onClick = onReferralClick,
        )
        SettingsRow(
            icon = Icons.Default.Lock,
            title = "Ubah Password",
            subtitle = "Perbarui kata sandi akunmu secara berkala",
            trailingIcon = null,
            onClick = onToggleChangePassword,
        )

        if (uiState.changePasswordSuccess) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = KlarTeal, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Password berhasil diubah", style = MaterialTheme.typography.bodyMedium, color = KlarTeal)
            }
        }

        if (uiState.isChangePasswordExpanded) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                PasswordField(value = uiState.oldPassword, placeholder = "Password Lama", onValueChange = onOldPasswordChange)
                Spacer(modifier = Modifier.height(10.dp))
                PasswordField(
                    value = uiState.newPassword,
                    placeholder = "Password Baru (min. 8 karakter)",
                    onValueChange = onNewPasswordChange,
                )
                Spacer(modifier = Modifier.height(10.dp))
                PasswordField(
                    value = uiState.confirmPassword,
                    placeholder = "Konfirmasi Password Baru",
                    onValueChange = onConfirmPasswordChange,
                )
                uiState.changePasswordError?.let { message ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onSubmitChangePassword,
                    enabled = uiState.isChangePasswordFormValid,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KlarTeal),
                ) {
                    if (uiState.isChangingPassword) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Simpan Password Baru", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        SettingsRow(
            icon = Icons.Default.NotificationsNone,
            title = "Notifikasi Keamanan",
            subtitle = "Pantau keamanan akun dan aktivitas gak wajar",
            onClick = onComingSoonClick,
        )
        SettingsRow(
            icon = Icons.Default.Pin,
            title = "Atur PIN",
            subtitle = "Ganti atau reset PIN kamu kapan aja dibutuhin",
            onClick = onComingSoonClick,
        )
        SettingsRow(
            icon = Icons.Default.Devices,
            title = "Perangkat yang Disimpan",
            subtitle = "Simpan perangkat biar lebih mudah saat masuk ke klar",
            onClick = onComingSoonClick,
        )
    }
}

/**
 * Opt-in toggle for LocationCaptureWorker - explicit, off by default, and honest about what
 * it does (no "improve your experience" euphemism). Turning it on triggers the location
 * permission request flow in AccountScreen BEFORE any consent state changes anywhere - see
 * that composable's onLocationConsentToggle. Every actual capture afterwards posts its own
 * notification (see LocationCaptureWorker), so this toggle is the one time-of-decision moment,
 * not the only place this feature is ever visible to the user.
 */
@Composable
private fun LocationConsentCard(isEnabled: Boolean, isUpdating: Boolean, onToggle: (Boolean) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = KlarTeal, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bagikan Lokasi untuk Penilaian Risiko",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Jika aktif, lokasi Anda diambil sekitar jam 02:00, 10:00, dan 15:00 setiap hari " +
                        "untuk membantu penilaian risiko kredit (waktu bisa sedikit meleset tergantung " +
                        "kondisi perangkat). Anda akan mendapat notifikasi setiap kali lokasi diambil, dan " +
                        "bisa matikan kapan saja.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            if (isUpdating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = KlarTeal, strokeWidth = 2.dp)
            } else {
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedTrackColor = KlarTeal),
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailingIcon: ImageVector? = Icons.Default.ChevronRight,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = KlarTeal, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailingIcon?.let {
            Icon(imageVector = it, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class TrustPoint(val label: String, val icon: ImageVector)

private val trustPoints = listOf(
    TrustPoint("Fitur keamanan akun", Icons.Default.Security),
    TrustPoint("Layanan pelanggan 24 jam", Icons.Default.SupportAgent),
    TrustPoint("Jaminan saldo kembali", Icons.Default.Shield),
    TrustPoint("Privasi & proteksi data", Icons.Default.PrivacyTip),
)

@Composable
private fun KlarAmanBanner(onPelajariClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(KlarTeal, KlarTealDark)))
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "K", color = KlarTeal, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = "klar aman", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onPelajariClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
            ) {
                Text("Pelajari")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            trustPoints.forEach { point ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(imageVector = point.icon, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = point.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyCard(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
    ) {
        Text(
            text = "Privasimu yang Paling Utama",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Kami menjaga keamanan data pribadi, KTP, & transaksi kamu dan tidak akan disebar ke mana pun.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Cek gimana kami melindungi datamu",
                style = MaterialTheme.typography.bodyMedium,
                color = KlarTeal,
                fontWeight = FontWeight.Medium,
            )
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = KlarTeal, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun LogoutButton(isLoggingOut: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = !isLoggingOut,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
    ) {
        if (isLoggingOut) {
            CircularProgressIndicator(
                modifier = Modifier.height(20.dp),
                color = MaterialTheme.colorScheme.error,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text("Keluar", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun PasswordField(value: String, placeholder: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = PasswordVisualTransformation(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
