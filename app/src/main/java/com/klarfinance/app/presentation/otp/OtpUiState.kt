package com.klarfinance.app.presentation.otp

data class OtpUiState(
    val phone: String = "",
    val otp: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isVerified: Boolean = false,
    val resendSecondsRemaining: Int = 60,
) {
    val canResend: Boolean get() = resendSecondsRemaining <= 0 && !isLoading
    val canVerify: Boolean get() = otp.length == 6 && !isLoading
}
