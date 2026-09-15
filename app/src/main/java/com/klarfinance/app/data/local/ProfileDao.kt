package com.klarfinance.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProfileEntity)

    @Query("SELECT * FROM profile_cache WHERE id = ${ProfileEntity.SINGLETON_ID} LIMIT 1")
    suspend fun get(): ProfileEntity?

    @Query("DELETE FROM profile_cache")
    suspend fun clear()
}
