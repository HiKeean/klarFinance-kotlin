package com.klarfinance.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "verified_phones")
data class VerifiedPhoneEntity(
    @PrimaryKey val phone: String,
    val verifiedAtMillis: Long,
)
