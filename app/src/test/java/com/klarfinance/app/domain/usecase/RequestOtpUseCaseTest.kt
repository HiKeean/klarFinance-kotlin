package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.model.OtpChannel
import com.klarfinance.app.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RequestOtpUseCaseTest {

    private val repository: AuthRepository = mockk()
    private lateinit var useCase: RequestOtpUseCase

    @Before
    fun setUp() {
        useCase = RequestOtpUseCase(repository)
    }

    @Test
    fun `phone with fewer than 9 digits fails without calling repository`() = runTest {
        val result = useCase("0812345")

        assertTrue(result.isFailure)
        assertEquals("Enter a valid phone number", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.requestOtp(any()) }
    }

    @Test
    fun `phone with exactly 9 digits is accepted`() = runTest {
        coEvery { repository.requestOtp("081234567") } returns Result.success(OtpChannel.WHATSAPP)

        val result = useCase("081234567")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.requestOtp("081234567") }
    }

    @Test
    fun `non-digit characters are stripped before validating and delegating`() = runTest {
        coEvery { repository.requestOtp("6281234567890") } returns Result.success(OtpChannel.WHATSAPP)

        val result = useCase("+62 812-3456-7890")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.requestOtp("6281234567890") }
    }
}
