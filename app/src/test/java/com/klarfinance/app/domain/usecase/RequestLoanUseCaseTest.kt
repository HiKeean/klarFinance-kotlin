package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.model.LoanRequestResult
import com.klarfinance.app.domain.repository.LoanRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RequestLoanUseCaseTest {

    private val repository: LoanRepository = mockk()
    private lateinit var useCase: RequestLoanUseCase

    @Before
    fun setUp() {
        useCase = RequestLoanUseCase(repository)
    }

    @Test
    fun `zero or negative amount fails without calling repository`() = runTest {
        val result = useCase(amount = 0, tenorMonths = 3, bankAccountNumber = "123", bankCode = "BCA")

        assertTrue(result.isFailure)
        assertEquals("Nominal pinjaman harus lebih dari 0", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.requestLoan(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `unsupported tenor fails`() = runTest {
        val result = useCase(amount = 100_000, tenorMonths = 2, bankAccountNumber = "123", bankCode = "BCA")

        assertEquals("Tenor tidak valid", result.exceptionOrNull()?.message)
    }

    @Test
    fun `blank bank account number fails`() = runTest {
        val result = useCase(amount = 100_000, tenorMonths = 3, bankAccountNumber = "  ", bankCode = "BCA")

        assertEquals("Nomor rekening wajib diisi", result.exceptionOrNull()?.message)
    }

    @Test
    fun `blank bank code fails`() = runTest {
        val result = useCase(amount = 100_000, tenorMonths = 3, bankAccountNumber = "123", bankCode = "")

        assertEquals("Bank tujuan wajib dipilih", result.exceptionOrNull()?.message)
    }

    @Test
    fun `valid input trims account number and delegates`() = runTest {
        val expected = LoanRequestResult(
            loanId = 1,
            disbursedAmount = 99_000,
            adminFee = 1_000,
            totalAmountDue = 103_000,
            installmentAmount = 34_333,
            tenorMonths = 3,
            reviewRequired = false,
            reviewRequestId = null,
            message = null,
        )
        coEvery {
            repository.requestLoan(100_000, 3, "123456", "BCA", emptyList(), emptyList())
        } returns Result.success(expected)

        val result = useCase(amount = 100_000, tenorMonths = 3, bankAccountNumber = "  123456  ", bankCode = "BCA")

        assertEquals(Result.success(expected), result)
    }
}
