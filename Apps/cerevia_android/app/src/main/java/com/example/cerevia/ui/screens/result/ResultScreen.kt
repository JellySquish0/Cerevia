package com.example.cerevia.ui.screens.result

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.platform.LocalContext
import com.example.cerevia.domain.model.RiskLevel
import com.example.cerevia.ui.components.triggerSosMessage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import com.example.cerevia.theme.Primary
import com.example.cerevia.ui.components.SosFab
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val repository: com.example.cerevia.data.repository.AnalysisRepository
) : ViewModel() {

    private val _analysisResult = kotlinx.coroutines.flow.MutableStateFlow<com.example.cerevia.domain.model.AnalysisResult?>(null)
    val analysisResult: kotlinx.coroutines.flow.StateFlow<com.example.cerevia.domain.model.AnalysisResult?> = _analysisResult.asStateFlow()

    fun loadAnalysis(id: Long) {
        viewModelScope.launch {
            _analysisResult.value = repository.getAnalysisById(id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(analysisId: Long, navController: NavController, viewModel: ResultViewModel = hiltViewModel()) {
    val bgColor = Color(0xFFF9F9FC)
    val coroutineScope = rememberCoroutineScope()
    var isSaved by remember { mutableStateOf(false) }
    var showFacePopup by remember { mutableStateOf(false) }
    var showMedicalPopup by remember { mutableStateOf(false) }

    val analysisResult by viewModel.analysisResult.collectAsState()
    val context = LocalContext.current
    var hasTriggeredAutoSos by remember { mutableStateOf(false) }

    LaunchedEffect(analysisId) {
        viewModel.loadAnalysis(analysisId)
    }

    LaunchedEffect(analysisResult) {
        if (analysisResult != null && !hasTriggeredAutoSos) {
            val riskLevel = analysisResult!!.riskLevel
            if (riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.VERY_HIGH) {
                hasTriggeredAutoSos = true
                triggerSosMessage(context, isWhatsApp = true)
            }
        }
    }

    if (showFacePopup && analysisResult != null) {
        AlertDialog(
            onDismissRequest = { showFacePopup = false },
            title = { Text("Hasil Analisis Wajah", fontWeight = FontWeight.Bold, color = Primary) },
            text = {
                val score = analysisResult?.stage3Score ?: 0f
                val percent = (score * 100).toInt()
                Text("Berdasarkan prediksi Tahap 3, kemungkinan risiko asimetri wajah Anda adalah $percent%.", style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                TextButton(onClick = { showFacePopup = false }) {
                    Text("Tutup", color = Primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showMedicalPopup && analysisResult != null) {
        val result = analysisResult!!
        val statusStr = when {
            (result.stage1Score ?: 0f) <= 0.33f -> "Low Risk"
            (result.stage1Score ?: 0f) <= 0.66f -> "Caution"
            else -> "High Risk"
        }
        val medicalStr = result.medicalRecord?.let {
            "Usia: ${it.usia}\nGula Darah: ${it.kadarGulaDarah}\nTekanan Darah: ${it.tekananDarahSistolik}/${it.tekananDarahDiastolik}\nRiwayat Jantung: ${if (it.riwayatPenyakitJantung) "Ya" else "Tidak"}\nMerokok: ${it.kebiasaanMerokok}"
        } ?: "Data medis belum tersedia."

        AlertDialog(
            onDismissRequest = { showMedicalPopup = false },
            title = { Text("Faktor Risiko Medis ($statusStr)", fontWeight = FontWeight.Bold, color = Primary) },
            text = {
                Column {
                    Text("Berdasarkan rekam medis Anda, status Anda adalah $statusStr. Hal ini dipengaruhi oleh faktor-faktor berikut:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(medicalStr, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(onClick = { showMedicalPopup = false }) {
                    Text("Tutup", color = Primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0E0E0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Cerevia", color = Primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.DarkGray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        floatingActionButton = { SosFab() },
        containerColor = bgColor
    ) { padding ->
        if (analysisResult == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            return@Scaffold
        }
        
        val result = analysisResult!!
        val combinedPercent = (result.combinedScore * 100).toInt()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            Text(
                "Skor Risiko Stroke",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Circular Progress Box
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                CircularProgressIndicator(
                    progress = 1f,
                    color = Color(0xFFEEEEEE),
                    strokeWidth = 14.dp,
                    modifier = Modifier.fillMaxSize()
                )
                CircularProgressIndicator(
                    progress = result.combinedScore,
                    color = Primary,
                    strokeWidth = 14.dp,
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier.fillMaxSize()
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$combinedPercent%", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Primary)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE0F2F1))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(result.riskLevel.label, color = Primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Berdasarkan analisis data kesehatan terbaru Anda, berikut adalah faktor-faktor yang menyumbang tingkat risiko Anda.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Faktor Kontribusi
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Faktor Kontribusi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            val stage1Status = when {
                (result.stage1Score ?: 0f) <= 0.33f -> "Low Risk"
                (result.stage1Score ?: 0f) <= 0.66f -> "Caution"
                else -> "High Risk"
            }
            FactorCard(
                title = "Rekam Medis",
                value = stage1Status,
                unit = "",
                badgeText = "Klik untuk detail",
                badgeIcon = Icons.Outlined.Info,
                icon = { Icon(Icons.Outlined.MedicalServices, null, tint = Primary) },
                onClick = { showMedicalPopup = true }
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            val afibStatus = if (result.stage2Score != null) {
                if (result.stage2Score > 0.5f) "Terdeteksi AFib" else "Non AFib"
            } else {
                "Belum dianalisis"
            }
            FactorCard(
                title = "Deteksi AFib",
                value = afibStatus,
                unit = "",
                badgeText = if (result.stage2Score != null) "Dari Tahap 2" else "Data Kosong",
                badgeIcon = Icons.Outlined.CheckCircle,
                icon = { Icon(Icons.Outlined.MonitorHeart, null, tint = Primary) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            FactorCard(
                title = "Simetri Wajah",
                value = if (result.stage3Score != null) "${(result.stage3Score * 100).toInt()}%" else "-",
                unit = "Risiko",
                badgeText = "Klik untuk detail",
                badgeIcon = Icons.Outlined.Info,
                icon = { Icon(Icons.Outlined.Face, null, tint = Primary) },
                onClick = { showFacePopup = true }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Tindakan yang disarankan
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, null, tint = Primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tindakan yang\nDisarankan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary, lineHeight = 20.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    ActionCard(
                        title = "Konsultasi Rutin",
                        desc = "Jadwalkan pemeriksaan kesehatan umum setiap 6 bulan untuk memantau tekanan darah dan metrik vital lainnya.",
                        icon = Icons.Outlined.Event
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ActionCard(
                        title = "Atur Pola Makan",
                        desc = "Pertahankan asupan rendah garam dan perbanyak sayuran hijau untuk menjaga kesehatan vaskular jangka panjang.",
                        icon = Icons.Outlined.Restaurant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Buttons
            Button(
                onClick = { 
                    navController.navigate(com.example.cerevia.ui.navigation.Routes.HOME) {
                        popUpTo(com.example.cerevia.ui.navigation.Routes.HOME) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Outlined.Home, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kembali ke Beranda", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = { },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray)
            ) {
                Icon(Icons.Outlined.Share, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Bagikan Hasil", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(100.dp)) // padding for FAB
        }
    }
}

@Composable
fun FactorCard(
    title: String,
    value: String,
    unit: String,
    badgeText: String,
    badgeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    icon: @Composable () -> Unit,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = Color.DarkGray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Text(unit, style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(bottom = 3.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(badgeIcon, null, tint = Primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(badgeText, color = Primary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun ActionCard(title: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE0F2F1)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(desc, style = MaterialTheme.typography.bodySmall, color = Color.Gray, lineHeight = 16.sp)
            }
        }
    }
}
