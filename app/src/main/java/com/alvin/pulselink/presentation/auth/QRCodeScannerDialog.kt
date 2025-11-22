package com.alvin.pulselink.presentation.auth

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * 二维码扫描对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRCodeScannerDialog(
    onDismiss: () -> Unit,
    onQRCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }
    
    // 从相册选择照片
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val image = InputImage.fromFilePath(context, it)
                val scanner = BarcodeScanning.getClient(
                    BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
                )
                
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            barcodes.firstOrNull()?.rawValue?.let { qrCode ->
                                onQRCodeScanned(qrCode)
                                onDismiss()
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("未在图片中检测到二维码")
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("QRScanner", "Error scanning image", e)
                        scope.launch {
                            snackbarHostState.showSnackbar("扫描图片失败")
                        }
                    }
            } catch (e: Exception) {
                Log.e("QRScanner", "Error loading image", e)
                scope.launch {
                    snackbarHostState.showSnackbar("加载图片失败")
                }
            }
        }
    }
    
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("扫描二维码", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "关闭")
                        }
                    },
                    actions = {
                        // 从相册选择按钮
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") }
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = "从相册选择",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                color = MaterialTheme.colorScheme.surface
            ) {
                if (hasCameraPermission) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CameraPreview(
                            onQRCodeScanned = { qrCode ->
                                onQRCodeScanned(qrCode)
                                onDismiss()
                            }
                        )
                        
                        // 扫描框提示 UI
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 64.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "将二维码放入框内\n或点击右上角从相册选择",
                                    color = androidx.compose.ui.graphics.Color.White,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .background(
                                            color = androidx.compose.ui.graphics.Color.Black.copy(
                                                alpha = 0.5f
                                            ),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                                8.dp
                                            )
                                        )
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                } else {
                    PermissionDeniedContent {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionDeniedContent(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("需要相机权限才能扫描二维码")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRequestPermission) {
                Text("授予权限")
            }
        }
    }
}

@Composable
private fun CameraPreview(
    onQRCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // 1. 优化：指定只扫描 QR_CODE，速度更快
    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }
    
    // 2. 状态管理
    var hasScanned by remember { mutableStateOf(false) }

    // 3. Executor 放在 remember 中，并在 onDispose 关闭
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                // 4. 优化：设置缩放类型，防止预览拉伸
                scaleType = PreviewView.ScaleType.FILL_CENTER
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            // 5. 异步获取 CameraProvider，避免阻塞主线程
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                // 预览用例
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // 图像分析用例
                val imageAnalysis = ImageAnalysis.Builder()
                    // 6. 优化：Backpressure 策略，只处理最新帧，防止内存溢出
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    // 如果已经扫到了，直接关闭后续帧，不再处理
                    if (hasScanned) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    
                        processImageProxy(barcodeScanner, imageProxy) { result ->
                        if (!hasScanned) { // 双重检查
                            hasScanned = true
                            // 切换回主线程回调 UI
                            previewView.post { 
                                onQRCodeScanned(result) 
                            }
                        }
                    }
                }

                try {
                    // 绑定前先解绑
                    cameraProvider.unbindAll()
                    
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    Log.e("CameraPreview", "Use case binding failed", exc)
                }

            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        // 7. 优化：update 块留空或只处理动态属性，
        // 不要在这里做绑定逻辑，因为这里会重复执行导致闪烁
        update = { } 
    )
}

private fun processImageProxy(
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onSuccess: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )
        
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let { qrCode ->
                        onSuccess(qrCode)
                        // 找到一个就退出循环
                        return@addOnSuccessListener 
                    }
                }
            }
            .addOnFailureListener {
                // 处理失败（可选）
            }
            .addOnCompleteListener {
                // 8. 必须关闭 imageProxy，否则相机卡死
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}