package com.klarfinance.app.domain.model

data class ReferralSummary(
    val referralCode: String,
    val minimumLoanAmount: Long,
    val rewardAmount: Long,
    val totalInvited: Long,
    val totalQualified: Long,
    val availableRewardsCount: Long,
    val availableDiscountTotal: Long,
)
