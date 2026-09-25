package com.klarfinance.app.core.call

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import com.klarfinance.app.R
import com.klarfinance.app.presentation.call.DeskcallCallActivity

const val INCOMING_CALL_CHANNEL_ID = "deskcall_incoming_call"
private const val INCOMING_CALL_NOTIFICATION_ID = 7301
private const val TAG = "IncomingCallNotifier"

/**
 * Layar panggilan masuk: notifikasi CallStyle + full-screen intent (muncul penuh di lock screen /
 * layar mati; kalau HP sedang dipakai, Android menampilkannya sebagai heads-up dengan tombol
 * Angkat/Tolak). Dering dari suara channel (ringtone default) + FLAG_INSISTENT supaya berulang,
 * berhenti otomatis saat batas dering lewat (setTimeoutAfter).
 */
object IncomingCallNotifier {

    fun show(context: Context, call: IncomingCall) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "POST_NOTIFICATIONS belum diizinkan - panggilan ${call.callId} tidak bisa ditampilkan")
            return
        }
        createChannel(context)

        val fullScreen = activityIntent(context, call, DeskcallCallActivity.ACTION_SHOW, requestCode = 0)
        val answer = activityIntent(context, call, DeskcallCallActivity.ACTION_ACCEPT, requestCode = 1)
        val decline = activityIntent(context, call, DeskcallCallActivity.ACTION_DECLINE, requestCode = 2)
        val caller = Person.Builder().setName(call.callerName).setImportant(true).build()

        val notification = NotificationCompat.Builder(context, INCOMING_CALL_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(call.callerName)
            .setContentText("Panggilan suara masuk")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(fullScreen)
            .setFullScreenIntent(fullScreen, true)
            .setStyle(NotificationCompat.CallStyle.forIncomingCall(caller, decline, answer))
            .setTimeoutAfter(call.remainingRingMillis().coerceAtLeast(1_000L))
            .build()
        notification.flags = notification.flags or Notification.FLAG_INSISTENT

        NotificationManagerCompat.from(context).notify(INCOMING_CALL_NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(INCOMING_CALL_NOTIFICATION_ID)
    }

    private fun activityIntent(context: Context, call: IncomingCall, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            DeskcallCallActivity.intent(context, call, action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val ringtone = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val channel = NotificationChannel(INCOMING_CALL_CHANNEL_ID, "Panggilan Masuk", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Panggilan suara dari KlarFinance"
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE), ringtone)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 1000, 1000)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
