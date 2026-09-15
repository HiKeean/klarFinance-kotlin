package com.klarfinance.app.presentation.otp

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.klarfinance.app.core.navigation.Screen
import com.klarfinance.app.domain.repository.VerifiedPhoneRepository
import com.klarfinance.app.domain.usecase.CheckPhoneRegisteredUseCase
import com.klarfinance.app.domain.usecase.RequestOtpUseCase
import com.klarfinance.app.domain.usecase.VerifyOtpUseCase
import com.klarfinance.app.presentation.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
    private val savedStateHandle: SavedStateHandle = mockk()

    private val phone = "6281234567890"
    private lateinit var viewModel: OtpViewModel

    @Before
    fun setUp() {
        every { savedStateHandle.get<String>(Screen.OtpVerification.ARG_PHONE) } returns phone
        viewModel = OtpViewModel(verifyOtpUseCase, requestOtpUseCase, verifiedPhoneRepository, checkPhoneRegisteredUseCase, savedStateHandle)
    }

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
        coEvery { requestOtpUseCase(phone) } returns Result.success(Unit)

        // Let the initial 60s cooldown run out (virtual time) so canResend becomes true.
        mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(60_000)
        mainDispatcherRule.testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.canResend)

        viewModel.onResendClick()

        coVerify(exactly = 1) { requestOtpUseCase(phone) }
        assertEquals("", viewModel.uiState.value.otp)
        assertEquals(60, viewModel.uiState.value.resendSecondsRemaining)
    }
}
