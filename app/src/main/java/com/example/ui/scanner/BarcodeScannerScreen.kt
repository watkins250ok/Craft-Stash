package com.example.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.dao.SupplyWithBatches
import com.example.domain.model.QuantityMode
import com.example.ui.CraftStashViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(
    viewModel: CraftStashViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSupplyDetail: (String) -> Unit,
    onNavigateToNewSupplyWithBarcode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var scannedBarcode by remember { mutableStateOf<String?>(null) }
    var matchingSupply by remember { mutableStateOf<SupplyWithBatches?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }
    var showManualInputDialog by remember { mutableStateOf(false) }

    val analyzer = remember {
        BarcodeAnalyzer { barcode ->
            coroutineScope.launch {
                scannedBarcode = barcode
                val found = viewModel.findSupplyByBarcode(barcode)
                matchingSupply = found
                showResultDialog = true
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("Scan Barcode") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showManualInputDialog = true },
                        modifier = Modifier.testTag("manual_barcode_button")
                    ) {
                        Text("Type Barcode")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        val cameraExecutor = Executors.newSingleThreadExecutor()

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(cameraExecutor, analyzer)
                                }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    ctx as androidx.lifecycle.LifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Viewfinder Reticle Overlay
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 280.dp, height = 180.dp)
                            .border(2.dp, Color.White, RoundedCornerShape(16.dp))
                            .background(Color.Transparent)
                    )

                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 48.dp)
                    ) {
                        Text(
                            text = "Align UPC / EAN barcode inside frame",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Camera Access Needed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Camera permission is used strictly on-device to decode craft product barcodes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Permission")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showManualInputDialog = true }) {
                        Text("Enter Barcode Manually Instead")
                    }
                }
            }
        }
    }

    // Scanned Result Dialog
    if (showResultDialog && scannedBarcode != null) {
        val code = scannedBarcode!!
        val supply = matchingSupply

        AlertDialog(
            onDismissRequest = {
                showResultDialog = false
                analyzer.resumeScanning()
            },
            title = {
                Text(
                    text = if (supply != null) "Found in Your Stash!" else "Unregistered Barcode",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Barcode: $code", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

                    if (supply != null) {
                        Text(
                            "\"${supply.supply.name}\" already exists in your collection with ${supply.batches.size} batch(es).",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "What would you like to do?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        Text(
                            "This barcode was not found in your local collection. You can create a new supply record with this barcode preserved.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                if (supply != null) {
                    Button(
                        onClick = {
                            showResultDialog = false
                            onNavigateToSupplyDetail(supply.supply.id)
                        }
                    ) {
                        Text("View Item")
                    }
                } else {
                    Button(
                        onClick = {
                            showResultDialog = false
                            onNavigateToNewSupplyWithBarcode(code)
                        }
                    ) {
                        Text("Create New Supply")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showResultDialog = false
                        analyzer.resumeScanning()
                    }
                ) {
                    Text("Scan Another")
                }
            }
        )
    }

    // Manual input dialog
    if (showManualInputDialog) {
        var inputCode by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showManualInputDialog = false },
            title = { Text("Enter Barcode Number", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Leading zeros will be preserved exactly.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { inputCode = it },
                        label = { Text("Barcode Number") },
                        placeholder = { Text("012345678905") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("manual_barcode_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showManualInputDialog = false
                        coroutineScope.launch {
                            val code = inputCode.trim()
                            val found = viewModel.findSupplyByBarcode(code)
                            scannedBarcode = code
                            matchingSupply = found
                            showResultDialog = true
                        }
                    },
                    enabled = inputCode.isNotBlank()
                ) {
                    Text("Search Stash")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualInputDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
