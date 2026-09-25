package com.klarfinance.app.presentation.history

import com.klarfinance.app.domain.model.Cached
import com.klarfinance.app.domain.model.LoanHistoryItem
import com.klarfinance.app.domain.model.LoanHistoryStatus
import com.klarfinance.app.domain.model.LoanHistoryType
import com.klarfinance.app.domain.model.LoanInstallment
import com.klarfinance.app.domain.usecase.GetLoanHistoryUseCase
import com.klarfinance.app.domain.usecase.RepayLoanUseCase
import com.klarfinance.app.presentation.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getLoanHistoryUseCase: GetLoanHistoryUseCase = mockk()
    private val repayLoanUseCase: RepayLoanUseCase = mockk()

    private fun item(loanId: Int = 1) = LoanHistoryItem(
        loanId = loanId,
        type = LoanHistoryType.LOAN,
        merchantName = null,
        requestedAmount = 300_000,
        totalAmountDue = 309_000,
        tenorMonths = 3,
        status = LoanHistoryStatus.ACTIVE,
        paidInstallments = 0,
        totalInstallments = 3,
        nextDueDate = "2026-10-01",
        nextDueAmount = 103_000,
        createdAt = "2026-09-01",
        installments = listOf(
            LoanInstallment(1, "2026-10-01", 103_000, isPaid = false, paidAt = null),
            LoanInstallment(2, "2026-11-01", 103_000, isPaid = false, paidAt = null),
            LoanInstallment(3, "2026-12-01", 103_000, isPaid = false, paidAt = null),
        ),
    )

    private fun createViewModel() = HistoryViewModel(getLoanHistoryUseCase, repayLoanUseCase)

    @Test
    fun `init loads history successfully`() = runTest {
        coEvery { getLoanHistoryUseCase() } returns Result.success(Cached(listOf(item()), isFromCache = false))

        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, viewModel.uiState.value.items.size)
        assertNull(viewModel.uiState.value.loadErrorMessage)
    }

    @Test
    fun `init sets loadErrorMessage on failure`() = runTest {
        coEvery { getLoanHistoryUseCase() } returns Result.failure(IllegalStateException("Gagal memuat riwayat"))

        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Gagal memuat riwayat", viewModel.uiState.value.loadErrorMessage)
    }

    @Test
    fun `onInstallmentToggled adds and removes installment numbers`() = runTest {
        coEvery { getLoanHistoryUseCase() } returns Result.success(Cached(listOf(item()), isFromCache = false))
        val viewModel = createViewModel()

        viewModel.onInstallmentToggled(1)
        assertEquals(setOf(1), viewModel.uiState.value.selectedInstallmentNumbers)

        viewModel.onInstallmentToggled(2)
        assertEquals(setOf(1, 2), viewModel.uiState.value.selectedInstallmentNumbers)

        viewModel.onInstallmentToggled(1)
        assertEquals(setOf(2), viewModel.uiState.value.selectedInstallmentNumbers)
    }

    @Test
    fun `resetPaymentSelection clears selection and error`() = runTest {
        coEvery { getLoanHistoryUseCase() } returns Result.success(Cached(listOf(item()), isFromCache = false))
        val viewModel = createViewModel()
        viewModel.onInstallmentToggled(1)

        viewModel.resetPaymentSelection()

        assertEquals(emptySet<Int>(), viewModel.uiState.value.selectedInstallmentNumbers)
        assertNull(viewModel.uiState.value.paymentErrorMessage)
    }

    @Test
    fun `submitPayment sums selected unpaid installments and replaces the updated item on success`() = runTest {
        coEvery { getLoanHistoryUseCase() } returns Result.success(Cached(listOf(item(loanId = 1)), isFromCache = false))
        val viewModel = createViewModel()
        viewModel.onInstallmentToggled(1)
        viewModel.onInstallmentToggled(2)

        val updated = item(loanId = 1).copy(paidInstallments = 2)
        coEvery { repayLoanUseCase(1, 206_000) } returns Result.success(updated)

        viewModel.submitPayment(1)

        assertFalse(viewModel.uiState.value.isSubmittingPayment)
        assertEquals(emptySet<Int>(), viewModel.uiState.value.selectedInstallmentNumbers)
        assertEquals("Pembayaran berhasil", viewModel.uiState.value.pendingPaymentMessage)
        assertEquals(2, viewModel.uiState.value.items.first().paidInstallments)
    }

    @Test
    fun `submitPayment sets paymentErrorMessage on failure`() = runTest {
        coEvery { getLoanHistoryUseCase() } returns Result.success(Cached(listOf(item(loanId = 1)), isFromCache = false))
        val viewModel = createViewModel()
        viewModel.onInstallmentToggled(1)
        coEvery { repayLoanUseCase(1, 103_000) } returns Result.failure(IllegalStateException("Minimum pembayaran belum tercapai"))

        viewModel.submitPayment(1)

        assertFalse(viewModel.uiState.value.isSubmittingPayment)
        assertEquals("Minimum pembayaran belum tercapai", viewModel.uiState.value.paymentErrorMessage)
    }

    @Test
    fun `consumePaymentMessage nulls the pending message`() = runTest {
        coEvery { getLoanHistoryUseCase() } returns Result.success(Cached(listOf(item(loanId = 1)), isFromCache = false))
        val viewModel = createViewModel()
        viewModel.onInstallmentToggled(1)
        coEvery { repayLoanUseCase(1, 103_000) } returns Result.success(item(loanId = 1))
        viewModel.submitPayment(1)

        viewModel.consumePaymentMessage()

        assertNull(viewModel.uiState.value.pendingPaymentMessage)
    }
}
