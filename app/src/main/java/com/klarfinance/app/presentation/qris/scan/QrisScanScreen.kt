package com.klarfinance.app.presentation.qris.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.klarfinance.app.core.theme.KlarTeal
import java.util.concurrent.Executors

/**
 * Scan QR toko (merchant code polos, lihat qris-generator/) buat mulai transaksi QRIS. Reuse
 * pola CameraX punya CameraCaptureScreen.kt (KTP/selfie) - bedanya di sini pakai use case
 * ImageAnalysis (bukan ImageCapture) yang nge-feed tiap frame ke ML Kit BarcodeScanning, gak ada
 * tombol shutter - berhenti otomatis begitu 1 barcode ke-decode.
 */
@OptIn(ExperimentalGetImage::class)
@Composable
fun QrisScanScreen(
    onScanned: (String) -> Unit,
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

    // rememberUpdatedState supaya analyzer (dibuat sekali di factory AndroidView) selalu
    // manggil callback TERBARU, bukan yang ke-capture pas komposisi pertama.
    val currentOnScanned by rememberUpdatedState(onScanned)
    var hasScanned by remember { mutableStateOf(false) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { cameraExecutor.shutdown() } }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener(
                        {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                analyzeFrame(imageProxy, hasScanned) { value ->
                                    hasScanned = true
                                    currentOnScanned(value)
                                }
                            }
                            val selector = CameraSelector.DEFAULT_BACK_CAMERA
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                            } catch (_: Exception) {
                                // Kamera gagal bind (jarang) - tetap tampilkan preview kosong,
                                // user bisa retry lewat tombol back lalu buka lagi.
                            }
                        },
                        ContextCompat.getMainExecutor(ctx),
                    )
                    previewView
                },
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.75f)
                    .aspectRatio(1f)
                    .border(2.dp, KlarTeal, RoundedCornerShape(16.dp)),
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column {
                    Text(
                        "Izin kamera diperlukan untuk scan QRIS.",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Berikan izin",
                        color = KlarTeal,
                        modifier = Modifier.clickable { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(4.dp),
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
            }
            Text("Scan QRIS", color = Color.White, style = MaterialTheme.typography.titleMedium)
        }

        if (hasScanned) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = KlarTeal)
            }
        }
    }
}

@ExperimentalGetImage
private fun analyzeFrame(imageProxy: androidx.camera.core.ImageProxy, alreadyScanned: Boolean, onDecoded: (String) -> Unit) {
    val mediaImage = imageProxy.image
    if (alreadyScanned || mediaImage == null) {
        imageProxy.close()
        return
    }
    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    BarcodeScanning.getClient()
        .process(inputImage)
        .addOnSuccessListener { barcodes ->
            val value = barcodes.firstOrNull { it.valueType == Barcode.TYPE_TEXT || it.rawValue != null }?.rawValue
            if (value != null) onDecoded(value)
        }
        .addOnCompleteListener { imageProxy.close() }
}
