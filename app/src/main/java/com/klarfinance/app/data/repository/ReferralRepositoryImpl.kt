package com.klarfinance.app.data.repository

import com.klarfinance.app.core.network.ApiService
import com.klarfinance.app.core.network.NetworkUnavailableException
import com.klarfinance.app.data.dto.ReferralSummaryResponseDto
import com.klarfinance.app.data.local.ReferralSummaryDao
import com.klarfinance.app.data.local.ReferralSummaryEntity
import com.klarfinance.app.domain.model.Cached
import com.klarfinance.app.domain.model.ReferralSummary
import com.klarfinance.app.domain.repository.ReferralRepository
import javax.inject.Inject

class ReferralRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val referralSummaryDao: ReferralSummaryDao,
) : ReferralRepository {

    override suspend fun getSummary(): Result<Cached<ReferralSummary>> {
        val networkResult = runCatching {
            val response = apiService.get<ReferralSummaryResponseDto>("api/v1/nasabah/referral/summary")
            val data = response.data ?: throw IllegalStateException("Unexpected response from server")
            ReferralSummary(
                referralCode = data.referralCode.orEmpty(),
                minimumLoanAmount = (data.minimumLoanAmount ?: 0.0).toLong(),
                rewardAmount = (data.rewardAmount ?: 0.0).toLong(),
                totalInvited = data.totalInvited ?: 0L,
                totalQualified = data.totalQualified ?: 0L,
                availableRewardsCount = data.availableRewardsCount ?: 0L,
                availableDiscountTotal = (data.availableDiscountTotal ?: 0.0).toLong(),
            )
        }
        networkResult.onSuccess { summary ->
            referralSummaryDao.upsert(
                ReferralSummaryEntity(
                    referralCode = summary.referralCode,
                    minimumLoanAmount = summary.minimumLoanAmount,
                    rewardAmount = summary.rewardAmount,
                    totalInvited = summary.totalInvited,
                    totalQualified = summary.totalQualified,
                    availableRewardsCount = summary.availableRewardsCount,
                    availableDiscountTotal = summary.availableDiscountTotal,
                    cachedAtMillis = System.currentTimeMillis(),
                ),
            )
            return Result.success(Cached(summary, isFromCache = false))
        }

        val error = networkResult.exceptionOrNull()
        if (error is NetworkUnavailableException) {
            referralSummaryDao.get()?.let { cached ->
                return Result.success(
                    Cached(
                        ReferralSummary(
                            referralCode = cached.referralCode,
                            minimumLoanAmount = cached.minimumLoanAmount,
                            rewardAmount = cached.rewardAmount,
                            totalInvited = cached.totalInvited,
                            totalQualified = cached.totalQualified,
                            availableRewardsCount = cached.availableRewardsCount,
                            availableDiscountTotal = cached.availableDiscountTotal,
                        ),
                        isFromCache = true,
                    ),
                )
            }
        }
        return Result.failure(error ?: IllegalStateException("Unknown error"))
    }
}
