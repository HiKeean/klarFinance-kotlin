package com.klarfinance.app.core.call

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomingCallTest {

    private val push = mapOf(
        "type" to DESKCALL_PUSH_TYPE,
        "callId" to "call-1",
        "livekitUrl" to "wss://livekit.example",
        "token" to "jwt",
        "callerName" to "KlarFinance",
        "ringDeadlineEpochMs" to "10000",
    )

    @Test
    fun `parses a complete deskcall push`() {
        val call = IncomingCall.fromPushData(push)!!

        assertEquals("call-1", call.callId)
        assertEquals("wss://livekit.example", call.livekitUrl)
        assertEquals("jwt", call.token)
        assertEquals(10_000L, call.ringDeadlineEpochMs)
    }

    @Test
    fun `ignores other push types and incomplete payloads`() {
        assertNull(IncomingCall.fromPushData(mapOf("type" to "LOAN_APPROVAL", "status" to "APPROVED")))
        assertNull(IncomingCall.fromPushData(push - "token"))
        assertNull(IncomingCall.fromPushData(push + ("livekitUrl" to " ")))
        assertNull(IncomingCall.fromPushData(push + ("ringDeadlineEpochMs" to "abc")))
    }

    @Test
    fun `falls back to KlarFinance when caller name is missing`() {
        assertEquals("KlarFinance", IncomingCall.fromPushData(push - "callerName")!!.callerName)
    }

    @Test
    fun `expires at the ring deadline`() {
        val call = IncomingCall.fromPushData(push)!!

        assertFalse(call.isExpired(now = 9_999L))
        assertTrue(call.isExpired(now = 10_000L))
        assertEquals(4_000L, call.remainingRingMillis(now = 6_000L))
    }
}
