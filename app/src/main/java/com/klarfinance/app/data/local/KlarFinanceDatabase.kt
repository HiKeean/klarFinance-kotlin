package com.klarfinance.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        VerifiedPhoneEntity::class,
        ProfileEntity::class,
        LoanHistoryItemEntity::class,
        TransjakartaTicketEntity::class,
        ReferralSummaryEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class KlarFinanceDatabase : RoomDatabase() {
    abstract fun verifiedPhoneDao(): VerifiedPhoneDao
    abstract fun profileDao(): ProfileDao
    abstract fun loanHistoryDao(): LoanHistoryDao
    abstract fun transjakartaTicketDao(): TransjakartaTicketDao
    abstract fun referralSummaryDao(): ReferralSummaryDao
}
