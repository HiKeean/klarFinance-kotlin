package com.klarfinance.app.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.klarfinance.app.MainActivity
import com.klarfinance.app.R
import com.klarfinance.app.core.session.SessionManager
import com.klarfinance.app.domain.repository.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

const val APPROVAL_NOTIFICATION_CHANNEL_ID = "approval_status"

/**
 * Push notification (FCM) approve/reject pengajuan - lihat kotlin-nasabah-app knowledge & backend
 * PushNotificationService. Hilt support @AndroidEntryPoint langsung di FirebaseMessagingService
 * (subclass Service), jadi bisa inject AuthRepository/SessionManager tanpa manual ServiceLocator.
 */
@AndroidEntryPoint
class KlarFirebaseMessagingService : FirebaseMessagingService() {
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var fcmEventBus: FcmEventBus

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Fires on first token generation AND on rotation - independent of login state (FCM
     * generates a token as soon as the app is installed, way before any login happens). Only
     * push it to backend kalau ada sesi aktif SAAT INI; kalau belum login, token bakal ke-sync
     * lewat AuthRepositoryImpl.login() begitu user beneran login nanti (lihat syncFcmToken di
     * sana) - gak ada gunanya push token buat user yang belum diketahui identitasnya.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (sessionManager.accessToken == null) return
        serviceScope.launch { authRepository.updateFcmToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Data payload is delivered regardless of app state (foreground/background), unlike
        // the notification{} block which the system tray swallows itself when backgrounded -
        // this is what lets a screen that's currently open react (refresh) instead of only
        // relying on the user tapping a tray notification. See PushNotificationService (backend).
        if (message.data.isNotEmpty()) {
            fcmEventBus.publish(FcmDataEvent(type = message.data["type"], status = message.data["status"]))
        }

        val title = message.notification?.title ?: "KlarFinance"
        val body = message.notification?.body ?: return
        showNotification(title, body)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun showNotification(title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, APPROVAL_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }
}

/** Dipanggil dari [com.klarfinance.app.KlarFinanceApp.onCreate] - notification channel WAJIB
 * ke-register sebelum notify() dipanggil di Android O+, dan idempotent (create ulang channel
 * yang sudah ada itu no-op), jadi aman dipanggil tiap app start. */
fun createApprovalNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
        APPROVAL_NOTIFICATION_CHANNEL_ID,
        "Status Pengajuan",
        NotificationManager.IMPORTANCE_HIGH,
    ).apply {
        description = "Notifikasi approve/reject pengajuan pinjaman"
    }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}
