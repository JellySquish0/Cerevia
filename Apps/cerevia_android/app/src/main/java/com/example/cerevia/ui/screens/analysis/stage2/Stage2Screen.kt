package com.example.cerevia.ui.screens.analysis.stage2

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.cerevia.ui.navigation.Routes
import com.example.cerevia.theme.Primary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sin
import com.example.cerevia.bluetooth.GarminBleManager
import com.example.cerevia.bluetooth.BleState
import com.example.cerevia.data.remote.CereviaApi
import com.example.cerevia.data.repository.AnalysisRepository
import com.example.cerevia.data.remote.dto.PpiAfibRequestDto

@HiltViewModel
class Stage2ViewModel @Inject constructor(
    val bleManager: GarminBleManager,
    private val api: CereviaApi,
    private val repository: AnalysisRepository
) : ViewModel() {
    var isAfibActive by mutableStateOf(true)
    val bleState = bleManager.bleState
    val ppiIntervals = bleManager.ppiIntervals
    val hrValues = bleManager.hrValues

    var isLoading by mutableStateOf(false)
    var predictionLabel by mutableStateOf<String?>(null)
    var afibProbability by mutableStateOf<Float?>(null)
    var errorMessage by mutableStateOf<String?>(null)
    var hasSubmittedThisSession = false

    init {
        resetAndStart()
    }

    fun predictAfibIfNeeded() {
        val currentPpis = ppiIntervals.value
        if (currentPpis.size >= 32 && !hasSubmittedThisSession && !isLoading) {
            hasSubmittedThisSession = true
            submitPpiData(currentPpis.toList())
        }
    }

    fun resetAndStart() {
        predictionLabel = null
        afibProbability = null
        errorMessage = null
        isLoading = false
        hasSubmittedThisSession = false
        bleManager.startRecording()
    }

    private fun submitPpiData(ppis: List<Float>) {
        isLoading = true
        errorMessage = null
        
        viewModelScope.launch {
            try {
                val request = PpiAfibRequestDto(ppi_intervals_ms = ppis)
                val response = api.predictPpiAfib(request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    predictionLabel = body.label
                    afibProbability = body.afib_probability

                    // Berhenti merekam segera setelah analisis berhasil
                    bleManager.stopRecording()

                    val currentRes = repository.currentSessionResult
                    if (currentRes != null) {
                        repository.currentSessionResult = currentRes.copy(
                            stage2Score = body.afib_probability,
                            stage2Done = true,
                            hrvMetrics = bleManager.calculateHrvMetrics()
                        )
                    }
                } else {
                    bleManager.stopRecording()
                    errorMessage = "Gagal memprediksi AFib. Coba lagi."
                }
            } catch (e: Exception) {
                bleManager.stopRecording()
                errorMessage = "Terjadi kesalahan koneksi."
            } finally {
                isLoading = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Stage2Screen(navController: NavController, viewModel: Stage2ViewModel = hiltViewModel()) {
    val bgColor = Color(0xFFF9F9FC)
    val bleState by viewModel.bleState.collectAsState()
    val hrValues by viewModel.hrValues.collectAsState()
    val ppiIntervals by viewModel.ppiIntervals.collectAsState()

    DisposableEffect(Unit) {
        // ALWAYS force reset when UI enters, in case ViewModel was cached
        viewModel.resetAndStart()
        
        onDispose {
            viewModel.bleManager.stopRecording()
        }
    }

    LaunchedEffect(ppiIntervals.size) {
        // ONLY predict if we actually have exactly 32 or more, AND it's not from a previous session
        // The delay ensures any StateFlow updates (like clearing the list) have time to propagate
        if (ppiIntervals.size >= 32 && viewModel.bleManager.isRecording) {
            kotlinx.coroutines.delay(100) // 100ms debounce to prevent race conditions
            if (viewModel.ppiIntervals.value.size >= 32) {
                viewModel.predictAfibIfNeeded()
            }
        }
    }

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
                Button(
                    onClick = { navController.navigate(Routes.ANALYSIS_STAGE3) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("Lanjut ke Tahap 3", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Progress Indicator
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tahap 2 dari 3", style = MaterialTheme.typography.labelMedium, color = Primary, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Primary, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Primary, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFE0E0E0), RoundedCornerShape(2.dp)))
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "Sinkronisasi Smartwatch",
                style = MaterialTheme.typography.titleMedium,
                color = Primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Pastikan smartwatch Anda terhubung via Bluetooth untuk membaca data detak jantung secara real-time.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // BLE Connection
            if (bleState is BleState.Recording || bleState is BleState.Connected) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, Primary)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Terhubung ke Smartwatch", color = Primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { navController.navigate(Routes.DEVICE_SCAN) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, Primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pilih Perangkat Bluetooth", fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Watch Graphic
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                // Background rings
                Box(modifier = Modifier.size(200.dp).clip(CircleShape).background(Color(0xFFE8F5E9).copy(alpha = 0.5f)))
                Box(modifier = Modifier.size(160.dp).clip(CircleShape).background(Color(0xFFE8F5E9)))
                Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(Color.White))
                
                // Watch Icon / Bluetooth badge
                Icon(
                    Icons.Default.MonitorHeart,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.DarkGray
                )
                
                // BT indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 24.dp, top = 24.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("B", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp) // Simplified BT icon
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Heart Rate Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text("DETAK JANTUNG", style = MaterialTheme.typography.labelMedium, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.Bottom) {
                                val currentBpm = if ((bleState is BleState.Recording || bleState is BleState.Connected) && hrValues.isNotEmpty() && hrValues.last() > 0) {
                                    hrValues.last().toString()
                                } else {
                                    "--"
                                }
                                Text(currentBpm, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("BPM", style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFEBEE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Wave graphic
                    Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                        val path1 = Path()
                        val path2 = Path()
                        val path3 = Path()
                        val width = size.width
                        val height = size.height
                        
                        for (i in 0..100) {
                            val x = i * width / 100
                            val y1 = height / 2 + sin(i * 0.2f) * 15f
                            val y2 = height / 2 + sin(i * 0.15f + 1f) * 10f
                            val y3 = height / 2 + sin(i * 0.25f + 2f) * 5f
                            
                            if (i == 0) {
                                path1.moveTo(x, y1)
                                path2.moveTo(x, y2)
                                path3.moveTo(x, y3)
                            } else {
                                path1.lineTo(x, y1)
                                path2.lineTo(x, y2)
                                path3.lineTo(x, y3)
                            }
                        }
                        
                        drawPath(path1, Primary.copy(alpha = 0.8f), style = Stroke(width = 3f))
                        drawPath(path2, Primary.copy(alpha = 0.4f), style = Stroke(width = 2f))
                        drawPath(path3, Primary.copy(alpha = 0.2f), style = Stroke(width = 2f))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // AFib Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = Primary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Deteksi AFib", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        
                        if (viewModel.isLoading) {
                            Text("Menganalisis ritme...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        } else if (viewModel.errorMessage != null) {
                            Text(viewModel.errorMessage!!, style = MaterialTheme.typography.bodySmall, color = Color(0xFFE53935))
                        } else if (viewModel.predictionLabel != null) {
                            Text(viewModel.predictionLabel!!, style = MaterialTheme.typography.bodySmall, color = if (viewModel.predictionLabel == "AFib") Color(0xFFE53935) else Primary, fontWeight = FontWeight.Bold)
                            if (viewModel.afibProbability != null) {
                                Text("Probabilitas: ${(viewModel.afibProbability!! * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        } else {
                            Text("Mengumpulkan data... (${ppiIntervals.size}/32)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Primary))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Aktif", color = Primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
