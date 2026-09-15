package com.klarfinance.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row cache - same reasoning as [ProfileEntity], only one account's referral summary
 * is ever relevant per device at a time. */
@Entity(tableName = "referral_summary_cache")
data class ReferralSummaryEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val referralCode: String,
    val minimumLoanAmount: Long,
    val rewardAmount: Long,
    val totalInvited: Long,
    val totalQualified: Long,
    val availableRewardsCount: Long,
    val availableDiscountTotal: Long,
    val cachedAtMillis: Long,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
