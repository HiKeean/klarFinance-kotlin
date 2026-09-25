package com.klarfinance.app.presentation.otp

import com.klarfinance.app.domain.model.OtpChannel

data class OtpUiState(
    val phone: String = "",
    val channel: OtpChannel = OtpChannel.WHATSAPP,
    /** SMS perlu dikirim dari layar (Firebase Phone Auth butuh Activity) - screen memanggil
     * OtpViewModel.sendSmsCode begitu flag ini true. */
    val smsSendPending: Boolean = false,
    val otp: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isVerified: Boolean = false,
    val resendSecondsRemaining: Int = 60,
) {
    val canResend: Boolean get() = resendSecondsRemaining <= 0 && !isLoading
    val canSwitchToSms: Boolean get() = channel == OtpChannel.WHATSAPP && !isLoading && !isVerified
    val canVerify: Boolean get() = otp.length == 6 && !isLoading
}
