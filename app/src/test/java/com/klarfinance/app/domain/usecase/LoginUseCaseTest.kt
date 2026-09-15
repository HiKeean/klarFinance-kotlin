package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.model.AccountState
import com.klarfinance.app.domain.model.LoginResult
import com.klarfinance.app.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoginUseCaseTest {

    private val repository: AuthRepository = mockk()
    private lateinit var useCase: LoginUseCase

    @Before
    fun setUp() {
        useCase = LoginUseCase(repository)
    }

    @Test
    fun `blank password fails without calling repository`() = runTest {
        val result = useCase("081234567890", "   ")

        assertTrue(result.isFailure)
        assertEquals("Enter your password", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.login(any(), any()) }
    }

    @Test
    fun `non-digit characters are stripped from identity before delegating`() = runTest {
        val expected = LoginResult(identity = "081234567890", name = "Test", accountState = AccountState.ACTIVE)
        coEvery { repository.login("081234567890", "secret123") } returns Result.success(expected)

        val result = useCase("0812-3456-7890", "secret123")

        assertEquals(Result.success(expected), result)
        coVerify(exactly = 1) { repository.login("081234567890", "secret123") }
    }

    @Test
    fun `propagates repository failure`() = runTest {
        val error = IllegalStateException("Invalid credentials")
        coEvery { repository.login(any(), any()) } returns Result.failure(error)

        val result = useCase("081234567890", "wrongpass")

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
