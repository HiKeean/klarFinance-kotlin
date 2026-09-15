package com.klarfinance.app.domain.model

data class RegisterResult(
    val identity: String?,
    val applicationStatus: String?,
    val suggestedLimit: Double?,
    val message: String?,
)
