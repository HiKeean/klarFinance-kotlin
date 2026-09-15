package com.klarfinance.app.data.dto

import kotlinx.serialization.Serializable

/** Mirrors backend ReferralSummaryResponse (referral/dto/response) - GET api/v1/nasabah/referral/summary. */
@Serializable
data class ReferralSummaryResponseDto(
    val referralCode: String? = null,
    val minimumLoanAmount: Double? = null,
    val rewardAmount: Double? = null,
    val totalInvited: Long? = null,
    val totalQualified: Long? = null,
    val availableRewardsCount: Long? = null,
    val availableDiscountTotal: Double? = null,
)
