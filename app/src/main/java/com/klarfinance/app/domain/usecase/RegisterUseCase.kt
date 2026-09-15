package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.model.RegisterResult
import com.klarfinance.app.domain.repository.AuthRepository
import okhttp3.MultipartBody
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(
        phone: String,
        nik: String,
        name: String,
        email: String,
        address: String,
        dob: String,
        villageId: Long?,
        password: String,
        referralCode: String = "",
        fotoKtp: MultipartBody.Part?,
        fotoKyc: MultipartBody.Part?,
        pinjolApps: List<String> = emptyList(),
        bankApps: List<String> = emptyList(),
    ): Result<RegisterResult> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Full name is required"))
        if (nik.filter(Char::isDigit).length != 16) return Result.failure(IllegalArgumentException("NIK must be 16 digits"))
        if (dob.isBlank()) return Result.failure(IllegalArgumentException("Date of birth is required"))
        if (!email.contains("@")) return Result.failure(IllegalArgumentException("Enter a valid email address"))
        if (password.length < 8) return Result.failure(IllegalArgumentException("Password must be at least 8 characters"))
        if (address.isBlank()) return Result.failure(IllegalArgumentException("Address is required"))
        if (villageId == null) return Result.failure(IllegalArgumentException("Please select your full region"))
        if (fotoKtp == null) return Result.failure(IllegalArgumentException("KTP photo is required"))
        if (fotoKyc == null) return Result.failure(IllegalArgumentException("Selfie photo is required"))

        return repository.register(
            phone = phone.filter(Char::isDigit),
            nik = nik.filter(Char::isDigit),
            name = name.trim(),
            email = email.trim(),
            address = address.trim(),
            dob = dob,
            villageId = villageId,
            password = password,
            referralCode = referralCode.trim(),
            fotoKtp = fotoKtp,
            fotoKyc = fotoKyc,
            pinjolApps = pinjolApps,
            bankApps = bankApps,
        )
    }
}
