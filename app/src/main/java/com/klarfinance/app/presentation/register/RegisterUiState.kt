package com.klarfinance.app.presentation.register

import android.net.Uri
import com.klarfinance.app.domain.model.LocationOption
import java.time.LocalDate

data class RegisterUiState(
    val phone: String = "",

    val ktpPhotoUri: Uri? = null,
    val kycPhotoUri: Uri? = null,

    val fullName: String = "",
    val nik: String = "",
    val dob: LocalDate? = null,
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,

    val address: String = "",
    val referralCode: String = "",
    val province: LocationOption? = null,
    val regency: LocationOption? = null,
    val district: LocationOption? = null,
    val village: LocationOption? = null,

    val provinceOptions: List<LocationOption> = emptyList(),
    val regencyOptions: List<LocationOption> = emptyList(),
    val districtOptions: List<LocationOption> = emptyList(),
    val villageOptions: List<LocationOption> = emptyList(),
    val isLoadingRegions: Boolean = false,

    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
) {
    val hasBothPhotos: Boolean
        get() = ktpPhotoUri != null && kycPhotoUri != null

    val isProfileFormValid: Boolean
        get() = fullName.isNotBlank() &&
            nik.length == 16 &&
            dob != null &&
            email.contains("@") &&
            password.length >= 8 &&
            address.isNotBlank() &&
            village != null &&
            !isSubmitting
}
