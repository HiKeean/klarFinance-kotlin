package com.klarfinance.app.presentation.account

import com.klarfinance.app.domain.model.AccountProfile

data class AccountUiState(
    val isLoading: Boolean = true,
    val profile: AccountProfile? = null,
    val loadErrorMessage: String? = null,
    /** True when [profile] is the last cached value (Room), shown because the network was
     * unreachable - see [com.klarfinance.app.domain.model.Cached]. */
    val isOffline: Boolean = false,

    val isChangePasswordExpanded: Boolean = false,
    val oldPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isChangingPassword: Boolean = false,
    val changePasswordError: String? = null,
    val changePasswordSuccess: Boolean = false,

    val isLoggingOut: Boolean = false,

    val isSecurityChecklistExpanded: Boolean = false,
    val isFingerprintEnabled: Boolean = false,
    val isEnablingFingerprint: Boolean = false,

    val isLocationConsentGiven: Boolean = false,
    val isUpdatingLocationConsent: Boolean = false,
) {
    val isChangePasswordFormValid: Boolean
        get() = oldPassword.isNotBlank() &&
            newPassword.length >= 8 &&
            newPassword == confirmPassword &&
            !isChangingPassword
}
