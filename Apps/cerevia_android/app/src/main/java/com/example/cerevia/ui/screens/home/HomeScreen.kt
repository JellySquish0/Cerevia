package com.example.cerevia.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.cerevia.data.repository.AnalysisRepository
import com.example.cerevia.ui.components.SosFab
import com.example.cerevia.ui.navigation.Routes
import com.example.cerevia.theme.Primary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import com.example.cerevia.domain.model.AnalysisResult

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: AnalysisRepository,
) : ViewModel() {
    val history: StateFlow<List<AnalysisResult>> = repository
        .getAllAnalysis()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = hiltViewModel()) {
    val history by viewModel.history.collectAsState()
    val latestResult = history.firstOrNull()
    
    val bgColor = Color(0xFFF9F9FC)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0F2F1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Person, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
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
        containerColor = bgColor,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Main Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(32.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Status Risiko Stroke Saat Ini",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Circular Progress representation
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                        CircularProgressIndicator(
                            progress = 1f,
                            color = Color(0xFFEEEEEE),
                            strokeWidth = 12.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                        CircularProgressIndicator(
                            progress = latestResult?.combinedScore ?: 0.0f,
                            color = if (latestResult != null) Color(latestResult.riskLevel.color) else Primary,
                            strokeWidth = 12.dp,
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier.fillMaxSize()
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val scorePercent = latestResult?.let { (it.combinedScore * 100).toInt() } ?: 0
                            val label = latestResult?.riskLevel?.label?.uppercase() ?: "BELUM ADA DATA"
                            val textColor = if (latestResult != null) Color(latestResult.riskLevel.color) else Primary
                            Text("${scorePercent}%", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = textColor)
                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = if (latestResult != null) "Kondisi Anda telah dianalisis. Tetap jaga pola makan dan rutin berolahraga." else "Silakan mulai analisis baru untuk mengetahui status Anda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Start Analysis Button
            Button(
                onClick = { navController.navigate(Routes.ANALYSIS_STAGE1) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(Icons.Default.InsertChart, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mulai Analisis Baru", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Vital Signs Section
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Tanda Vital Terakhir", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Lihat Detail", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Primary, modifier = Modifier.clickable { })
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Vital Cards
            VitalCard(
                icon = { Icon(Icons.Outlined.MonitorHeart, null, tint = Primary) },
                title = "Tekanan Darah",
                value = latestResult?.medicalRecord?.let { "${it.tekananDarahSistolik}/${it.tekananDarahDiastolik}" } ?: "-",
                unit = "mmHg",
                badgeText = if (latestResult?.medicalRecord?.tekananDarahSistolik ?: 0 > 120) "Tinggi" else if (latestResult != null) "Normal" else "-",
                badgeColor = Color(0xFFE0F2F1),
                badgeTextColor = Primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            VitalCard(
                icon = { Icon(Icons.Outlined.FavoriteBorder, null, tint = Primary) },
                title = "Detak Jantung",
                value = latestResult?.hrvMetrics?.meanHr?.toInt()?.toString() ?: "-",
                unit = "bpm",
                badgeText = latestResult?.hrvMetrics?.meanHr?.let { if (it > 100f) "Tinggi" else if (it < 60f) "Rendah" else "Normal" } ?: "-",
                badgeColor = Color(0xFFE0F2F1),
                badgeTextColor = Primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            VitalCard(
                icon = { Icon(Icons.Outlined.Bloodtype, null, tint = Primary) },
                title = "Gula Darah",
                value = latestResult?.medicalRecord?.kadarGulaDarah?.toInt()?.toString() ?: "-",
                unit = "mg/dL",
                badgeText = if (latestResult?.medicalRecord?.kadarGulaDarah ?: 0f > 140f) "Tinggi" else if (latestResult != null) "Normal" else "-",
                badgeColor = Color(0xFFE0F2F1),
                badgeTextColor = Primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Tips Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tips Kesehatan Hari Ini", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Konsumsi buah-buahan beri untuk menjaga kesehatan pembuluh darah otak.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF424242),
                            lineHeight = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Image(
                        painter = rememberAsyncImagePainter("https://images.unsplash.com/photo-1596484552993-9c8e197d1b32?auto=format&fit=crop&w=150&q=80"),
                        contentDescription = "Berries",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(88.dp)) // padding for FAB and bottom nav
        }
    }
}

@Composable
fun VitalCard(
    icon: @Composable () -> Unit,
    title: String,
    value: String,
    unit: String,
    badgeText: String,
    badgeColor: Color,
    badgeTextColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = Color.DarkGray)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(unit, style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(bottom = 3.dp))
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeColor)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(badgeText, color = badgeTextColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}
