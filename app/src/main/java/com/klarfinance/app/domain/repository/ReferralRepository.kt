package com.klarfinance.app.domain.repository

import com.klarfinance.app.domain.model.Cached
import com.klarfinance.app.domain.model.ReferralSummary

interface ReferralRepository {
    /** GET api/v1/nasabah/referral/summary - own referral code plus invite/reward counts.
     * Network-first, falls back to the last cached summary (Room) if the device is offline -
     * see [Cached.isFromCache]. */
    suspend fun getSummary(): Result<Cached<ReferralSummary>>
}
