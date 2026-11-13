package com.example.ui.attendance

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.rememberAsyncImagePainter
import com.example.domain.QrCodeHelper
import com.example.domain.QrValidationResult
import com.example.localization.LocalStrings
import com.example.model.WorkplaceQr
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.PresentGreen
import com.example.ui.theme.TealSecondary
import java.io.File
import java.util.concurrent.Executors

private enum class AttendanceStep { QR_SCAN, SELFIE_CAPTURE, CONFIRM }

@Composable
fun QrSelfieAttendanceDialog(
    isPunchOut: Boolean,
    assignedWorkplaceId: String,
    registeredWorkplaces: List<WorkplaceQr>,
    onDismiss: () -> Unit,
    onCompleted: (WorkplaceQr, String) -> Unit,
    onManualCorrectionFallback: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    var step by remember { mutableStateOf(AttendanceStep.QR_SCAN) }
    var validatedWorkplace by remember { mutableStateOf<WorkplaceQr?>(null) }
    var selfieUri by remember { mutableStateOf<String?>(null) }
    var qrErrorMsg by remember { mutableStateOf<String?>(null) }

    val qrPhotoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val decoded = decodeQrFromUri(context, uri)
            if (decoded == null) {
                qrErrorMsg = if (strings.isHindi) "फोटो में मान्य QR कोड नहीं मिला।" else "No valid QR code found in image."
            } else {
                handleQrDecoded(
                    raw = decoded,
                    workplaces = registeredWorkplaces,
                    assignedId = assignedWorkplaceId,
                    isHindi = strings.isHindi,
                    onValid = { wp ->
                        qrErrorMsg = null
                        validatedWorkplace = wp
                        step = AttendanceStep.SELFIE_CAPTURE
                    },
                    onError = { qrErrorMsg = it }
                )
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isPunchOut) strings.punchOut else strings.punchIn,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Text(
                            text = strings.qrAttendanceTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (!hasCameraPermission) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(strings.cameraPermissionRequired, style = MaterialTheme.typography.bodyMedium)
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                modifier = Modifier.testTag("grant_camera_permission_button")
                            ) {
                                Text(strings.grantCameraPermission)
                            }
                        }
                    }
                }

                when (step) {
                    AttendanceStep.QR_SCAN -> {
                        Text(
                            text = if (strings.isHindi) "चरण 1/2: ${strings.scanCompanyQrHint}" else "Step 1/2: ${strings.scanCompanyQrHint}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (hasCameraPermission) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(2.dp, TealSecondary, RoundedCornerShape(16.dp))
                            ) {
                                CameraQrScannerView(
                                    onQrScanned = { rawText ->
                                        handleQrDecoded(
                                            raw = rawText,
                                            workplaces = registeredWorkplaces,
                                            assignedId = assignedWorkplaceId,
                                            isHindi = strings.isHindi,
                                            onValid = { wp ->
                                                qrErrorMsg = null
                                                validatedWorkplace = wp
                                                step = AttendanceStep.SELFIE_CAPTURE
                                            },
                                            onError = { qrErrorMsg = it }
                                        )
                                    }
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(Color(0x88000000))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (strings.isHindi) "QR कोड को फ्रेम के अंदर रखें" else "Align workplace QR inside frame",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }

                        if (qrErrorMsg != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = qrErrorMsg!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                qrPhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (strings.isHindi) "गैलरी से QR फोटो चुनें" else "Pick Saved QR Image")
                        }

                        TextButton(
                            onClick = onManualCorrectionFallback,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("qr_manual_correction_button")
                        ) {
                            Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.manualCorrectionFallback)
                        }
                    }

                    AttendanceStep.SELFIE_CAPTURE -> {
                        val wp = validatedWorkplace!!
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PresentGreen)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "QR Verified: ${wp.companyName} (${wp.factoryName})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = if (strings.isHindi) "चरण 2/2: ${strings.takeSelfieHint}" else "Step 2/2: ${strings.takeSelfieHint}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = strings.selfiePrivacyNotice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        if (hasCameraPermission) {
                            CameraSelfieCaptureView(
                                onSelfieCaptured = { uri ->
                                    selfieUri = uri
                                    step = AttendanceStep.CONFIRM
                                },
                                onError = { msg -> qrErrorMsg = msg }
                            )
                        }
                    }

                    AttendanceStep.CONFIRM -> {
                        val wp = validatedWorkplace!!
                        Text(
                            text = if (strings.isHindi) "हाजिरी प्रमाण की पुष्टि करें" else "Confirm Attendance Evidence",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        selfieUri?.let { uri ->
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "Captured Selfie",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Text(
                            text = "${wp.companyName} • ${wp.factoryName} (${wp.industrialArea})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { step = AttendanceStep.SELFIE_CAPTURE },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (strings.isHindi) "दोबारा फोटो" else "Retake")
                            }
                            Button(
                                onClick = { onCompleted(wp, selfieUri.orEmpty()) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("confirm_attendance_submit"),
                                colors = ButtonDefaults.buttonColors(containerColor = PresentGreen)
                            ) {
                                Text(if (isPunchOut) strings.punchOut else strings.punchIn)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun handleQrDecoded(
    raw: String,
    workplaces: List<WorkplaceQr>,
    assignedId: String,
    isHindi: Boolean,
    onValid: (WorkplaceQr) -> Unit,
    onError: (String) -> Unit
) {
    when (val res = QrCodeHelper.validateScannedQr(raw, workplaces, assignedId)) {
        is QrValidationResult.Valid -> onValid(res.workplace)
        is QrValidationResult.Expired -> onError(
            if (isHindi) "${res.workplaceName} का QR कोड समाप्त हो गया है। HR से नया QR जनरेट करें।"
            else "QR code for ${res.workplaceName} has expired."
        )
        QrValidationResult.Inactive -> onError(
            if (isHindi) "यह कार्यस्थल QR कोड निष्क्रिय है।" else "This workplace QR code is inactive."
        )
        QrValidationResult.UnrecognizedOrUnauthorized -> onError(
            if (isHindi) "अमान्य या अनधिकृत कार्यस्थल QR कोड।" else "Unrecognized or unauthorized workplace QR code."
        )
    }
}

private fun decodeQrFromUri(context: Context, uri: Uri): String? {
    return try {
        val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val src = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        QrCodeHelper.decodeFromBitmap(bmp)
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun CameraQrScannerView(onQrScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    var lastDecoded by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(executor) { imageProxy ->
                                try {
                                    val buffer = imageProxy.planes[0].buffer
                                    val data = ByteArray(buffer.remaining())
                                    buffer.get(data)
                                    val decoded = QrCodeHelper.decodeFromYuv(data, imageProxy.width, imageProxy.height)
                                    if (decoded != null && decoded != lastDecoded) {
                                        lastDecoded = decoded
                                        ContextCompat.getMainExecutor(ctx).execute {
                                            onQrScanned(decoded)
                                        }
                                    }
                                } catch (_: Exception) {
                                } finally {
                                    imageProxy.close()
                                }
                            }
                        }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("QrScanner", "CameraX QR bind failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

@Composable
private fun CameraSelfieCaptureView(
    onSelfieCaptured: (String) -> Unit,
    onError: (String) -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, NavyPrimary, RoundedCornerShape(16.dp))
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val selector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                        } catch (e: Exception) {
                            Log.e("SelfieCam", "CameraX selfie bind failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                }
            )
        }

        Button(
            onClick = {
                val outDir = File(context.filesDir, "attendance_selfies").apply { mkdirs() }
                val outFile = File(outDir, "selfie_${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(outFile).build()
                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            onSelfieCaptured(Uri.fromFile(outFile).toString())
                        }

                        override fun onError(exception: ImageCaptureException) {
                            onError(exception.message ?: "Selfie capture failed")
                        }
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("capture_selfie_button"),
            colors = ButtonDefaults.buttonColors(containerColor = TealSecondary)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(strings.captureSelfie)
        }
    }
}
