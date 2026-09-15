package com.klarfinance.app.presentation.passwordlogin

data class PasswordLoginUiState(
    val phone: String,
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val isLoginEnabled: Boolean
        get() = password.isNotBlank() && !isLoading
}
