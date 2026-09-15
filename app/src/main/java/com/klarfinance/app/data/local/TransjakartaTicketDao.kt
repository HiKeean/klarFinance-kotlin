package com.klarfinance.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface TransjakartaTicketDao {
    @Query("SELECT * FROM transjakarta_ticket_cache")
    suspend fun getAll(): List<TransjakartaTicketEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOne(entity: TransjakartaTicketEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<TransjakartaTicketEntity>)

    @Query("DELETE FROM transjakarta_ticket_cache")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(entities: List<TransjakartaTicketEntity>) {
        clear()
        upsertAll(entities)
    }
}
