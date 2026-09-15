package com.klarfinance.app.presentation.register

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klarfinance.app.core.navigation.Screen
import com.klarfinance.app.core.ocr.KtpTextRecognizer
import com.klarfinance.app.core.scan.InstalledAppsScanner
import com.klarfinance.app.core.util.compressImageForUpload
import com.klarfinance.app.domain.model.LocationOption
import com.klarfinance.app.domain.model.RegisterResult
import com.klarfinance.app.domain.repository.LocationRepository
import com.klarfinance.app.domain.usecase.LoginUseCase
import com.klarfinance.app.domain.usecase.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val registerUseCase: RegisterUseCase,
    private val loginUseCase: LoginUseCase,
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val phone: String = checkNotNull(savedStateHandle[Screen.RegisterGraph.ARG_PHONE])

    private val _uiState = MutableStateFlow(RegisterUiState(phone = phone))
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _registerCompleted = MutableSharedFlow<RegisterResult>()
    val registerCompleted: SharedFlow<RegisterResult> = _registerCompleted.asSharedFlow()

    init {
        loadProvinces()
    }

    /** Auto-fill NIK/Nama dari foto KTP (konfirmasi user 2026-09-06, OCR on-device - lihat
     * KtpTextRecognizer) - best-effort, field tetap bisa dikoreksi manual di CompleteProfileScreen.
     * Aman dijalankan tiap kali KTP di-(re)capture karena user selalu lewat langkah ini SEBELUM
     * CompleteProfileScreen (lihat KlarNavHost), jadi tidak akan menimpa input manual. */
    fun onKtpCaptured(uri: Uri) {
        _uiState.update { it.copy(ktpPhotoUri = uri) }
        viewModelScope.launch {
            val result = KtpTextRecognizer.recognize(appContext, uri)
            _uiState.update {
                it.copy(
                    fullName = result.name ?: it.fullName,
                    nik = result.nik ?: it.nik,
                )
            }
        }
    }

    fun onKycCaptured(uri: Uri) {
        _uiState.update { it.copy(kycPhotoUri = uri) }
    }

    fun onFullNameChange(value: String) = _uiState.update { it.copy(fullName = value, errorMessage = null) }

    fun onNikChange(value: String) =
        _uiState.update { it.copy(nik = value.filter(Char::isDigit).take(16), errorMessage = null) }

    fun onDobSelected(epochMillis: Long) {
        val date = java.time.Instant.ofEpochMilli(epochMillis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
        _uiState.update { it.copy(dob = date, errorMessage = null) }
    }

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, errorMessage = null) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }

    fun onTogglePasswordVisibility() = _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    fun onAddressChange(value: String) = _uiState.update { it.copy(address = value, errorMessage = null) }

    fun onReferralCodeChange(value: String) = _uiState.update { it.copy(referralCode = value.uppercase(), errorMessage = null) }

    fun onProvinceSelected(option: LocationOption) {
        _uiState.update {
            it.copy(
                province = option,
                regency = null,
                district = null,
                village = null,
                regencyOptions = emptyList(),
                districtOptions = emptyList(),
                villageOptions = emptyList(),
            )
        }
        loadRegencies(option.id)
    }

    fun onRegencySelected(option: LocationOption) {
        _uiState.update {
            it.copy(
                regency = option,
                district = null,
                village = null,
                districtOptions = emptyList(),
                villageOptions = emptyList(),
            )
        }
        loadDistricts(option.id)
    }

    fun onDistrictSelected(option: LocationOption) {
        _uiState.update { it.copy(district = option, village = null, villageOptions = emptyList()) }
        loadVillages(option.id)
    }

    fun onVillageSelected(option: LocationOption) {
        _uiState.update { it.copy(village = option) }
    }

    private fun loadProvinces() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRegions = true) }
            locationRepository.getProvinces()
                .onSuccess { options -> _uiState.update { it.copy(provinceOptions = options, isLoadingRegions = false) } }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoadingRegions = false, errorMessage = throwable.message ?: "Failed to load provinces")
                    }
                }
        }
    }

    private fun loadRegencies(provinceId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRegions = true) }
            locationRepository.getRegencies(provinceId)
                .onSuccess { options -> _uiState.update { it.copy(regencyOptions = options, isLoadingRegions = false) } }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoadingRegions = false, errorMessage = throwable.message ?: "Failed to load regencies")
                    }
                }
        }
    }

    private fun loadDistricts(regencyId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRegions = true) }
            locationRepository.getDistricts(regencyId)
                .onSuccess { options -> _uiState.update { it.copy(districtOptions = options, isLoadingRegions = false) } }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoadingRegions = false, errorMessage = throwable.message ?: "Failed to load districts")
                    }
                }
        }
    }

    private fun loadVillages(districtId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRegions = true) }
            locationRepository.getVillages(districtId)
                .onSuccess { options -> _uiState.update { it.copy(villageOptions = options, isLoadingRegions = false) } }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoadingRegions = false, errorMessage = throwable.message ?: "Failed to load villages")
                    }
                }
        }
    }

    fun onSubmitProfileClick() {
        val state = _uiState.value
        if (!state.isProfileFormValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            val fotoKtp: MultipartBody.Part?
            val fotoKyc: MultipartBody.Part?
            try {
                fotoKtp = state.ktpPhotoUri?.let { uriToPart(it, "fotoKtp") }
                fotoKyc = state.kycPhotoUri?.let { uriToPart(it, "fotoKyc") }
            } catch (e: IllegalStateException) {
                _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = e.message ?: "Failed to process photo, please retake it")
                }
                return@launch
            }

            // Device app scoring (BRD/lending-flow.md) - dulu selalu kosong (app ini tidak pernah
            // benar-benar scan), lihat InstalledAppsScanner untuk detail & caveat package name.
            val scanResult = withContext(Dispatchers.IO) { InstalledAppsScanner.scan(appContext) }

            registerUseCase(
                phone = state.phone,
                nik = state.nik,
                name = state.fullName,
                email = state.email,
                address = state.address,
                dob = state.dob?.toString().orEmpty(),
                villageId = state.village?.id,
                password = state.password,
                referralCode = state.referralCode,
                fotoKtp = fotoKtp,
                fotoKyc = fotoKyc,
                pinjolApps = scanResult.pinjolApps,
                bankApps = scanResult.bankApps,
            )
                .onSuccess { result ->
                    // Best-effort: establishes a session right after registering (same
                    // phone/password just submitted) so the Account screen has a token to
                    // work with on the PENDING_APPLICATION dashboard - registration itself
                    // already succeeded, so a login hiccup here shouldn't block navigation.
                    loginUseCase(state.phone, state.password)
                    _uiState.update { it.copy(isSubmitting = false) }
                    _registerCompleted.emit(result)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isSubmitting = false, errorMessage = throwable.message ?: "Registration failed")
                    }
                }
        }
    }

    /** Compresses to <=10MB before upload - see [compressImageForUpload] and the
     * matching `spring.servlet.multipart.max-file-size` on the backend. */
    private suspend fun uriToPart(uri: Uri, partName: String): MultipartBody.Part = withContext(Dispatchers.Default) {
        val bytes = compressImageForUpload(appContext, uri)
        val requestBody = bytes.toRequestBody("image/jpeg".toMediaType())
        MultipartBody.Part.createFormData(partName, "$partName.jpg", requestBody)
    }
}
