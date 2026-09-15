package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChangePasswordUseCaseTest {

    private val repository: AuthRepository = mockk()
    private lateinit var useCase: ChangePasswordUseCase

    @Before
    fun setUp() {
        useCase = ChangePasswordUseCase(repository)
    }

    @Test
    fun `blank old password fails without calling repository`() = runTest {
        val result = useCase("", "newpassword1")

        assertTrue(result.isFailure)
        assertEquals("Masukkan password lama", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.changePassword(any(), any()) }
    }

    @Test
    fun `new password shorter than 8 chars fails`() = runTest {
        val result = useCase("oldpassword", "short1")

        assertTrue(result.isFailure)
        assertEquals("Password baru minimal 8 karakter", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.changePassword(any(), any()) }
    }

    @Test
    fun `new password of exactly 8 chars passes validation`() = runTest {
        coEvery { repository.changePassword("oldpassword", "newpass1") } returns Result.success(Unit)

        val result = useCase("oldpassword", "newpass1")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.changePassword("oldpassword", "newpass1") }
    }
}
