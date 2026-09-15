package com.klarfinance.app.presentation.loan

import com.klarfinance.app.domain.model.LimitSummary
import com.klarfinance.app.domain.model.SavedBankAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestLoanUiStateTest {

    @Test
    fun `isAmountStepValid requires a positive amount`() {
        assertFalse(RequestLoanUiState(amountInput = "0").isAmountStepValid)
        assertFalse(RequestLoanUiState(amountInput = "").isAmountStepValid)
        assertTrue(RequestLoanUiState(amountInput = "100000").isAmountStepValid)
    }

    @Test
    fun `isAmountStepValid rejects an amount above the available limit`() {
        val summary = LimitSummary(totalLimit = 1_000_000, usedLimit = 0, availableLimit = 500_000)
        assertFalse(RequestLoanUiState(amountInput = "600000", limitSummary = summary).isAmountStepValid)
        assertTrue(RequestLoanUiState(amountInput = "500000", limitSummary = summary).isAmountStepValid)
    }

    @Test
    fun `isBankStepValid is false while saved accounts are loading`() {
        val state = RequestLoanUiState(isLoadingSavedBankAccounts = true, bankAccountNumber = "123")
        assertFalse(state.isBankStepValid)
    }

    @Test
    fun `isBankStepValid falls back to manual entry when no saved accounts exist`() {
        val base = RequestLoanUiState(isLoadingSavedBankAccounts = false, savedBankAccounts = emptyList())
        assertFalse(base.copy(bankAccountNumber = "  ").isBankStepValid)
        assertTrue(base.copy(bankAccountNumber = "123456").isBankStepValid)
    }

    @Test
    fun `isBankStepValid requires a selection when saved accounts exist`() {
        val saved = listOf(SavedBankAccount(id = 1, bankCode = "BCA", bankAccountNumber = "123"))
        val base = RequestLoanUiState(isLoadingSavedBankAccounts = false, savedBankAccounts = saved)
        assertFalse(base.copy(selectedSavedBankAccountId = null).isBankStepValid)
        assertTrue(base.copy(selectedSavedBankAccountId = 1).isBankStepValid)
    }

    @Test
    fun `resolvedBankAccount prefers the selected saved account`() {
        val saved = listOf(SavedBankAccount(id = 1, bankCode = "BCA", bankAccountNumber = "999"))
        val state = RequestLoanUiState(savedBankAccounts = saved, selectedSavedBankAccountId = 1, bankCode = "BNI", bankAccountNumber = "111")

        assertEquals("BCA" to "999", state.resolvedBankAccount)
    }

    @Test
    fun `resolvedBankAccount is null when saved accounts exist but nothing is selected`() {
        val saved = listOf(SavedBankAccount(id = 1, bankCode = "BCA", bankAccountNumber = "999"))
        val state = RequestLoanUiState(savedBankAccounts = saved, selectedSavedBankAccountId = null)

        assertNull(state.resolvedBankAccount)
    }

    @Test
    fun `resolvedBankAccount falls back to manual entry when no saved accounts`() {
        val state = RequestLoanUiState(savedBankAccounts = emptyList(), bankCode = "BNI", bankAccountNumber = "111")

        assertEquals("BNI" to "111", state.resolvedBankAccount)
    }

    @Test
    fun `amountPresets are 25-50-100 percent of available limit rounded down to nearest 50000`() {
        val summary = LimitSummary(totalLimit = 1_000_000, usedLimit = 0, availableLimit = 1_000_000)
        val state = RequestLoanUiState(limitSummary = summary)

        assertEquals(listOf(250_000L, 500_000L, 1_000_000L), state.amountPresets)
    }

    @Test
    fun `amountPresets drops presets that round down to zero`() {
        val summary = LimitSummary(totalLimit = 120_000, usedLimit = 0, availableLimit = 120_000)
        val state = RequestLoanUiState(limitSummary = summary)

        // 25% of 120000 = 30000 -> rounds down to 0 and is dropped
        assertEquals(listOf(50_000L, 100_000L), state.amountPresets)
    }

    @Test
    fun `amountPresets is empty without a limit summary or with zero available limit`() {
        assertTrue(RequestLoanUiState(limitSummary = null).amountPresets.isEmpty())
        val zeroAvailable = LimitSummary(totalLimit = 1_000_000, usedLimit = 1_000_000, availableLimit = 0)
        assertTrue(RequestLoanUiState(limitSummary = zeroAvailable).amountPresets.isEmpty())
    }

    @Test
    fun `recommendedAmount picks the largest preset that stays within the 30 percent review threshold`() {
        val summary = LimitSummary(totalLimit = 1_000_000, usedLimit = 0, availableLimit = 1_000_000)
        val state = RequestLoanUiState(limitSummary = summary)

        // presets: 250000/500000/1000000 - only 250000 keeps utilization at/under 30%
        assertEquals(250_000L, state.recommendedAmount)
    }

    @Test
    fun `recommendedAmount falls back to the smallest preset when all exceed the threshold`() {
        val summary = LimitSummary(totalLimit = 1_000_000, usedLimit = 800_000, availableLimit = 200_000)
        val state = RequestLoanUiState(limitSummary = summary)

        // presets: 50000/100000/200000 - all push utilization past 30%
        assertEquals(50_000L, state.recommendedAmount)
    }

    @Test
    fun `recommendedAmount is null when there is no limit summary`() {
        assertNull(RequestLoanUiState(limitSummary = null).recommendedAmount)
    }

    @Test
    fun `projectedUtilizationPercent and willRequireReview reflect usage after this loan`() {
        val summary = LimitSummary(totalLimit = 1_000_000, usedLimit = 200_000, availableLimit = 800_000)
        val underThreshold = RequestLoanUiState(amountInput = "50000", limitSummary = summary)
        val overThreshold = RequestLoanUiState(amountInput = "200000", limitSummary = summary)

        assertEquals(25.0, underThreshold.projectedUtilizationPercent!!, 0.001)
        assertFalse(underThreshold.willRequireReview)

        assertEquals(40.0, overThreshold.projectedUtilizationPercent!!, 0.001)
        assertTrue(overThreshold.willRequireReview)
    }

    @Test
    fun `willRequireReview is false without a limit summary`() {
        assertFalse(RequestLoanUiState(amountInput = "999999999", limitSummary = null).willRequireReview)
    }

    @Test
    fun `adminFee is 1 percent of the amount and netAmountReceived subtracts it`() {
        val state = RequestLoanUiState(amountInput = "100000")

        assertEquals(1_000L, state.adminFee)
        assertEquals(99_000L, state.netAmountReceived)
    }

    @Test
    fun `monthlyRatePercentLabel formats known tenors and falls back for unknown ones`() {
        assertEquals("3.5%", RequestLoanUiState(tenorMonths = 1).monthlyRatePercentLabel)
        assertEquals("3.3%", RequestLoanUiState(tenorMonths = 3).monthlyRatePercentLabel)
        assertEquals("3.1%", RequestLoanUiState(tenorMonths = 6).monthlyRatePercentLabel)
        assertEquals("3%", RequestLoanUiState(tenorMonths = 9).monthlyRatePercentLabel)
        assertEquals("3%", RequestLoanUiState(tenorMonths = 12).monthlyRatePercentLabel)
        assertEquals("-", RequestLoanUiState(tenorMonths = 2).monthlyRatePercentLabel)
    }

    @Test
    fun `totalInterest totalAmountDue and installmentAmount are computed from the tenor rate`() {
        val state = RequestLoanUiState(amountInput = "100000", tenorMonths = 3)

        assertEquals(9_900L, state.totalInterest)
        assertEquals(109_900L, state.totalAmountDue)
        assertEquals(36_633L, state.installmentAmount)
    }

    @Test
    fun `installmentAmount is zero when tenorMonths is not positive`() {
        val state = RequestLoanUiState(amountInput = "100000", tenorMonths = 0)

        assertEquals(0L, state.installmentAmount)
    }

    @Test
    fun `amountValue parses digits from amountInput ignoring formatting`() {
        assertEquals(1_234_567L, RequestLoanUiState(amountInput = "1.234.567").amountValue())
        assertEquals(0L, RequestLoanUiState(amountInput = "").amountValue())
    }
}
