package com.klarfinance.app.presentation.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klarfinance.app.core.navigation.Screen
import com.klarfinance.app.core.notification.FcmEventBus
import com.klarfinance.app.domain.model.AccountState
import com.klarfinance.app.domain.model.LimitSummary
import com.klarfinance.app.domain.usecase.GetLimitSummaryUseCase
import com.klarfinance.app.domain.usecase.GetProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val LOAN_APPROVAL_EVENT_TYPE = "LOAN_APPROVAL"

/** BM approve/reject pengajuan tarik tunai >30% plafond (LoanReviewService.bmDecide di backend) -
 * beda dari LOAN_APPROVAL (approval plafond awal), event ini tidak mengubah AccountState, cuma
 * availableLimit/usedLimit/hasPendingLoanReview nasabah - jadi cukup refresh limit summary,
 * tidak perlu refreshAccountState. */
private const val LOAN_REVIEW_DECISION_EVENT_TYPE = "LOAN_REVIEW_DECISION"

/**
 * Owns [accountState] as live, observable state instead of Home only ever reading it once from
 * a nav arg (which is how the FCM-approval bug this fixes happened - see kotlin-nasabah-app
 * knowledge, "State model 3-tingkat": the nav-arg value is frozen the moment Home is composed).
 *
 * Scoped to the `home?accountState=...` backstack entry, same as the nav arg used to be - a
 * fresh instance (and fresh initial value) is created whenever KlarNavHost pushes a brand new
 * Home entry (login/register/logout all do `popUpTo(Home){inclusive=true}` + navigate, see
 * KlarNavHost), so this doesn't need to handle "the account itself changed identity", only
 * "the same account's status changed while this Home instance is alive".
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getLimitSummaryUseCase: GetLimitSummaryUseCase,
    fcmEventBus: FcmEventBus,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val initialAccountState = AccountState.valueOf(
        savedStateHandle.get<String>(Screen.Home.ARG_ACCOUNT_STATE) ?: AccountState.GUEST.name,
    )

    private val _accountState = MutableStateFlow(initialAccountState)
    val accountState: StateFlow<AccountState> = _accountState.asStateFlow()

    /** Real totalLimit/usedLimit/availableLimit for the "Available Loan" card, replacing the
     * old hardcoded placeholder numbers - see LoanRepository.getLimitSummary. Null while
     * loading/not-yet-fetched (GUEST/PENDING_APPLICATION never have an ActiveLimit to fetch, so
     * this simply stays null for them - HomeScreen keeps its existing pending-state UI either way). */
    private val _limitSummary = MutableStateFlow<LimitSummary?>(null)
    val limitSummary: StateFlow<LimitSummary?> = _limitSummary.asStateFlow()

    init {
        if (initialAccountState == AccountState.ACTIVE) {
            viewModelScope.launch { refreshLimitSummary() }
        }

        // GUEST has no session/token to query a status for - only listen once there's actually
        // an account, matching every other gate on this screen (see HomeScreen.onLockedFeatureClick).
        if (initialAccountState != AccountState.GUEST) {
            viewModelScope.launch {
                fcmEventBus.events.collect { event ->
                    when (event.type) {
                        LOAN_APPROVAL_EVENT_TYPE -> refreshAccountState()
                        LOAN_REVIEW_DECISION_EVENT_TYPE -> refreshLimitSummary()
                    }
                }
            }
        }
    }

    /** Dipanggil dari LifecycleEventEffect(ON_RESUME) di KlarNavHost - instance Home yang sama
     * tetap hidup di backstack selama alur Ajukan Pinjaman (RequestLoanGraph) atau QRIS
     * (di-push, bukan popUpTo Home), jadi begitu balik ke Home setelah transaksi SELESAI SAAT ITU
     * JUGA (pinjaman auto-cair di bawah ambang review, atau QRIS berhasil), kotak plafond perlu
     * di-refresh manual di sini - tidak ada FCM push buat kasus sinkron ini (beda dari kasus BM
     * approve/reject yang asinkron lewat LOAN_APPROVAL/LOAN_REVIEW_DECISION di atas). */
    fun onResume() {
        if (_accountState.value == AccountState.ACTIVE) {
            viewModelScope.launch { refreshLimitSummary() }
        }
    }

    private suspend fun refreshAccountState() {
        // Re-fetch from the backend rather than trusting the push payload's `status` directly -
        // the push is just a "something changed, go check" signal; GET /profile stays the single
        // source of truth for what AccountState actually is right now (see AccountProfile.accountState).
        getProfileUseCase().onSuccess { cached ->
            _accountState.value = cached.value.accountState
            // A BM approval push is exactly the moment PENDING_APPLICATION -> ACTIVE happens,
            // i.e. the moment an ActiveLimit (and therefore a fetchable limit summary) starts
            // existing - fetch it now instead of waiting for the next cold Home.
            if (cached.value.accountState == AccountState.ACTIVE) refreshLimitSummary()
        }
    }

    private suspend fun refreshLimitSummary() {
        getLimitSummaryUseCase().onSuccess { summary -> _limitSummary.value = summary }
    }
}
