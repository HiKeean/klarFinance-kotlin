package com.klarfinance.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.klarfinance.app.core.location.createLocationCaptureNotificationChannel
import com.klarfinance.app.core.notification.createApprovalNotificationChannel
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KlarFinanceApp : Application(), Configuration.Provider {

    /** Lets Hilt-injected Workers (see LocationCaptureWorker) get constructor dependencies -
     * WorkManager is initialized on demand from this Configuration.Provider; the default
     * WorkManagerInitializer is removed in AndroidManifest.xml (otherwise it wins and ignores this). */
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        createApprovalNotificationChannel(this)
        createLocationCaptureNotificationChannel(this)
    }
}
