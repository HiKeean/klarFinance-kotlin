package com.klarfinance.app.presentation.loan

import android.content.Context
import app.cash.turbine.test
import com.klarfinance.app.core.session.SecureTokenStore
import com.klarfinance.app.domain.model.LimitSummary
import com.klarfinance.app.domain.model.LoanRequestResult
import com.klarfinance.app.domain.model.SavedBankAccount
import com.klarfinance.app.domain.usecase.GetLimitSummaryUseCase
import com.klarfinance.app.domain.usecase.GetSavedBankAccountsUseCase
import com.klarfinance.app.domain.usecase.RequestLoanUseCase
import com.klarfinance.app.domain.usecase.VerifyPasswordUseCase
import com.klarfinance.app.presentation.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RequestLoanViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getLimitSummaryUseCase: GetLimitSummaryUseCase = mockk()
    private val getSavedBankAccountsUseCase: GetSavedBankAccountsUseCase = mockk()
    private val requestLoanUseCase: RequestLoanUseCase = mockk()
    private val verifyPasswordUseCase: VerifyPasswordUseCase = mockk()
    private val secureTokenStore: SecureTokenStore = mockk(relaxed = true)
    private val appContext: Context = mockk(relaxed = true)

    private val limitSummary = LimitSummary(totalLimit = 10_000_000, usedLimit = 0, availableLimit = 10_000_000)

    @Before
    fun setUp() {
        every { secureTokenStore.hasRefreshToken() } returns false
    }

    private fun createViewModel() = RequestLoanViewModel(
        getLimitSummaryUseCase,
        getSavedBankAccountsUseCase,
        requestLoanUseCase,
        verifyPasswordUseCase,
        secureTokenStore,
        appContext,
    )

    @Test
    fun `init loads limit summary and saved bank accounts`() = runTest {
        coEvery { getLimitSummaryUseCase() } returns Result.success(limitSummary)
        coEvery { getSavedBankAccountsUseCase() } returns Result.success(emptyList())

        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isLoadingLimit)
        assertEquals(limitSummary, viewModel.uiState.value.limitSummary)
        assertFalse(viewModel.uiState.value.isLoadingSavedBankAccounts)
    }

    @Test
    fun `loadLimit sets loadErrorMessage on failure`() = runTest {
        coEvery { getLimitSummaryUseCase() } returns Result.failure(IllegalStateException("Gagal memuat data limit"))
        coEvery { getSavedBankAccountsUseCase() } returns Result.success(emptyList())

        val viewModel = createViewModel()

        assertEquals("Gagal memuat data limit", viewModel.uiState.value.loadErrorMessage)
    }

    @Test
    fun `loadSavedBankAccounts preselects the first saved account`() = runTest {
        coEvery { getLimitSummaryUseCase() } returns Result.success(limitSummary)
        val accounts = listOf(SavedBankAccount(id = 5, bankCode = "BCA", bankAccountNumber = "123"))
        coEvery { getSavedBankAccountsUseCase() } returns Result.success(accounts)

        val viewModel = createViewModel()

        assertEquals(accounts, viewModel.uiState.value.savedBankAccounts)
        assertEquals(5, viewModel.uiState.value.selectedSavedBankAccountId)
    }

    @Test
    fun `loadSavedBankAccounts failure is silent (no error message, list stays empty)`() = runTest {
        coEvery { getLimitSummaryUseCase() } returns Result.success(limitSummary)
        coEvery { getSavedBankAccountsUseCase() } returns Result.failure(IllegalStateException("network"))

        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isLoadingSavedBankAccounts)
        assertTrue(viewModel.uiState.value.savedBankAccounts.isEmpty())
    }

    @Test
    fun `onAmountChange keeps digits only and formats with thousands separators`() = runTest {
        coEvery { getLimitSummaryUseCase() } returns Result.success(limitSummary)
        coEvery { getSavedBankAccountsUseCase() } returns Result.success(emptyList())
        val viewModel = createViewModel()

        viewModel.onAmountChange("1.000.000abc")

        assertEquals("1.000.000", viewModel.uiState.value.amountInput)
    }

    @Test
    fun `onSubmitClick requires password confirmation when fingerprint is not enabled`() = runTest {
        coEvery { getLimitSummaryUseCase() } returns Result.success(limitSummary)
        coEvery { getSavedBankAccountsUseCase() } returns Result.success(emptyList())
        val viewModel = createViewModel()
        viewModel.onAmountChange("500000")
        viewModel.onBankAccountNumberChange("123456")
        val activity = mockk<androidx.fragment.app.FragmentActivity>(relaxed = true)

        viewModel.onSubmitClick(activity)

        assertTrue(viewModel.uiState.value.requiresPasswordConfirm)
        coVerify(exactly = 0) { requestLoanUseCase(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `onSubmitClick does nothing when the form is invalid`() = runTest {
        coEvery { getLimitSummaryUseCase() } returns Result.success(limitSummary)
        coEvery { getSavedBankAccountsUseCase() } returns Result.success(emptyList())
        val viewModel = createViewModel()
        // amountInput left blank -> isAmountStepValid is false
        val activity = mockk<androidx.fragment.app.FragmentActivity>(relaxed = true)

        viewModel.onSubmitClick(activity)

        assertFalse(viewModel.uiState.value.requiresPasswordConfirm)
    }

    @Test
    fun `onPasswordConfirm does nothing when password is blank`() = runTest {
        coEvery { getLimitSummaryUseCase() } returns Result.success(limitSummary)
        coEvery { getSavedBankAccountsUseCase() } returns Result.success(emptyList())
        val viewModel = createViewModel()

        viewModel.onPasswordConfirm()

        coVerify(exactly = 0) { verifyPasswordUseCase(any()) }
    }

    @Test
    fun `onPasswordConfirm verifies then submits the loan on success`() = runTest {
        coEvery { getLimitSummaryUseCase() } returns Result.success(limitSummary)
        coEvery { getSavedBankAccountsUseCase() } returns Result.success(emptyList())
        val viewModel = createViewModel()
        viewModel.onAmountChange("500000")
        viewModel.onBankAccountNumberChange("123456")
        viewModel.onBankCodeSelected("BCA")
        viewModel.onPasswordInputChange("mypassword")
        coEvery { verifyPasswordUseCase("mypassword") } returns Result.success(Unit)
        val result = LoanRequestResult(
            loanId = 1,
            disbursedAmount = 495_000,
            adminFee = 5_000,
            totalAmountDue = 516_500,
            installmentAmount = 172_167,
            tenorMonths = 3,
            reviewRequired = false,
            reviewRequestId = null,
            message = null,
        )
        coEvery {
            requestLoanUseCase(500_000, 1, "123456", "BCA", emptyList(), emptyList())
        } returns Result.success(result)

        viewModel.submitted.test {
            viewModel.onPasswordConfirm()
            assertEquals(result, awaitItem())
        }
        assertFalse(viewModel.uiState.value.requiresPasswordConfirm)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `onPasswordConfirm sets passwordError when verification fails`() = runTest {
        coEvery { getLimitSummaryUseCase() } returns Result.success(limitSummary)
        coEvery { getSavedBankAccountsUseCase() } returns Result.success(emptyList())
        val viewModel = createViewModel()
        viewModel.onPasswordInputChange("wrongpass")
        coEvery { verifyPasswordUseCase("wrongpass") } returns Result.failure(IllegalStateException("Password salah"))

        viewModel.onPasswordConfirm()

        assertEquals("Password salah", viewModel.uiState.value.passwordError)
        assertTrue(viewModel.uiState.value.requiresPasswordConfirm)
        coVerify(exactly = 0) { requestLoanUseCase(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `onPasswordConfirmDismiss resets the dialog state`() = runTest {
        coEvery { getLimitSummaryUseCase() } returns Result.success(limitSummary)
        coEvery { getSavedBankAccountsUseCase() } returns Result.success(emptyList())
        val viewModel = createViewModel()
        viewModel.onAmountChange("500000")
        viewModel.onBankAccountNumberChange("123456")
        viewModel.onSubmitClick(mockk(relaxed = true))

        viewModel.onPasswordConfirmDismiss()

        assertFalse(viewModel.uiState.value.requiresPasswordConfirm)
    }
}
