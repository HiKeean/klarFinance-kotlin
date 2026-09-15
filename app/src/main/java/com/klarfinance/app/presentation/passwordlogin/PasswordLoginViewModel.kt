package com.klarfinance.app.presentation.passwordlogin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klarfinance.app.core.navigation.Screen
import com.klarfinance.app.domain.model.AccountState
import com.klarfinance.app.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PasswordLoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val phone: String = checkNotNull(savedStateHandle[Screen.PasswordLogin.ARG_PHONE])

    private val _uiState = MutableStateFlow(PasswordLoginUiState(phone = phone))
    val uiState: StateFlow<PasswordLoginUiState> = _uiState.asStateFlow()

    private val _loginSucceeded = MutableSharedFlow<AccountState>()
    val loginSucceeded: SharedFlow<AccountState> = _loginSucceeded.asSharedFlow()

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onLoginClick() {
        val state = _uiState.value
        if (!state.isLoginEnabled) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            loginUseCase(state.phone, state.password)
                .onSuccess { result ->
                    _uiState.update { it.copy(isLoading = false) }
                    _loginSucceeded.emit(result.accountState)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = throwable.message ?: "Login failed")
                    }
                }
        }
    }
}
