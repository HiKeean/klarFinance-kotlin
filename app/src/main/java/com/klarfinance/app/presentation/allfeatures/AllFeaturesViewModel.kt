package com.klarfinance.app.presentation.allfeatures

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klarfinance.app.domain.model.AccountState
import com.klarfinance.app.domain.usecase.GetProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Resolves its own [accountState] via [GetProfileUseCase] instead of taking it as a nav arg -
 * this screen is now reachable from several places (Home's "More" quick action, Home's section
 * arrows, the "Loans" bottom-nav tab from Home/History/Account/itself), so having each entry
 * point correctly thread the live accountState through would be repetitive and error-prone
 * (get it wrong and a PENDING_APPLICATION user sees the wrong locked-tap response). Defaults to
 * PENDING_APPLICATION while loading - same convention as [AccountState.fromBackend] treating an
 * unresolved-but-logged-in state as PENDING rather than assuming ACTIVE.
 */
@HiltViewModel
class AllFeaturesViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
) : ViewModel() {

    private val _accountState = MutableStateFlow(AccountState.PENDING_APPLICATION)
    val accountState: StateFlow<AccountState> = _accountState.asStateFlow()

    init {
        viewModelScope.launch {
            getProfileUseCase().onSuccess { cached -> _accountState.value = cached.value.accountState }
        }
    }
}
