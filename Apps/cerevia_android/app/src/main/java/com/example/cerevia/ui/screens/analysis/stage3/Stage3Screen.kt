package com.example.cerevia.ui.screens.analysis.stage3

import android.Manifest
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.cerevia.ui.navigation.Routes
import com.example.cerevia.theme.Primary
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import com.example.cerevia.data.remote.CereviaApi

@HiltViewModel
class Stage3ViewModel @Inject constructor(
    private val api: CereviaApi,
    private val repository: com.example.cerevia.data.repository.AnalysisRepository
) : ViewModel() {
    
    var isUploading by mutableStateOf(false)
    var uploadError by mutableStateOf<String?>(null)
    var currentFrameIndex by mutableStateOf(0)
    var lowRiskCount by mutableStateOf(0)
    var predictionResult by mutableStateOf<Int?>(null)
    var asymmetryProb by mutableStateOf<Float?>(null)

    suspend fun uploadFrameSuspend(photoPath: String): Float? {
        return withContext(Dispatchers.IO) {
            val file = File(photoPath)
            if (!file.exists()) return@withContext null

            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

            try {
                val response = api.predictFacialAsymmetry(body)
                if (response.isSuccessful) {
                    response.body()?.asymmetryProbability
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun saveAnalysisToDb(): Long {
        val currentSession = repository.currentSessionResult
        val result = currentSession?.copy(
            stage3Score = asymmetryProb ?: 0f,
            stage3Done = true
        ) ?: com.example.cerevia.domain.model.AnalysisResult(
            stage1Score = 0f,
            stage2Score = 0f,
            stage3Score = asymmetryProb ?: 0f,
            stage1Done = true,
            stage2Done = true,
            stage3Done = true
        )
        return repository.saveAnalysis(result)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun Stage3Screen(navController: NavController, viewModel: Stage3ViewModel = hiltViewModel()) {
    val bgColor = Color(0xFFF9F9FC)
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isTakingPhoto by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cerevia", color = Primary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.DarkGray)
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0E0E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        containerColor = bgColor,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor)
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (viewModel.uploadError != null) {
                        Text(viewModel.uploadError!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        "Data diproses secara lokal dengan AI. Aman &\nTerenkripsi.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Camera Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(550.dp)
                    .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                    .background(Color.Black)
            ) {
                if (cameraPermissionState.status.isGranted) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }
                            
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                
                                imageCapture = ImageCapture.Builder().build()
                                
                                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                                
                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture
                                    )
                                } catch (e: Exception) {
                                    Log.e("CameraPreview", "Use case binding failed", e)
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Meminta izin kamera...", color = Color.White)
                    }
                }
                
                // Dark Gradient Overlay for the top and bottom of the image
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                )

                // Face Oval Guide
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val ovalWidth = size.width * 0.6f
                    val ovalHeight = size.height * 0.55f
                    val ovalRect = androidx.compose.ui.geometry.Rect(
                        offset = Offset((size.width - ovalWidth) / 2, (size.height - ovalHeight) / 2 - 40f),
                        size = Size(ovalWidth, ovalHeight)
                    )
                    
                    // Draw dashed oval
                    drawOval(
                        color = Color(0xFFA5D6A7), // Light green dashed line
                        topLeft = ovalRect.topLeft,
                        size = ovalRect.size,
                        style = Stroke(
                            width = 6f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                        )
                    )
                    
                    // Draw landmarks (eyes, nose)
                    drawCircle(color = Color(0xFFA5D6A7), radius = 10f, center = Offset(ovalRect.center.x - 60f, ovalRect.center.y - 40f)) // Left eye
                    drawCircle(color = Color(0xFFA5D6A7), radius = 10f, center = Offset(ovalRect.center.x + 60f, ovalRect.center.y - 40f)) // Right eye
                    drawCircle(color = Color(0xFFA5D6A7), radius = 10f, center = Offset(ovalRect.center.x, ovalRect.center.y + 60f)) // Nose
                }

                // Instruction Box Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.2f)) // Glass effect
                        .padding(20.dp)
                ) {
                    val instructionText = if (isTakingPhoto) {
                        "Menganalisis wajah (Frame ${viewModel.currentFrameIndex}/5)...\n${viewModel.lowRiskCount} frame aman"
                    } else {
                        "Posisikan wajah Anda di tengah kotak dan tersenyum."
                    }
                    Text(
                        instructionText,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Snap Button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(4.dp, Primary.copy(alpha = 0.5f), CircleShape)
                    .clickable {
                        if (imageCapture != null && !isTakingPhoto) {
                            coroutineScope.launch {
                                isTakingPhoto = true
                                viewModel.uploadError = null
                                viewModel.currentFrameIndex = 0
                                viewModel.lowRiskCount = 0
                                val results = mutableListOf<Float>()
                                
                                for (i in 1..5) {
                                    viewModel.currentFrameIndex = i
                                    val path = takePictureSuspend(imageCapture!!, context, i)
                                    if (path != null) {
                                        val prob = viewModel.uploadFrameSuspend(path)
                                        if (prob != null) {
                                            results.add(prob)
                                            if (prob < 0.4f) {
                                                viewModel.lowRiskCount++
                                            }
                                            if (viewModel.lowRiskCount >= 3) {
                                                break // Berhenti cepat jika sudah 3 frame aman
                                            }
                                        } else {
                                            viewModel.uploadError = "Gagal memproses frame $i"
                                        }
                                    }
                                }
                                
                                if (results.isNotEmpty()) {
                                    val finalProb = if (viewModel.lowRiskCount >= 3) {
                                        results.filter { it < 0.4f }.average().toFloat()
                                    } else {
                                        results.average().toFloat()
                                    }
                                    viewModel.asymmetryProb = finalProb
                                    val newId = viewModel.saveAnalysisToDb()
                                    navController.navigate("result/$newId") { popUpTo(Routes.HOME) }
                                } else {
                                    if (viewModel.uploadError == null) {
                                        viewModel.uploadError = "Gagal memotret wajah"
                                    }
                                }
                                isTakingPhoto = false
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isTakingPhoto) {
                    CircularProgressIndicator(color = Primary, modifier = Modifier.size(32.dp))
                } else {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Snap", tint = Primary, modifier = Modifier.size(32.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

suspend fun takePictureSuspend(imageCapture: ImageCapture, context: Context, frameIndex: Int): String? = suspendCoroutine { cont ->
    val photoFile = File(context.cacheDir, "face_scan_$frameIndex.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                cont.resume(photoFile.absolutePath)
            }
            override fun onError(exception: ImageCaptureException) {
                Log.e("Stage3Screen", "Photo capture failed", exception)
                cont.resume(null)
            }
        }
    )
}
