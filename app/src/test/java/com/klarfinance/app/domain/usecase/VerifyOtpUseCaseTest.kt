package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VerifyOtpUseCaseTest {

    private val repository: AuthRepository = mockk()
    private lateinit var useCase: VerifyOtpUseCase

    @Before
    fun setUp() {
        useCase = VerifyOtpUseCase(repository)
    }

    @Test
    fun `otp shorter than 6 digits fails without calling repository`() = runTest {
        val result = useCase("081234567890", "123")

        assertTrue(result.isFailure)
        assertEquals("Enter the 6-digit code", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.verifyOtp(any(), any()) }
    }

    @Test
    fun `otp with non-digit characters fails`() = runTest {
        val result = useCase("081234567890", "12a456")

        assertTrue(result.isFailure)
        assertEquals("Enter the 6-digit code", result.exceptionOrNull()?.message)
    }

    @Test
    fun `valid 6-digit otp delegates with digits-only phone`() = runTest {
        coEvery { repository.verifyOtp("081234567890", "123456") } returns Result.success(Unit)

        val result = useCase("0812-3456-7890", "123456")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.verifyOtp("081234567890", "123456") }
    }
}
