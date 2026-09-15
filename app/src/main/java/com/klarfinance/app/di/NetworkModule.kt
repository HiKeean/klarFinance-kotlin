package com.klarfinance.app.di

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.klarfinance.app.BuildConfig
import com.klarfinance.app.core.network.AuthInterceptor
import com.klarfinance.app.core.network.HmacInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /** Release build pakai library-no-op (lihat app/build.gradle.kts) - kelas yang sama tapi
     * gak ngapa-ngapain, jadi interceptor ini aman dipasang tanpa dibungkus BuildConfig.DEBUG. */
    @Provides
    @Singleton
    fun provideChuckerInterceptor(@ApplicationContext context: Context): ChuckerInterceptor =
        ChuckerInterceptor.Builder(context).build()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        hmacInterceptor: HmacInterceptor,
        authInterceptor: AuthInterceptor,
        chuckerInterceptor: ChuckerInterceptor,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(hmacInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            // Chucker terakhir - biar dia nangkep request/response abis semua interceptor lain
            // (Hmac/Auth) ngerubah headernya.
            .addInterceptor(chuckerInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}
