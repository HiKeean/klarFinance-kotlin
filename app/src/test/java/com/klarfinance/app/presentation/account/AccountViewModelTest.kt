package com.klarfinance.app.presentation.account

import app.cash.turbine.test
import com.klarfinance.app.core.location.LocationScheduler
import com.klarfinance.app.core.session.SecureTokenStore
import com.klarfinance.app.core.session.SessionManager
import com.klarfinance.app.domain.model.AccountProfile
import com.klarfinance.app.domain.model.AccountState
import com.klarfinance.app.domain.model.Cached
import com.klarfinance.app.domain.usecase.ChangePasswordUseCase
import com.klarfinance.app.domain.usecase.GetLocationConsentUseCase
import com.klarfinance.app.domain.usecase.GetProfileUseCase
import com.klarfinance.app.domain.usecase.LogLocationFailureUseCase
import com.klarfinance.app.domain.usecase.LogoutUseCase
import com.klarfinance.app.domain.usecase.SetLocationConsentUseCase
import com.klarfinance.app.presentation.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AccountViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getProfileUseCase: GetProfileUseCase = mockk()
    private val changePasswordUseCase: ChangePasswordUseCase = mockk()
    private val logoutUseCase: LogoutUseCase = mockk()
    private val sessionManager: SessionManager = mockk(relaxed = true)
    private val secureTokenStore: SecureTokenStore = mockk(relaxed = true)
    private val getLocationConsentUseCase: GetLocationConsentUseCase = mockk()
    private val setLocationConsentUseCase: SetLocationConsentUseCase = mockk()
    private val logLocationFailureUseCase: LogLocationFailureUseCase = mockk(relaxed = true)
    private val locationScheduler: LocationScheduler = mockk(relaxed = true)

    private val profile = AccountProfile(
        name = "John",
        phone = "081234567890",
        role = "NASABAH",
        dob = "2000-01-01",
        email = "john@example.com",
        emailVerified = true,
        accountState = AccountState.ACTIVE,
    )

    @Before
    fun setUp() {
        every { secureTokenStore.hasRefreshToken() } returns false
        coEvery { getLocationConsentUseCase() } returns Result.success(false)
    }

    private fun createViewModel() = AccountViewModel(
        getProfileUseCase,
        changePasswordUseCase,
        logoutUseCase,
        sessionManager,
        secureTokenStore,
        getLocationConsentUseCase,
        setLocationConsentUseCase,
        logLocationFailureUseCase,
        locationScheduler,
    )

    @Test
    fun `init loads profile and location consent`() = runTest {
        coEvery { getProfileUseCase() } returns Result.success(Cached(profile, isFromCache = false))

        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(profile, viewModel.uiState.value.profile)
        assertFalse(viewModel.uiState.value.isLocationConsentGiven)
    }

    @Test
    fun `loadProfile sets loadErrorMessage on failure`() = runTest {
        coEvery { getProfileUseCase() } returns Result.failure(IllegalStateException("Gagal memuat akun"))

        val viewModel = createViewModel()

        assertEquals("Gagal memuat akun", viewModel.uiState.value.loadErrorMessage)
    }

    @Test
    fun `onLocationConsentEnabled schedules location and updates state on success`() = runTest {
        coEvery { getProfileUseCase() } returns Result.success(Cached(profile, isFromCache = false))
        val viewModel = createViewModel()
        coEvery { setLocationConsentUseCase(true) } returns Result.success(Unit)

        viewModel.onLocationConsentEnabled()

        verify(exactly = 1) { locationScheduler.schedule() }
        assertTrue(viewModel.uiState.value.isLocationConsentGiven)
        assertFalse(viewModel.uiState.value.isUpdatingLocationConsent)
    }

    @Test
    fun `onLocationConsentEnabled logs failure without a snackbar`() = runTest {
        coEvery { getProfileUseCase() } returns Result.success(Cached(profile, isFromCache = false))
        val viewModel = createViewModel()
        coEvery { setLocationConsentUseCase(true) } returns Result.failure(IllegalStateException("boom"))

        viewModel.snackbarMessage.test {
            viewModel.onLocationConsentEnabled()
            expectNoEvents()
        }
        coVerify(exactly = 1) { logLocationFailureUseCase("boom") }
        assertFalse(viewModel.uiState.value.isLocationConsentGiven)
    }

    @Test
    fun `onLocationConsentDisabled cancels the scheduler on success`() = runTest {
        coEvery { getProfileUseCase() } returns Result.success(Cached(profile, isFromCache = false))
        coEvery { getLocationConsentUseCase() } returns Result.success(true)
        val viewModel = createViewModel()
        coEvery { setLocationConsentUseCase(false) } returns Result.success(Unit)

        viewModel.onLocationConsentDisabled()

        verify(exactly = 1) { locationScheduler.cancel() }
        assertFalse(viewModel.uiState.value.isLocationConsentGiven)
    }

    @Test
    fun `onLocationConsentDisabled emits a snackbar on failure`() = runTest {
        coEvery { getProfileUseCase() } returns Result.success(Cached(profile, isFromCache = false))
        coEvery { getLocationConsentUseCase() } returns Result.success(true)
        val viewModel = createViewModel()
        coEvery { setLocationConsentUseCase(false) } returns Result.failure(IllegalStateException("Gagal menonaktifkan lokasi"))

        viewModel.snackbarMessage.test {
            viewModel.onLocationConsentDisabled()
            assertEquals("Gagal menonaktifkan lokasi", awaitItem())
        }
    }

    @Test
    fun `onLocationPermissionDenied logs a failure`() = runTest {
        coEvery { getProfileUseCase() } returns Result.success(Cached(profile, isFromCache = false))
        val viewModel = createViewModel()

        viewModel.onLocationPermissionDenied()

        coVerify(exactly = 1) { logLocationFailureUseCase("Location permission denied by user") }
    }

    @Test
    fun `onLogoutClick logs out and emits loggedOut`() = runTest {
        coEvery { getProfileUseCase() } returns Result.success(Cached(profile, isFromCache = false))
        val viewModel = createViewModel()
        coEvery { logoutUseCase() } returns Result.success(Unit)

        viewModel.loggedOut.test {
            viewModel.onLogoutClick()
            awaitItem()
        }
        assertFalse(viewModel.uiState.value.isLoggingOut)
    }

    @Test
    fun `onSubmitChangePassword fails fast when confirmation does not match`() = runTest {
        coEvery { getProfileUseCase() } returns Result.success(Cached(profile, isFromCache = false))
        val viewModel = createViewModel()
        viewModel.onOldPasswordChange("oldpass1")
        viewModel.onNewPasswordChange("newpass1")
        viewModel.onConfirmPasswordChange("different")

        viewModel.onSubmitChangePassword()

        assertEquals("Konfirmasi password tidak cocok", viewModel.uiState.value.changePasswordError)
        coVerify(exactly = 0) { changePasswordUseCase(any(), any()) }
    }

    @Test
    fun `onSubmitChangePassword succeeds and resets the form`() = runTest {
        coEvery { getProfileUseCase() } returns Result.success(Cached(profile, isFromCache = false))
        val viewModel = createViewModel()
        viewModel.onOldPasswordChange("oldpass1")
        viewModel.onNewPasswordChange("newpass1")
        viewModel.onConfirmPasswordChange("newpass1")
        coEvery { changePasswordUseCase("oldpass1", "newpass1") } returns Result.success(Unit)

        viewModel.onSubmitChangePassword()

        assertTrue(viewModel.uiState.value.changePasswordSuccess)
        assertEquals("", viewModel.uiState.value.newPassword)
        assertNull(viewModel.uiState.value.changePasswordError)
    }

    @Test
    fun `onSubmitChangePassword sets error on failure`() = runTest {
        coEvery { getProfileUseCase() } returns Result.success(Cached(profile, isFromCache = false))
        val viewModel = createViewModel()
        viewModel.onOldPasswordChange("oldpass1")
        viewModel.onNewPasswordChange("newpass1")
        viewModel.onConfirmPasswordChange("newpass1")
        coEvery { changePasswordUseCase("oldpass1", "newpass1") } returns Result.failure(IllegalStateException("Password lama salah"))

        viewModel.onSubmitChangePassword()

        assertEquals("Password lama salah", viewModel.uiState.value.changePasswordError)
        assertFalse(viewModel.uiState.value.changePasswordSuccess)
    }

    @Test
    fun `onToggleChangePassword clears the form fields`() = runTest {
        coEvery { getProfileUseCase() } returns Result.success(Cached(profile, isFromCache = false))
        val viewModel = createViewModel()
        viewModel.onOldPasswordChange("abc")

        viewModel.onToggleChangePassword()

        assertTrue(viewModel.uiState.value.isChangePasswordExpanded)
        assertEquals("", viewModel.uiState.value.oldPassword)
    }
}
