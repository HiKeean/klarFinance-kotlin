package com.klarfinance.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** [installmentsJson] holds the full installment schedule as a plain JSON string (encoded/
 * decoded manually with kotlinx.serialization in LoanRepositoryImpl, reusing the same [kotlinx.serialization.json.Json]
 * bean the network layer uses) - no Room TypeConverter needed since the column is already a
 * String. Chosen over a normalized child table because installments are only ever read/written
 * as one full list per loan, never queried individually across loans. */
@Entity(tableName = "loan_history_cache")
data class LoanHistoryItemEntity(
    @PrimaryKey val loanId: Int,
    val type: String,
    val merchantName: String?,
    val requestedAmount: Long,
    val totalAmountDue: Long,
    val tenorMonths: Int,
    val status: String,
    val paidInstallments: Int,
    val totalInstallments: Int,
    val nextDueDate: String?,
    val nextDueAmount: Long?,
    val createdAt: String?,
    val installmentsJson: String,
    val cachedAtMillis: Long,
)

@Serializable
data class CachedInstallment(
    val installmentNumber: Int,
    val dueDate: String?,
    val amount: Long,
    val isPaid: Boolean,
    val paidAt: String?,
)
