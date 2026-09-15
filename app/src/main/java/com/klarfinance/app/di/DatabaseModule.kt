package com.klarfinance.app.di

import android.content.Context
import androidx.room.Room
import com.klarfinance.app.data.local.KlarFinanceDatabase
import com.klarfinance.app.data.local.LoanHistoryDao
import com.klarfinance.app.data.local.ProfileDao
import com.klarfinance.app.data.local.ReferralSummaryDao
import com.klarfinance.app.data.local.TransjakartaTicketDao
import com.klarfinance.app.data.local.VerifiedPhoneDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KlarFinanceDatabase =
        Room.databaseBuilder(context, KlarFinanceDatabase::class.java, "klarfinance.db")
            // v1->v2 added offline-cache tables (profile/loan history/Transjakarta tickets) -
            // no real Migration written since the app hasn't shipped to real devices yet and
            // every table involved is a disposable cache, not user-authored data.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideVerifiedPhoneDao(database: KlarFinanceDatabase): VerifiedPhoneDao = database.verifiedPhoneDao()

    @Provides
    fun provideProfileDao(database: KlarFinanceDatabase): ProfileDao = database.profileDao()

    @Provides
    fun provideLoanHistoryDao(database: KlarFinanceDatabase): LoanHistoryDao = database.loanHistoryDao()

    @Provides
    fun provideTransjakartaTicketDao(database: KlarFinanceDatabase): TransjakartaTicketDao =
        database.transjakartaTicketDao()

    @Provides
    fun provideReferralSummaryDao(database: KlarFinanceDatabase): ReferralSummaryDao =
        database.referralSummaryDao()
}
