package com.klarfinance.app.presentation.call

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.klarfinance.app.core.call.IncomingCall
import com.klarfinance.app.core.call.IncomingCallNotifier
import com.klarfinance.app.core.theme.KlarFinanceTheme
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Layar panggilan dari AI agent deskcall (DEMO): berdering -> Angkat -> join room LiveKit dengan
 * token dari push -> ngobrol (mic nasabah dipublish, suara agent diputar otomatis) -> selesai saat
 * agent keluar room atau nasabah menutup. Tolak / tak terjawab cukup menutup layar; deskcall
 * sendiri yang menandai panggilan tak diangkat setelah batas dering.
 *
 * Batasan demo: belum ada foreground service, jadi layar ini harus tetap terbuka selama panggilan.
 */
class DeskcallCallActivity : ComponentActivity() {

    private lateinit var call: IncomingCall
    private val uiState = mutableStateOf<CallUiState>(CallUiState.Ringing)
    private var room: Room? = null
    private var ringTimeoutJob: Job? = null

    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) connect() else end("Izin mikrofon ditolak, panggilan tidak bisa dilanjutkan")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        val incoming = IncomingCall.fromIntent(intent)
        if (incoming == null || incoming.isExpired()) {
            IncomingCallNotifier.cancel(this)
            finish()
            return
        }
        call = incoming

        setContent {
            KlarFinanceTheme {
                DeskcallCallScreen(
                    callerName = call.callerName,
                    state = uiState.value,
                    onAccept = ::accept,
                    onDecline = ::decline,
                    onHangUp = { end("Panggilan diakhiri") },
                )
            }
        }

        ringTimeoutJob = lifecycleScope.launch {
            delay(call.remainingRingMillis().coerceAtLeast(0L))
            if (uiState.value == CallUiState.Ringing) end("Panggilan tak terjawab")
        }
        handleAction(intent.action)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // tombol Angkat/Tolak di heads-up notification saat layar ini sudah terbuka (launchMode singleTop)
        handleAction(intent.action)
    }

    private fun handleAction(action: String?) {
        when (action) {
            ACTION_ACCEPT -> accept()
            ACTION_DECLINE -> decline()
        }
    }

    private fun accept() {
        if (uiState.value != CallUiState.Ringing) return
        ringTimeoutJob?.cancel()
        IncomingCallNotifier.cancel(this)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            connect()
        } else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun decline() {
        if (uiState.value != CallUiState.Ringing) return
        IncomingCallNotifier.cancel(this)
        finish()
    }

    private fun connect() {
        uiState.value = CallUiState.Connecting
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val lkRoom = LiveKit.create(appContext = applicationContext)
        room = lkRoom

        lifecycleScope.launch {
            lkRoom.events.collect { event ->
                when (event) {
                    // room deskcall cuma berisi nasabah + agent: agent keluar = percakapan selesai
                    is RoomEvent.ParticipantDisconnected -> end("Panggilan selesai")
                    is RoomEvent.Disconnected -> end("Panggilan selesai")
                    else -> Unit
                }
            }
        }
        lifecycleScope.launch {
            try {
                lkRoom.connect(call.livekitUrl, call.token)
                lkRoom.localParticipant.setMicrophoneEnabled(true)
                if (uiState.value == CallUiState.Connecting) {
                    uiState.value = CallUiState.InCall(startedAtMillis = System.currentTimeMillis())
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gagal join room panggilan ${call.callId}", e)
                end("Gagal tersambung, coba lagi nanti")
            }
        }
    }

    private fun end(message: String) {
        if (uiState.value is CallUiState.Ended) return
        ringTimeoutJob?.cancel()
        IncomingCallNotifier.cancel(this)
        releaseRoom()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        uiState.value = CallUiState.Ended(message)
        lifecycleScope.launch {
            delay(1_500L)
            finish()
        }
    }

    private fun releaseRoom() {
        room?.let {
            it.disconnect()
            it.release()
        }
        room = null
    }

    override fun onDestroy() {
        releaseRoom()
        super.onDestroy()
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
    }

    companion object {
        private const val TAG = "DeskcallCallActivity"
        const val ACTION_SHOW = "com.klarfinance.app.deskcall.SHOW"
        const val ACTION_ACCEPT = "com.klarfinance.app.deskcall.ACCEPT"
        const val ACTION_DECLINE = "com.klarfinance.app.deskcall.DECLINE"

        fun intent(context: Context, call: IncomingCall, action: String): Intent =
            call.writeTo(Intent(context, DeskcallCallActivity::class.java))
                .setAction(action)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
