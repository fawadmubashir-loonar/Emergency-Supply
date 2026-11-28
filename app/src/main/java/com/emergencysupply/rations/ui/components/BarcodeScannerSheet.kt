package com.emergencysupply.rations.ui.components

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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.emergencysupply.rations.ui.theme.BorderSlate
import com.emergencysupply.rations.ui.theme.CardSurface
import com.emergencysupply.rations.ui.theme.CobaltSoft
import com.emergencysupply.rations.ui.theme.EmeraldVibrant
import com.emergencysupply.rations.ui.theme.InkBlack
import com.emergencysupply.rations.ui.theme.LightSlate
import com.emergencysupply.rations.ui.theme.MediumSlate
import com.emergencysupply.rations.ui.theme.MutedSlate
import com.emergencysupply.rations.ui.theme.SoftTileSurface
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

/**
 * Budgetly-styled ModalBottomSheet providing real-time CameraX + ML Kit UPC/barcode scanning
 * with camera permission handling, reticle targeting, and manual UPC lookup fallback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerSheet(
    onBarcodeDetected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var handled by remember { mutableStateOf(false) }
    var manualBarcode by remember { mutableStateOf("") }

    // Cleanly unbind ProcessCameraProvider when leaving composition
    DisposableEffect(lifecycleOwner, cameraProvider) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title, Icon & High-Contrast InkBlack Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldVibrant.copy(alpha = 0.12f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = EmeraldVibrant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Barcode Scanner",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkBlack
                        )
                        Text(
                            text = "Scan UPC or type barcode manually",
                            fontSize = 12.sp,
                            color = MediumSlate
                        )
                    }
                }

                // InkBlack high-contrast dismiss button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(34.dp)
                        .background(InkBlack, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Viewfinder or Camera Permission Card
            if (hasCameraPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    // CameraX PreviewView View Interop
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                try {
                                    val provider = cameraProviderFuture.get()
                                    cameraProvider = provider

                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }

                                    val barcodeScanner = BarcodeScanning.getClient()
                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()

                                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                        @androidx.camera.core.ExperimentalGetImage
                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null && !handled) {
                                            val inputImage = InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees
                                            )
                                            barcodeScanner.process(inputImage)
                                                .addOnSuccessListener { barcodes ->
                                                    if (!handled) {
                                                        for (barcode in barcodes) {
                                                            val raw = barcode.rawValue ?: barcode.displayValue
                                                            if (!raw.isNullOrBlank()) {
                                                                handled = true
                                                                onBarcodeDetected(raw.trim())
                                                                break
                                                            }
                                                        }
                                                    }
                                                }
                                                .addOnFailureListener {
                                                    // Continuous frame analysis error ignored
                                                }
                                                .addOnCompleteListener {
                                                    imageProxy.close()
                                                }
                                        } else {
                                            imageProxy.close()
                                        }
                                    }

                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageAnalysis
                                    )
                                } catch (_: Exception) {
                                    // Camera bind failed or dismissed
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // EmeraldVibrant Scan Reticle Border
                    Box(
                        modifier = Modifier
                            .size(width = 230.dp, height = 140.dp)
                            .border(
                                border = BorderStroke(2.5.dp, EmeraldVibrant),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .background(
                                color = EmeraldVibrant.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(16.dp)
                            )
                    )

                    // Target indicator pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = InkBlack.copy(alpha = 0.75f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(EmeraldVibrant, CircleShape)
                            )
                            Text(
                                text = "Align barcode within reticle",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                // Clean Permission Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftTileSurface),
                    border = BorderStroke(1.dp, BorderSlate)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = CobaltSoft,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = InkBlack,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Camera Permission Required",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = InkBlack
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Camera access is needed to scan item barcodes instantly into your pantry or groceries.",
                            fontSize = 12.sp,
                            color = MediumSlate,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = InkBlack,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Enable Camera",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Manual Barcode Input & Lookup Button (right below camera viewfinder)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = manualBarcode,
                    onValueChange = { input ->
                        manualBarcode = input.filter { it.isLetterOrDigit() || it == '-' }
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "Enter UPC manually",
                            color = MutedSlate,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MediumSlate,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (manualBarcode.isNotEmpty()) {
                            IconButton(onClick = { manualBarcode = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = LightSlate,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldVibrant,
                        unfocusedBorderColor = BorderSlate,
                        focusedContainerColor = SoftTileSurface.copy(alpha = 0.5f),
                        unfocusedContainerColor = SoftTileSurface.copy(alpha = 0.5f),
                        focusedTextColor = InkBlack,
                        unfocusedTextColor = InkBlack,
                        cursorColor = EmeraldVibrant
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            val trimmed = manualBarcode.trim()
                            if (trimmed.isNotEmpty() && !handled) {
                                handled = true
                                onBarcodeDetected(trimmed)
                            }
                        }
                    )
                )

                Button(
                    onClick = {
                        val trimmed = manualBarcode.trim()
                        if (trimmed.isNotEmpty() && !handled) {
                            handled = true
                            onBarcodeDetected(trimmed)
                        }
                    },
                    enabled = manualBarcode.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InkBlack,
                        contentColor = Color.White,
                        disabledContainerColor = SoftTileSurface,
                        disabledContentColor = MutedSlate
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "Lookup",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

