package com.klarfinance.app.domain.model

data class LoginResult(
    val identity: String?,
    val name: String?,
    val accountState: AccountState,
)
