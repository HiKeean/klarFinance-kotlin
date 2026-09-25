package com.klarfinance.app.di

import com.klarfinance.app.core.auth.FirebaseSmsOtpSender
import com.klarfinance.app.core.auth.SmsOtpSender
import com.klarfinance.app.data.repository.AuthRepositoryImpl
import com.klarfinance.app.data.repository.LoanRepositoryImpl
import com.klarfinance.app.data.repository.LocationRepositoryImpl
import com.klarfinance.app.data.repository.LocationTrackingRepositoryImpl
import com.klarfinance.app.data.repository.QrisRepositoryImpl
import com.klarfinance.app.data.repository.ReferralRepositoryImpl
import com.klarfinance.app.data.repository.TransjakartaRepositoryImpl
import com.klarfinance.app.data.repository.VerifiedPhoneRepositoryImpl
import com.klarfinance.app.domain.repository.AuthRepository
import com.klarfinance.app.domain.repository.LoanRepository
import com.klarfinance.app.domain.repository.LocationRepository
import com.klarfinance.app.domain.repository.LocationTrackingRepository
import com.klarfinance.app.domain.repository.QrisRepository
import com.klarfinance.app.domain.repository.ReferralRepository
import com.klarfinance.app.domain.repository.TransjakartaRepository
import com.klarfinance.app.domain.repository.VerifiedPhoneRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    /** Sengaja tidak @Singleton - menyimpan verificationId per alur OTP (satu instance per OtpViewModel). */
    @Binds
    abstract fun bindSmsOtpSender(impl: FirebaseSmsOtpSender): SmsOtpSender

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds
    @Singleton
    abstract fun bindVerifiedPhoneRepository(impl: VerifiedPhoneRepositoryImpl): VerifiedPhoneRepository

    @Binds
    @Singleton
    abstract fun bindLoanRepository(impl: LoanRepositoryImpl): LoanRepository

    @Binds
    @Singleton
    abstract fun bindLocationTrackingRepository(impl: LocationTrackingRepositoryImpl): LocationTrackingRepository

    @Binds
    @Singleton
    abstract fun bindReferralRepository(impl: ReferralRepositoryImpl): ReferralRepository

    @Binds
    @Singleton
    abstract fun bindQrisRepository(impl: QrisRepositoryImpl): QrisRepository

    @Binds
    @Singleton
    abstract fun bindTransjakartaRepository(impl: TransjakartaRepositoryImpl): TransjakartaRepository
}
