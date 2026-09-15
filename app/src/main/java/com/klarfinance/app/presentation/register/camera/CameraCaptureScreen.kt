package com.klarfinance.app.presentation.register.camera

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.klarfinance.app.core.theme.KlarTeal
import java.io.File

enum class CaptureFrameShape { ID_CARD, FACE_OVAL }

@Composable
fun CameraCaptureScreen(
    title: String,
    subtitle: String,
    lensFacing: Int,
    frameShape: CaptureFrameShape,
    onCaptured: (Uri) -> Unit,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun capturePhoto() {
        val capture = imageCapture ?: return
        val photoFile = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onCaptured(Uri.fromFile(photoFile))
                }

                override fun onError(exc: ImageCaptureException) {
                    errorMessage = exc.message ?: "Failed to capture photo"
                }
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener(
                        {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val capture = ImageCapture.Builder().build()
                            val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                                imageCapture = capture
                            } catch (exc: Exception) {
                                errorMessage = exc.message ?: "Unable to start camera"
                            }
                        },
                        ContextCompat.getMainExecutor(ctx),
                    )
                    previewView
                },
            )

            CaptureFrameOverlay(frameShape = frameShape, modifier = Modifier.align(Alignment.Center))
        } else {
            PermissionRationale(onGrantClick = { permissionLauncher.launch(Manifest.permission.CAMERA) })
        }

        Column(modifier = Modifier.fillMaxSize()) {
            CaptureTopBar(onBackClick = onBackClick)

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            )

            errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            CaptureBottomBar(
                onShutterClick = ::capturePhoto,
                shutterEnabled = hasCameraPermission && imageCapture != null,
            )
        }
    }
}

@Composable
private fun CaptureTopBar(onBackClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Text(
            text = "KlarFinance",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.width(48.dp))
    }
}

@Composable
private fun CaptureFrameOverlay(frameShape: CaptureFrameShape, modifier: Modifier = Modifier) {
    val shape = when (frameShape) {
        CaptureFrameShape.ID_CARD -> RoundedCornerShape(16.dp)
        CaptureFrameShape.FACE_OVAL -> RoundedCornerShape(percent = 50)
    }
    val aspectRatio = when (frameShape) {
        CaptureFrameShape.ID_CARD -> 1.586f
        CaptureFrameShape.FACE_OVAL -> 0.78f
    }
    val widthFraction = when (frameShape) {
        CaptureFrameShape.ID_CARD -> 0.85f
        CaptureFrameShape.FACE_OVAL -> 0.62f
    }

    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .aspectRatio(aspectRatio)
            .border(2.dp, KlarTeal, shape),
    )
}

@Composable
private fun PermissionRationale(onGrantClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Camera access is needed to capture your documents.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Grant permission",
                style = MaterialTheme.typography.labelLarge,
                color = KlarTeal,
                modifier = Modifier.clickable(onClick = onGrantClick),
            )
        }
    }
}

@Composable
private fun CaptureBottomBar(
    onShutterClick: () -> Unit,
    shutterEnabled: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(72.dp)
                .clip(CircleShape)
                .background(if (shutterEnabled) KlarTeal else KlarTeal.copy(alpha = 0.4f))
                .clickable(enabled = shutterEnabled, onClick = onShutterClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = "Capture", tint = Color.White)
        }
    }
}
