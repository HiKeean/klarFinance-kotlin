package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.model.QrisTransactionInfo
import com.klarfinance.app.domain.repository.QrisRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScanQrisUseCaseTest {

    private val repository: QrisRepository = mockk()
    private lateinit var useCase: ScanQrisUseCase

    @Before
    fun setUp() {
        useCase = ScanQrisUseCase(repository)
    }

    @Test
    fun `blank merchant code fails without calling repository`() = runTest {
        val result = useCase("   ")

        assertTrue(result.isFailure)
        assertEquals("QR tidak valid", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.scan(any()) }
    }

    @Test
    fun `merchant code is trimmed before delegating`() = runTest {
        val expected = QrisTransactionInfo(token = "tok-1", merchantName = "Toko A")
        coEvery { repository.scan("MERCHANT123") } returns Result.success(expected)

        val result = useCase("  MERCHANT123  ")

        assertEquals(Result.success(expected), result)
    }
}
