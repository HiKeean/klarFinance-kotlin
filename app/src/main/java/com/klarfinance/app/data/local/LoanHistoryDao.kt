package com.klarfinance.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface LoanHistoryDao {
    @Query("SELECT * FROM loan_history_cache")
    suspend fun getAll(): List<LoanHistoryItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOne(entity: LoanHistoryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<LoanHistoryItemEntity>)

    @Query("DELETE FROM loan_history_cache")
    suspend fun clear()

    /** Replaces the whole cache with a fresh network result so a loan that no longer comes back
     * from the server (shouldn't normally happen, but keeps the cache honest) doesn't linger. */
    @Transaction
    suspend fun replaceAll(entities: List<LoanHistoryItemEntity>) {
        clear()
        upsertAll(entities)
    }
}
