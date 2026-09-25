package com.klarfinance.app.presentation.otp

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import android.app.Activity
import com.klarfinance.app.core.auth.SmsOtpEvent
import com.klarfinance.app.core.auth.SmsOtpSender
import com.klarfinance.app.core.navigation.Screen
import com.klarfinance.app.domain.model.OtpChannel
import com.klarfinance.app.domain.repository.VerifiedPhoneRepository
import com.klarfinance.app.domain.usecase.CheckPhoneRegisteredUseCase
import com.klarfinance.app.domain.usecase.RequestOtpUseCase
import com.klarfinance.app.domain.usecase.VerifyFirebasePhoneUseCase
import com.klarfinance.app.domain.usecase.VerifyOtpUseCase
import com.klarfinance.app.presentation.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OtpViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val verifyOtpUseCase: VerifyOtpUseCase = mockk()
    private val requestOtpUseCase: RequestOtpUseCase = mockk()
    private val verifiedPhoneRepository: VerifiedPhoneRepository = mockk(relaxed = true)
    private val checkPhoneRegisteredUseCase: CheckPhoneRegisteredUseCase = mockk()
    private val verifyFirebasePhoneUseCase: VerifyFirebasePhoneUseCase = mockk()
    private val smsOtpSender: SmsOtpSender = mockk(relaxed = true)
    private val activity: Activity = mockk()
    private val savedStateHandle: SavedStateHandle = mockk()

    private val phone = "6281234567890"
    private lateinit var viewModel: OtpViewModel

    @Before
    fun setUp() {
        every { savedStateHandle.get<String>(Screen.OtpVerification.ARG_PHONE) } returns phone
        every { savedStateHandle.get<String>(Screen.OtpVerification.ARG_CHANNEL) } returns null
        viewModel = createViewModel()
    }

    private fun createViewModel() = OtpViewModel(
        verifyOtpUseCase, requestOtpUseCase, verifiedPhoneRepository, checkPhoneRegisteredUseCase,
        verifyFirebasePhoneUseCase, smsOtpSender, savedStateHandle,
    )

    @Test
    fun `initial state seeds phone from saved state and starts the resend cooldown`() {
        assertEquals(phone, viewModel.uiState.value.phone)
        assertEquals(60, viewModel.uiState.value.resendSecondsRemaining)
        assertTrue(!viewModel.uiState.value.canResend)
    }

    @Test
    fun `onOtpChange updates otp and clears error`() {
        viewModel.onOtpChange("123456")

        assertEquals("123456", viewModel.uiState.value.otp)
    }

    @Test
    fun `onVerifyClick does nothing when otp is not 6 digits`() = runTest {
        viewModel.onOtpChange("123")

        viewModel.onVerifyClick()

        coVerify(exactly = 0) { verifyOtpUseCase(any(), any()) }
    }

    @Test
    fun `onVerifyClick marks phone verified and emits otpVerified when phone not yet registered`() = runTest {
        viewModel.onOtpChange("123456")
        coEvery { verifyOtpUseCase(phone, "123456") } returns Result.success(Unit)
        coEvery { checkPhoneRegisteredUseCase(phone) } returns Result.success(false)

        viewModel.otpVerified.test {
            viewModel.onVerifyClick()
            assertEquals(phone, awaitItem())
        }
        coVerify(exactly = 1) { verifiedPhoneRepository.markVerified(phone) }
        assertTrue(viewModel.uiState.value.isVerified)
    }

    @Test
    fun `onVerifyClick emits needsPasswordLogin when phone already registered`() = runTest {
        viewModel.onOtpChange("123456")
        coEvery { verifyOtpUseCase(phone, "123456") } returns Result.success(Unit)
        coEvery { checkPhoneRegisteredUseCase(phone) } returns Result.success(true)

        viewModel.needsPasswordLogin.test {
            viewModel.onVerifyClick()
            assertEquals(phone, awaitItem())
        }
    }

    @Test
    fun `onVerifyClick sets errorMessage when verification fails`() = runTest {
        viewModel.onOtpChange("123456")
        coEvery { verifyOtpUseCase(phone, "123456") } returns Result.failure(IllegalArgumentException("Invalid code"))

        viewModel.onVerifyClick()

        assertEquals("Invalid code", viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { verifiedPhoneRepository.markVerified(any()) }
    }

    @Test
    fun `onResendClick does nothing while cooldown is still active`() = runTest {
        viewModel.onResendClick()

        coVerify(exactly = 0) { requestOtpUseCase(any()) }
    }

    @Test
    fun `onResendClick requests a new otp and restarts the cooldown once it expires`() = runTest {
        viewModel.onOtpChange("111111")
        coEvery { requestOtpUseCase(phone) } returns Result.success(OtpChannel.WHATSAPP)

        // Let the initial 60s cooldown run out (virtual time) so canResend becomes true.
        mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(60_000)
        mainDispatcherRule.testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.canResend)

        viewModel.onResendClick()

        coVerify(exactly = 1) { requestOtpUseCase(phone) }
        assertEquals("", viewModel.uiState.value.otp)
        assertEquals(60, viewModel.uiState.value.resendSecondsRemaining)
    }

    @Test
    fun `sms channel from nav arg asks the screen to send the sms code`() {
        every { savedStateHandle.get<String>(Screen.OtpVerification.ARG_CHANNEL) } returns OtpChannel.SMS.name

        val smsViewModel = createViewModel()

        assertEquals(OtpChannel.SMS, smsViewModel.uiState.value.channel)
        assertTrue(smsViewModel.uiState.value.smsSendPending)
    }

    @Test
    fun `onSwitchToSmsClick switches channel and sends sms once the screen provides the activity`() {
        viewModel.onSwitchToSmsClick()
        assertTrue(viewModel.uiState.value.smsSendPending)

        viewModel.sendSmsCode(activity)

        assertEquals(OtpChannel.SMS, viewModel.uiState.value.channel)
        assertFalse(viewModel.uiState.value.smsSendPending)
        coVerify(exactly = 1) { smsOtpSender.send(activity, phone, any()) }
    }

    @Test
    fun `sms verify exchanges firebase token with backend and emits otpVerified`() = runTest {
        val onEvent = slot<(SmsOtpEvent) -> Unit>()
        every { smsOtpSender.send(activity, phone, capture(onEvent)) } returns Unit
        coEvery { smsOtpSender.confirm("654321") } returns Result.success("firebase-id-token")
        coEvery { verifyFirebasePhoneUseCase(phone, "firebase-id-token") } returns Result.success(Unit)
        coEvery { checkPhoneRegisteredUseCase(phone) } returns Result.success(false)

        viewModel.onSwitchToSmsClick()
        viewModel.sendSmsCode(activity)
        onEvent.captured(SmsOtpEvent.CodeSent)
        viewModel.onOtpChange("654321")

        viewModel.otpVerified.test {
            viewModel.onVerifyClick()
            assertEquals(phone, awaitItem())
        }
        coVerify(exactly = 0) { verifyOtpUseCase(any(), any()) }
        coVerify(exactly = 1) { verifiedPhoneRepository.markVerified(phone) }
    }

    @Test
    fun `sms send failure surfaces the error and allows retry`() {
        val onEvent = slot<(SmsOtpEvent) -> Unit>()
        every { smsOtpSender.send(activity, phone, capture(onEvent)) } returns Unit

        viewModel.onSwitchToSmsClick()
        viewModel.sendSmsCode(activity)
        onEvent.captured(SmsOtpEvent.Failed("Too many SMS requests. Please try again later"))

        assertEquals("Too many SMS requests. Please try again later", viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.canResend)
    }
}
