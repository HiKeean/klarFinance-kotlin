package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CheckPhoneRegisteredUseCaseTest {

    private val repository: AuthRepository = mockk()
    private lateinit var useCase: CheckPhoneRegisteredUseCase

    @Before
    fun setUp() {
        useCase = CheckPhoneRegisteredUseCase(repository)
    }

    @Test
    fun `non-digit characters are stripped before delegating`() = runTest {
        coEvery { repository.isPhoneRegistered("6281234567890") } returns Result.success(true)

        val result = useCase("+62 812-3456-7890")

        assertEquals(Result.success(true), result)
        coVerify(exactly = 1) { repository.isPhoneRegistered("6281234567890") }
    }

    @Test
    fun `propagates repository failure`() = runTest {
        val error = IllegalStateException("Network error")
        coEvery { repository.isPhoneRegistered(any()) } returns Result.failure(error)

        val result = useCase("081234567890")

        assertEquals(error, result.exceptionOrNull())
    }
}
