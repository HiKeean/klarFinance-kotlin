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

class VerifyPasswordUseCaseTest {

    private val repository: AuthRepository = mockk()
    private lateinit var useCase: VerifyPasswordUseCase

    @Before
    fun setUp() {
        useCase = VerifyPasswordUseCase(repository)
    }

    @Test
    fun `blank password fails without calling repository`() = runTest {
        val result = useCase("   ")

        assertTrue(result.isFailure)
        assertEquals("Masukkan password", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.verifyPassword(any()) }
    }

    @Test
    fun `non-blank password delegates to repository`() = runTest {
        coEvery { repository.verifyPassword("secret123") } returns Result.success(Unit)

        val result = useCase("secret123")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.verifyPassword("secret123") }
    }

    @Test
    fun `propagates repository failure`() = runTest {
        val error = IllegalStateException("Password salah")
        coEvery { repository.verifyPassword(any()) } returns Result.failure(error)

        val result = useCase("wrong")

        assertEquals(error, result.exceptionOrNull())
    }
}
