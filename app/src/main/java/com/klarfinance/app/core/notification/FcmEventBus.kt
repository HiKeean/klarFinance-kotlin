package com.klarfinance.app.core.notification

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Structured payload from an FCM data message - see PushNotificationService (backend) for
 * the fields actually sent. [type] lets subscribers ignore push categories they don't care
 * about instead of reacting to every FCM message. */
data class FcmDataEvent(val type: String?, val status: String?)

/**
 * App-wide bridge from [KlarFirebaseMessagingService] (which has no UI/ViewModel of its own)
 * to whichever screen is currently alive and cares about a given push - e.g. Home refreshing
 * [com.klarfinance.app.domain.model.AccountState] the moment a loan approval push lands while
 * the app is already open, instead of only showing a system tray notification. `extraBufferCapacity
 * = 1` + [tryEmit] so a publish from the (non-suspend) `onMessageReceived` callback never blocks
 * or gets silently dropped for lack of a collector at that exact instant.
 */
@Singleton
class FcmEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<FcmDataEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<FcmDataEvent> = _events.asSharedFlow()

    fun publish(event: FcmDataEvent) {
        _events.tryEmit(event)
    }
}
