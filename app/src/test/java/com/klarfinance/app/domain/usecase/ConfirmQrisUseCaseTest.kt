package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.model.QrisConfirmResult
import com.klarfinance.app.domain.repository.QrisRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConfirmQrisUseCaseTest {

    private val repository: QrisRepository = mockk()
    private lateinit var useCase: ConfirmQrisUseCase

    @Before
    fun setUp() {
        useCase = ConfirmQrisUseCase(repository)
    }

    @Test
    fun `zero or negative amount fails without calling repository`() = runTest {
        val result = useCase("token-1", 0)

        assertTrue(result.isFailure)
        assertEquals("Nominal harus lebih dari 0", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.confirm(any(), any()) }
    }

    @Test
    fun `positive amount delegates to repository`() = runTest {
        val expected = QrisConfirmResult(loanId = 1, merchantName = "Toko A", totalAmountDue = 103_000, installmentAmount = 34_333)
        coEvery { repository.confirm("token-1", 100_000) } returns Result.success(expected)

        val result = useCase("token-1", 100_000)

        assertEquals(Result.success(expected), result)
    }
}
