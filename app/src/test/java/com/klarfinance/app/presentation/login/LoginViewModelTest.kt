package com.klarfinance.app.presentation.login

import app.cash.turbine.test
import com.klarfinance.app.domain.model.OtpChannel
import com.klarfinance.app.domain.repository.VerifiedPhoneRepository
import com.klarfinance.app.domain.usecase.CheckPhoneRegisteredUseCase
import com.klarfinance.app.domain.usecase.RequestOtpUseCase
import com.klarfinance.app.presentation.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val requestOtpUseCase: RequestOtpUseCase = mockk()
    private val verifiedPhoneRepository: VerifiedPhoneRepository = mockk()
    private val checkPhoneRegisteredUseCase: CheckPhoneRegisteredUseCase = mockk()
    private lateinit var viewModel: LoginViewModel

    private val phoneNumber = "081234567890"
    private val fullPhone = "6281234567890"

    @Before
    fun setUp() {
        viewModel = LoginViewModel(requestOtpUseCase, verifiedPhoneRepository, checkPhoneRegisteredUseCase)
        viewModel.onPhoneNumberChange(phoneNumber)
    }

    @Test
    fun `onPhoneNumberChange strips non-digit characters and clears error`() {
        viewModel.onPhoneNumberChange("0812-3456")

        assertEquals("08123456", viewModel.uiState.value.phoneNumber)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `onContinueClick does nothing when phone number is too short`() = runTest {
        viewModel.onPhoneNumberChange("123")

        viewModel.onContinueClick()

        coVerify(exactly = 0) { verifiedPhoneRepository.isRecentlyVerified(any()) }
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `onContinueClick requests otp and emits otpRequested on success when not recently verified`() = runTest {
        coEvery { verifiedPhoneRepository.isRecentlyVerified(fullPhone) } returns false
        coEvery { requestOtpUseCase(fullPhone) } returns Result.success(OtpChannel.WHATSAPP)

        viewModel.otpRequested.test {
            viewModel.onContinueClick()
            assertEquals(fullPhone to OtpChannel.WHATSAPP, awaitItem())
        }
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `onContinueClick sets errorMessage when requestOtp fails`() = runTest {
        coEvery { verifiedPhoneRepository.isRecentlyVerified(fullPhone) } returns false
        coEvery { requestOtpUseCase(fullPhone) } returns Result.failure(IllegalStateException("Network down"))

        viewModel.onContinueClick()

        assertEquals("Network down", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `onContinueClick routes to password login when phone recently verified and already registered`() = runTest {
        coEvery { verifiedPhoneRepository.isRecentlyVerified(fullPhone) } returns true
        coEvery { checkPhoneRegisteredUseCase(fullPhone) } returns Result.success(true)

        viewModel.needsPasswordLogin.test {
            viewModel.onContinueClick()
            assertEquals(fullPhone, awaitItem())
        }
        coVerify(exactly = 0) { requestOtpUseCase(any()) }
    }

    @Test
    fun `onContinueClick routes to already-verified flow when phone recently verified but not registered`() = runTest {
        coEvery { verifiedPhoneRepository.isRecentlyVerified(fullPhone) } returns true
        coEvery { checkPhoneRegisteredUseCase(fullPhone) } returns Result.success(false)

        viewModel.alreadyVerified.test {
            viewModel.onContinueClick()
            assertEquals(fullPhone, awaitItem())
        }
    }
}
