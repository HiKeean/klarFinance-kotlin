package com.klarfinance.app.presentation.register.ktpscan

import androidx.camera.core.CameraSelector
import androidx.compose.runtime.Composable
import com.klarfinance.app.presentation.register.RegisterViewModel
import com.klarfinance.app.presentation.register.camera.CaptureFrameShape
import com.klarfinance.app.presentation.register.camera.CameraCaptureScreen

@Composable
fun KtpScanScreen(
    viewModel: RegisterViewModel,
    onCaptured: () -> Unit,
    onBackClick: () -> Unit,
) {
    CameraCaptureScreen(
        title = "Scan your KTP",
        subtitle = "Align your KTP within the frame until it is clear and readable.",
        lensFacing = CameraSelector.LENS_FACING_BACK,
        frameShape = CaptureFrameShape.ID_CARD,
        onCaptured = { uri ->
            viewModel.onKtpCaptured(uri)
            onCaptured()
        },
        onBackClick = onBackClick,
    )
}
