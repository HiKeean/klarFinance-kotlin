package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.model.RegisterResult
import com.klarfinance.app.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MultipartBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RegisterUseCaseTest {

    private val repository: AuthRepository = mockk()
    private lateinit var useCase: RegisterUseCase
    private val ktpPhoto: MultipartBody.Part = mockk(relaxed = true)
    private val selfiePhoto: MultipartBody.Part = mockk(relaxed = true)

    @Before
    fun setUp() {
        useCase = RegisterUseCase(repository)
    }

    private fun validArgs(
        name: String = "John Doe",
        nik: String = "1234567890123456",
        dob: String = "2000-01-01",
        email: String = "john@example.com",
        password: String = "password1",
        address: String = "Jl. Merdeka",
        villageId: Long? = 42L,
        fotoKtp: MultipartBody.Part? = ktpPhoto,
        fotoKyc: MultipartBody.Part? = selfiePhoto,
    ) = suspend {
        useCase(
            phone = "0812-3456-7890",
            nik = nik,
            name = name,
            email = email,
            address = address,
            dob = dob,
            villageId = villageId,
            password = password,
            fotoKtp = fotoKtp,
            fotoKyc = fotoKyc,
        )
    }

    @Test
    fun `blank name fails without calling repository`() = runTest {
        val result = validArgs(name = "  ")()

        assertTrue(result.isFailure)
        assertEquals("Full name is required", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.register(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `nik not 16 digits fails`() = runTest {
        val result = validArgs(nik = "123")()

        assertTrue(result.isFailure)
        assertEquals("NIK must be 16 digits", result.exceptionOrNull()?.message)
    }

    @Test
    fun `blank dob fails`() = runTest {
        val result = validArgs(dob = "")()

        assertEquals("Date of birth is required", result.exceptionOrNull()?.message)
    }

    @Test
    fun `email without at-sign fails`() = runTest {
        val result = validArgs(email = "not-an-email")()

        assertEquals("Enter a valid email address", result.exceptionOrNull()?.message)
    }

    @Test
    fun `password shorter than 8 chars fails`() = runTest {
        val result = validArgs(password = "short1")()

        assertEquals("Password must be at least 8 characters", result.exceptionOrNull()?.message)
    }

    @Test
    fun `blank address fails`() = runTest {
        val result = validArgs(address = "   ")()

        assertEquals("Address is required", result.exceptionOrNull()?.message)
    }

    @Test
    fun `null villageId fails`() = runTest {
        val result = validArgs(villageId = null)()

        assertEquals("Please select your full region", result.exceptionOrNull()?.message)
    }

    @Test
    fun `null ktp photo fails`() = runTest {
        val result = validArgs(fotoKtp = null)()

        assertEquals("KTP photo is required", result.exceptionOrNull()?.message)
    }

    @Test
    fun `null selfie photo fails`() = runTest {
        val result = validArgs(fotoKyc = null)()

        assertEquals("Selfie photo is required", result.exceptionOrNull()?.message)
    }

    @Test
    fun `valid input trims strings, strips non-digits, and delegates to repository`() = runTest {
        val expected = RegisterResult(identity = "081234567890", applicationStatus = "PENDING", suggestedLimit = null, message = null)
        coEvery {
            repository.register(
                phone = "081234567890",
                nik = "1234567890123456",
                name = "John Doe",
                email = "john@example.com",
                address = "Jl. Merdeka",
                dob = "2000-01-01",
                villageId = 42L,
                password = "password1",
                referralCode = "",
                fotoKtp = ktpPhoto,
                fotoKyc = selfiePhoto,
                pinjolApps = emptyList(),
                bankApps = emptyList(),
            )
        } returns Result.success(expected)

        val result = validArgs(name = "  John Doe  ", email = " john@example.com ", address = " Jl. Merdeka ")()

        assertEquals(Result.success(expected), result)
    }
}
