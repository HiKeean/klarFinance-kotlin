package com.klarfinance.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row cache - only one account is ever logged in per device at a time in this app. */
@Entity(tableName = "profile_cache")
data class ProfileEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val name: String?,
    val phone: String?,
    val role: String?,
    val dob: String?,
    val email: String?,
    val emailVerified: Boolean,
    val accountState: String,
    val cachedAtMillis: Long,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
