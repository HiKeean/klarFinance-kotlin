package com.klarfinance.app.presentation.referral

import com.klarfinance.app.domain.model.ReferralSummary

data class ReferralUiState(
    val isLoading: Boolean = true,
    val summary: ReferralSummary? = null,
    val loadErrorMessage: String? = null,
    /** True when [summary] is the last cached value (Room), shown because the network was
     * unreachable - see [com.klarfinance.app.domain.model.Cached]. */
    val isOffline: Boolean = false,
)
