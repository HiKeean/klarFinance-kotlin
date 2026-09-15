package com.klarfinance.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReferralSummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ReferralSummaryEntity)

    @Query("SELECT * FROM referral_summary_cache WHERE id = ${ReferralSummaryEntity.SINGLETON_ID} LIMIT 1")
    suspend fun get(): ReferralSummaryEntity?

    @Query("DELETE FROM referral_summary_cache")
    suspend fun clear()
}
