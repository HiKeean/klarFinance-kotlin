package com.klarfinance.app.core.call

import android.content.Intent

/** Must match DeskcallCallService.PUSH_TYPE_INCOMING_CALL (backend). */
const val DESKCALL_PUSH_TYPE = "DESKCALL_INCOMING_CALL"

/**
 * Panggilan masuk dari AI agent deskcall (DEMO, tombol Call di NPL Report webadmin). Datang lewat
 * FCM data-only - lihat backend DeskcallCallService. [token] = JWT LiveKit untuk join room sebagai
 * nasabah; [ringDeadlineEpochMs] = batas dering, lewat dari itu panggilan dianggap tak terjawab.
 */
data class IncomingCall(
    val callId: String,
    val livekitUrl: String,
    val token: String,
    val callerName: String,
    val ringDeadlineEpochMs: Long,
) {
    fun remainingRingMillis(now: Long = System.currentTimeMillis()): Long = ringDeadlineEpochMs - now

    fun isExpired(now: Long = System.currentTimeMillis()): Boolean = remainingRingMillis(now) <= 0

    fun writeTo(intent: Intent): Intent = intent
        .putExtra(EXTRA_CALL_ID, callId)
        .putExtra(EXTRA_LIVEKIT_URL, livekitUrl)
        .putExtra(EXTRA_TOKEN, token)
        .putExtra(EXTRA_CALLER_NAME, callerName)
        .putExtra(EXTRA_RING_DEADLINE, ringDeadlineEpochMs)

    companion object {
        private const val EXTRA_CALL_ID = "deskcall.callId"
        private const val EXTRA_LIVEKIT_URL = "deskcall.livekitUrl"
        private const val EXTRA_TOKEN = "deskcall.token"
        private const val EXTRA_CALLER_NAME = "deskcall.callerName"
        private const val EXTRA_RING_DEADLINE = "deskcall.ringDeadline"

        /** null kalau bukan push panggilan deskcall atau field wajibnya tidak lengkap. */
        fun fromPushData(data: Map<String, String>): IncomingCall? {
            if (data["type"] != DESKCALL_PUSH_TYPE) return null
            return IncomingCall(
                callId = data["callId"]?.takeIf { it.isNotBlank() } ?: return null,
                livekitUrl = data["livekitUrl"]?.takeIf { it.isNotBlank() } ?: return null,
                token = data["token"]?.takeIf { it.isNotBlank() } ?: return null,
                callerName = data["callerName"]?.takeIf { it.isNotBlank() } ?: "KlarFinance",
                ringDeadlineEpochMs = data["ringDeadlineEpochMs"]?.toLongOrNull() ?: return null,
            )
        }

        fun fromIntent(intent: Intent?): IncomingCall? {
            intent ?: return null
            return IncomingCall(
                callId = intent.getStringExtra(EXTRA_CALL_ID) ?: return null,
                livekitUrl = intent.getStringExtra(EXTRA_LIVEKIT_URL) ?: return null,
                token = intent.getStringExtra(EXTRA_TOKEN) ?: return null,
                callerName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: "KlarFinance",
                ringDeadlineEpochMs = intent.getLongExtra(EXTRA_RING_DEADLINE, 0L),
            )
        }
    }
}
