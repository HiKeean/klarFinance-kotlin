package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.model.LoanHistoryItem
import com.klarfinance.app.domain.model.LoanHistoryStatus
import com.klarfinance.app.domain.model.LoanHistoryType
import com.klarfinance.app.domain.repository.LoanRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RepayLoanUseCaseTest {

    private val repository: LoanRepository = mockk()
    private lateinit var useCase: RepayLoanUseCase

    @Before
    fun setUp() {
        useCase = RepayLoanUseCase(repository)
    }

    @Test
    fun `zero or negative amount fails without calling repository`() = runTest {
        val result = useCase(loanId = 1, amount = 0)

        assertTrue(result.isFailure)
        assertEquals("Nominal pembayaran harus lebih dari 0", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.repay(any(), any()) }
    }

    @Test
    fun `positive amount delegates to repository`() = runTest {
        val expected = LoanHistoryItem(
            loanId = 1,
            type = LoanHistoryType.LOAN,
            merchantName = null,
            requestedAmount = 100_000,
            totalAmountDue = 103_000,
            tenorMonths = 3,
            status = LoanHistoryStatus.ACTIVE,
            paidInstallments = 1,
            totalInstallments = 3,
            nextDueDate = "2026-10-01",
            nextDueAmount = 34_333,
            createdAt = "2026-09-01",
            installments = emptyList(),
        )
        coEvery { repository.repay(1, 34_333) } returns Result.success(expected)

        val result = useCase(loanId = 1, amount = 34_333)

        assertEquals(Result.success(expected), result)
    }
}
