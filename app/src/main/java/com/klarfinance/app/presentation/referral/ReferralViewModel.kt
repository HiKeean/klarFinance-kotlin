package com.klarfinance.app.presentation.referral

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klarfinance.app.domain.usecase.GetReferralSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReferralViewModel @Inject constructor(
    private val getReferralSummaryUseCase: GetReferralSummaryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReferralUiState())
    val uiState: StateFlow<ReferralUiState> = _uiState.asStateFlow()

    init {
        loadSummary()
    }

    fun loadSummary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadErrorMessage = null) }
            getReferralSummaryUseCase()
                .onSuccess { cached ->
                    _uiState.update { it.copy(isLoading = false, summary = cached.value, isOffline = cached.isFromCache) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, loadErrorMessage = throwable.message ?: "Gagal memuat data referral")
                    }
                }
        }
    }
}
