package com.klarfinance.app.presentation.passwordlogin

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.klarfinance.app.core.navigation.Screen
import com.klarfinance.app.domain.model.AccountState
import com.klarfinance.app.domain.model.LoginResult
import com.klarfinance.app.domain.usecase.LoginUseCase
import com.klarfinance.app.presentation.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PasswordLoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loginUseCase: LoginUseCase = mockk()
    private val savedStateHandle: SavedStateHandle = mockk()
    private lateinit var viewModel: PasswordLoginViewModel

    private val phone = "6281234567890"

    @Before
    fun setUp() {
        every { savedStateHandle.get<String>(Screen.PasswordLogin.ARG_PHONE) } returns phone
        viewModel = PasswordLoginViewModel(loginUseCase, savedStateHandle)
    }

    @Test
    fun `initial state seeds phone from saved state`() {
        assertEquals(phone, viewModel.uiState.value.phone)
        assertEquals("", viewModel.uiState.value.password)
    }

    @Test
    fun `onLoginClick does nothing when password is blank`() = runTest {
        viewModel.onLoginClick()

        coVerify(exactly = 0) { loginUseCase(any(), any()) }
    }

    @Test
    fun `onLoginClick emits loginSucceeded with the resulting account state`() = runTest {
        viewModel.onPasswordChange("secret123")
        val loginResult = LoginResult(identity = phone, name = "John", accountState = AccountState.ACTIVE)
        coEvery { loginUseCase(phone, "secret123") } returns Result.success(loginResult)

        viewModel.loginSucceeded.test {
            viewModel.onLoginClick()
            assertEquals(AccountState.ACTIVE, awaitItem())
        }
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `onLoginClick sets errorMessage on failure`() = runTest {
        viewModel.onPasswordChange("wrongpass")
        coEvery { loginUseCase(phone, "wrongpass") } returns Result.failure(IllegalStateException("Invalid credentials"))

        viewModel.onLoginClick()

        assertEquals("Invalid credentials", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
