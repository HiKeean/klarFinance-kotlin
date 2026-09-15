package com.klarfinance.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.klarfinance.app.domain.model.AccountState
import com.klarfinance.app.domain.model.FeatureCategoryKey
import com.klarfinance.app.presentation.account.AccountScreen
import com.klarfinance.app.presentation.allfeatures.AllFeaturesScreen
import com.klarfinance.app.presentation.bills.BillsScreen
import com.klarfinance.app.presentation.history.HistoryScreen
import com.klarfinance.app.presentation.history.HistoryViewModel
import com.klarfinance.app.presentation.history.PaymentScreen
import com.klarfinance.app.presentation.home.HomeScreen
import com.klarfinance.app.presentation.home.HomeViewModel
import com.klarfinance.app.presentation.loan.RequestLoanViewModel
import com.klarfinance.app.presentation.loan.amount.LoanAmountScreen
import com.klarfinance.app.presentation.loan.bankaccount.LoanBankAccountScreen
import com.klarfinance.app.presentation.login.LoginScreen
import com.klarfinance.app.presentation.otp.OtpVerificationScreen
import com.klarfinance.app.presentation.passwordlogin.PasswordLoginScreen
import com.klarfinance.app.presentation.qris.amount.QrisAmountScreen
import com.klarfinance.app.presentation.qris.scan.QrisScanScreen
import com.klarfinance.app.presentation.referral.ReferralScreen
import com.klarfinance.app.presentation.register.RegisterViewModel
import com.klarfinance.app.presentation.register.ktpscan.KtpScanScreen
import com.klarfinance.app.presentation.register.profile.CompleteProfileScreen
import com.klarfinance.app.presentation.register.selfie.SelfieCaptureScreen
import com.klarfinance.app.presentation.register.success.RegisterSuccessScreen
import com.klarfinance.app.presentation.register.verify.VerifyIdentityScreen
import com.klarfinance.app.presentation.splash.SplashScreen
import com.klarfinance.app.presentation.transjakarta.TransjakartaPurchaseViewModel
import com.klarfinance.app.presentation.transjakarta.confirm.TransjakartaConfirmScreen
import com.klarfinance.app.presentation.transjakarta.home.TransjakartaHomeScreen

@Composable
fun KlarNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onTimeout = { accountState ->
                    navController.navigate(Screen.Home.createRoute(accountState)) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Screen.Home.route,
            arguments = listOf(
                navArgument(Screen.Home.ARG_ACCOUNT_STATE) {
                    type = NavType.StringType
                    defaultValue = AccountState.GUEST.name
                },
            ),
        ) {
            val viewModel = hiltViewModel<HomeViewModel>()
            val accountState by viewModel.accountState.collectAsStateWithLifecycle()
            val limitSummary by viewModel.limitSummary.collectAsStateWithLifecycle()
            // Home instance ini tetap hidup di backstack selama alur Ajukan Pinjaman/QRIS (push,
            // bukan popUpTo Home) - refresh kotak plafond tiap balik ke Home (termasuk begitu
            // pinjaman auto-cair atau QRIS selesai), lihat HomeViewModel.onResume().
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
            HomeScreen(
                onLoginRequested = { navController.navigate(Screen.Login.route) },
                onAccountClick = { navController.navigate(Screen.Account.route) },
                onHistoryClick = { navController.navigate(Screen.HistoryGraph.route) },
                onBillsClick = { navController.navigate(Screen.Bills.route) },
                onRequestLoanClick = { navController.navigate(Screen.RequestLoanGraph.route) },
                onPayClick = { navController.navigate(Screen.QrisScan.route) },
                onTransjakartaClick = { navController.navigate(Screen.TransjakartaGraph.route) },
                onMoreClick = { navController.navigate(Screen.AllFeatures.route) },
                onSectionMoreClick = { key -> navController.navigate(Screen.AllFeatures.createRoute(scrollTo = key)) },
                accountState = accountState,
                limitSummary = limitSummary,
            )
        }

        composable(
            route = Screen.AllFeatures.route,
            arguments = listOf(
                navArgument(Screen.AllFeatures.ARG_SCROLL_TO) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val scrollTo = backStackEntry.arguments?.getString(Screen.AllFeatures.ARG_SCROLL_TO)
                ?.let { runCatching { FeatureCategoryKey.valueOf(it) }.getOrNull() }
            AllFeaturesScreen(
                onHomeClick = { navController.popBackStack(Screen.Home.route, inclusive = false) },
                onAccountClick = {
                    navController.navigate(Screen.Account.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onHistoryClick = {
                    navController.navigate(Screen.HistoryGraph.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onTransjakartaClick = { navController.navigate(Screen.TransjakartaGraph.route) },
                scrollToCategory = scrollTo,
            )
        }

        historyGraph(navController)

        composable(Screen.Referral.route) {
            ReferralScreen(onBackClick = { navController.popBackStack() })
        }

        transjakartaGraph(navController)

        composable(Screen.QrisScan.route) {
            QrisScanScreen(
                onScanned = { merchantCode ->
                    navController.navigate(Screen.QrisAmount.createRoute(merchantCode)) {
                        popUpTo(Screen.QrisScan.route) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.QrisAmount.route,
            arguments = listOf(navArgument(Screen.QrisAmount.ARG_MERCHANT_CODE) { type = NavType.StringType }),
        ) {
            QrisAmountScreen(
                onBackClick = { navController.popBackStack() },
                // Selesai (sukses ATAU error fatal) - balik ke Home, konsisten sama pola
                // LoanBankAccountScreen.onDone (bukan cuma popBackStack ke scan lagi).
                onDone = { navController.popBackStack(Screen.Home.route, inclusive = false) },
            )
        }

        requestLoanGraph(navController)

        composable(Screen.Account.route) {
            AccountScreen(
                onHomeClick = { navController.popBackStack() },
                onReferralClick = { navController.navigate(Screen.Referral.route) },
                onHistoryClick = {
                    navController.navigate(Screen.HistoryGraph.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onLoansClick = {
                    navController.navigate(Screen.AllFeatures.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onLoggedOut = {
                    // Same pattern as RegisterSuccess/PasswordLogin - drop the old (signed-in)
                    // Home plus Account itself, leaving a fresh GUEST Home as the sole entry.
                    navController.navigate(Screen.Home.createRoute(AccountState.GUEST)) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onOtpRequested = { phone ->
                    navController.navigate(Screen.OtpVerification.createRoute(phone))
                },
                onAlreadyVerified = { phone ->
                    navController.navigate(Screen.RegisterGraph.createRoute(phone)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNeedsPasswordLogin = { phone ->
                    navController.navigate(Screen.PasswordLogin.createRoute(phone))
                },
            )
        }

        composable(
            route = Screen.OtpVerification.route,
            arguments = listOf(navArgument(Screen.OtpVerification.ARG_PHONE) { type = NavType.StringType }),
        ) {
            OtpVerificationScreen(
                onBackClick = { navController.popBackStack() },
                onVerified = { phone ->
                    navController.navigate(Screen.RegisterGraph.createRoute(phone)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNeedsPasswordLogin = { phone ->
                    navController.navigate(Screen.PasswordLogin.createRoute(phone))
                },
            )
        }

        composable(
            route = Screen.PasswordLogin.route,
            arguments = listOf(navArgument(Screen.PasswordLogin.ARG_PHONE) { type = NavType.StringType }),
        ) {
            PasswordLoginScreen(
                onBackClick = { navController.popBackStack() },
                onLoginSucceeded = { accountState ->
                    // Same pattern as RegisterSuccess.onDashboardClick - drop the old (guest)
                    // Home plus everything from Login/Otp/PasswordLogin, leaving the fresh Home
                    // as the only backstack entry.
                    navController.navigate(Screen.Home.createRoute(accountState)) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
            )
        }

        registerGraph(navController)
    }
}

private fun NavGraphBuilder.registerGraph(navController: NavHostController) {
    navigation(
        startDestination = Screen.KtpScan.route,
        route = Screen.RegisterGraph.route,
        arguments = listOf(navArgument(Screen.RegisterGraph.ARG_PHONE) { type = NavType.StringType }),
    ) {
        composable(Screen.KtpScan.route) { backStackEntry ->
            val viewModel = backStackEntry.registerViewModel(navController)
            KtpScanScreen(
                viewModel = viewModel,
                onCaptured = { navController.navigate(Screen.VerifyIdentity.route) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Screen.SelfieCapture.route) { backStackEntry ->
            val viewModel = backStackEntry.registerViewModel(navController)
            SelfieCaptureScreen(
                viewModel = viewModel,
                onCaptured = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Screen.VerifyIdentity.route) { backStackEntry ->
            val viewModel = backStackEntry.registerViewModel(navController)
            VerifyIdentityScreen(
                viewModel = viewModel,
                onRetakeKtp = {
                    navController.navigate(Screen.KtpScan.route) {
                        popUpTo(Screen.VerifyIdentity.route) { inclusive = true }
                    }
                },
                onTakeSelfie = { navController.navigate(Screen.SelfieCapture.route) },
                onContinueClick = { navController.navigate(Screen.CompleteProfile.route) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Screen.CompleteProfile.route) { backStackEntry ->
            val viewModel = backStackEntry.registerViewModel(navController)
            CompleteProfileScreen(
                viewModel = viewModel,
                onRegistered = {
                    navController.navigate(Screen.RegisterSuccess.route) {
                        popUpTo(Screen.RegisterGraph.route) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Screen.RegisterSuccess.route) {
            RegisterSuccessScreen(
                onDashboardClick = {
                    // Account exists now (KYC submitted) but the application isn't decided
                    // yet - PENDING_APPLICATION, not GUEST. popUpTo(Home, inclusive=true)
                    // drops the OLD (guest) Home instance too, along with everything from
                    // Login/OtpVerification/RegisterGraph - the fresh Home this pushes is the
                    // only backstack entry left, so system back from here exits the app
                    // instead of falling back into Login/Register.
                    navController.navigate(Screen.Home.createRoute(AccountState.PENDING_APPLICATION)) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
            )
        }
    }
}

/**
 * All register-flow screens share one [RegisterViewModel] instance, scoped to the
 * `register/{phone}` nested graph entry rather than each individual step - this is what
 * lets captured photos and form fields survive navigation between the 4 steps.
 */
@Composable
private fun NavBackStackEntry.registerViewModel(navController: NavHostController): RegisterViewModel {
    val parentEntry = remember(this) { navController.getBackStackEntry(Screen.RegisterGraph.route) }
    return hiltViewModel(parentEntry)
}

/** History (read-only), Bills (tagihan belum lunas), dan Payment (halaman Bayar) berbagi satu
 * [HistoryViewModel] - sama pola dengan [requestLoanGraph] - supaya Bills/Payment gak perlu
 * network call baru buat data cicilan yang sudah kebawa dari GET /loan/history. */
private fun NavGraphBuilder.historyGraph(navController: NavHostController) {
    navigation(startDestination = Screen.History.route, route = Screen.HistoryGraph.route) {
        composable(Screen.History.route) { backStackEntry ->
            val viewModel = backStackEntry.historyViewModel(navController)
            HistoryScreen(
                // History selalu dipush langsung di atas Home (dari tab Home ATAU Account,
                // lihat wiring di bawah), sama pola dengan onHomeClick punya AccountScreen -
                // satu pop cukup buat balik ke Home.
                onHomeClick = { navController.popBackStack(Screen.Home.route, inclusive = false) },
                onAccountClick = {
                    navController.navigate(Screen.Account.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onLoansClick = {
                    navController.navigate(Screen.AllFeatures.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                viewModel = viewModel,
            )
        }

        composable(Screen.Bills.route) { backStackEntry ->
            val viewModel = backStackEntry.historyViewModel(navController)
            BillsScreen(
                onBackClick = { navController.popBackStack() },
                onBayarClick = { item -> navController.navigate(Screen.Payment.createRoute(item.loanId)) },
                viewModel = viewModel,
            )
        }

        composable(
            route = Screen.Payment.route,
            arguments = listOf(navArgument(Screen.Payment.ARG_LOAN_ID) { type = NavType.IntType }),
        ) { backStackEntry ->
            val viewModel = backStackEntry.historyViewModel(navController)
            val loanId = backStackEntry.arguments?.getInt(Screen.Payment.ARG_LOAN_ID) ?: 0
            PaymentScreen(
                loanId = loanId,
                onBackClick = { navController.popBackStack() },
                viewModel = viewModel,
            )
        }
    }
}

/** Sama pola dengan [requestLoanViewModel]/[registerViewModel] - History dan Payment berbagi satu
 * instance supaya jadwal cicilan yang sudah dimuat di History masih ada pas Payment dibuka. */
@Composable
private fun NavBackStackEntry.historyViewModel(navController: NavHostController): HistoryViewModel {
    val parentEntry = remember(this) { navController.getBackStackEntry(Screen.HistoryGraph.route) }
    return hiltViewModel(parentEntry)
}

private fun NavGraphBuilder.requestLoanGraph(navController: NavHostController) {
    navigation(startDestination = Screen.LoanAmount.route, route = Screen.RequestLoanGraph.route) {
        composable(Screen.LoanAmount.route) { backStackEntry ->
            val viewModel = backStackEntry.requestLoanViewModel(navController)
            LoanAmountScreen(
                onBackClick = { navController.popBackStack() },
                onContinueClick = { navController.navigate(Screen.LoanBankAccount.route) },
                viewModel = viewModel,
            )
        }

        composable(Screen.LoanBankAccount.route) { backStackEntry ->
            val viewModel = backStackEntry.requestLoanViewModel(navController)
            LoanBankAccountScreen(
                onBackClick = { navController.popBackStack() },
                // Balik ke Home (bukan cuma popBackStack ke LoanAmount) - baik sukses maupun
                // ditahan buat review, pengajuannya udah "selesai" dari sisi nasabah, gak ada
                // alasan balik ke form nominal lagi.
                onDone = { navController.popBackStack(Screen.RequestLoanGraph.route, inclusive = true) },
                viewModel = viewModel,
            )
        }
    }
}

/** Sama pola dengan [registerViewModel] - LoanAmountScreen dan LoanBankAccountScreen berbagi
 * satu instance supaya nominal/tenor yang dipilih di langkah 1 masih ada pas submit di langkah 2. */
@Composable
private fun NavBackStackEntry.requestLoanViewModel(navController: NavHostController): RequestLoanViewModel {
    val parentEntry = remember(this) { navController.getBackStackEntry(Screen.RequestLoanGraph.route) }
    return hiltViewModel(parentEntry)
}

private fun NavGraphBuilder.transjakartaGraph(navController: NavHostController) {
    navigation(startDestination = Screen.TransjakartaHome.route, route = Screen.TransjakartaGraph.route) {
        composable(Screen.TransjakartaHome.route) { backStackEntry ->
            val viewModel = backStackEntry.transjakartaPurchaseViewModel(navController)
            TransjakartaHomeScreen(
                onBackClick = { navController.popBackStack() },
                onBuyClick = { navController.navigate(Screen.TransjakartaConfirm.route) },
                viewModel = viewModel,
            )
        }

        composable(Screen.TransjakartaConfirm.route) { backStackEntry ->
            val viewModel = backStackEntry.transjakartaPurchaseViewModel(navController)
            TransjakartaConfirmScreen(
                onBackClick = { navController.popBackStack() },
                // Balik ke Home (bukan halaman tiket terpisah - QR ditampilin inline di riwayat
                // Home, konfirmasi user) - popBackStack polos cukup karena Home masih di
                // backstack (start destination graph ini, cuma di-push sekali).
                onPurchased = {
                    viewModel.consumePurchasedTickets()
                    navController.popBackStack()
                },
                viewModel = viewModel,
            )
        }
    }
}

/** Sama pola dengan [requestLoanViewModel] - TransjakartaHomeScreen dan TransjakartaConfirmScreen
 * berbagi satu instance supaya qty yang dipilih di Home masih ada pas submit di Confirm. */
@Composable
private fun NavBackStackEntry.transjakartaPurchaseViewModel(navController: NavHostController): TransjakartaPurchaseViewModel {
    val parentEntry = remember(this) { navController.getBackStackEntry(Screen.TransjakartaGraph.route) }
    return hiltViewModel(parentEntry)
}
