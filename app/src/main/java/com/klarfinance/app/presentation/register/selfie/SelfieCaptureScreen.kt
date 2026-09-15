package com.klarfinance.app.presentation.register.selfie

import androidx.camera.core.CameraSelector
import androidx.compose.runtime.Composable
import com.klarfinance.app.presentation.register.RegisterViewModel
import com.klarfinance.app.presentation.register.camera.CaptureFrameShape
import com.klarfinance.app.presentation.register.camera.CameraCaptureScreen

@Composable
fun SelfieCaptureScreen(
    viewModel: RegisterViewModel,
    onCaptured: () -> Unit,
    onBackClick: () -> Unit,
) {
    CameraCaptureScreen(
        title = "Take a Selfie",
        subtitle = "Position your face within the oval and hold still.",
        lensFacing = CameraSelector.LENS_FACING_FRONT,
        frameShape = CaptureFrameShape.FACE_OVAL,
        onCaptured = { uri ->
            viewModel.onKycCaptured(uri)
            onCaptured()
        },
        onBackClick = onBackClick,
    )
}
