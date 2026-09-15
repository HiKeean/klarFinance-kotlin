package com.klarfinance.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transjakarta_ticket_cache")
data class TransjakartaTicketEntity(
    @PrimaryKey val ticketId: Int,
    val ticketCode: String,
    val amount: Long,
    val used: Boolean,
    val purchasedAt: String,
    val loanId: Int,
    val billingCycle: String,
    val dueDate: String,
    val cachedAtMillis: Long,
)
