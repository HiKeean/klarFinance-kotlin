package com.klarfinance.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VerifiedPhoneDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VerifiedPhoneEntity)

    @Query("SELECT * FROM verified_phones WHERE phone = :phone LIMIT 1")
    suspend fun findByPhone(phone: String): VerifiedPhoneEntity?
}
