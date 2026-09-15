package com.klarfinance.app.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.klarfinance.app.core.theme.KlarBackground
import com.klarfinance.app.domain.model.AccountState

@Composable
fun SplashScreen(onTimeout: (AccountState) -> Unit, viewModel: SplashViewModel = hiltViewModel()) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.resolved.collect { accountState -> onTimeout(accountState) }
    }
    LaunchedEffect(Unit) {
        val activity = context as? FragmentActivity
        if (activity != null) {
            viewModel.resolveSession(activity)
        } else {
            // Should never happen - MainActivity is a FragmentActivity - but fail safe to
            // GUEST rather than stranding the splash screen if the cast is ever wrong.
            onTimeout(AccountState.GUEST)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KlarBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "K",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Text(
                text = "KlarFinance",
                modifier = Modifier.padding(top = 20.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}
