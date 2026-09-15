package com.klarfinance.app.presentation.login

data class LoginUiState(
    val countryCode: String = "+62",
    val phoneNumber: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val isContinueEnabled: Boolean
        get() = phoneNumber.filter(Char::isDigit).length >= 9 && !isLoading
}
