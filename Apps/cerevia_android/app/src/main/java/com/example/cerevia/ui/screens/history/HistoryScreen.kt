package com.example.cerevia.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.cerevia.data.repository.AnalysisRepository
import com.example.cerevia.domain.model.AnalysisResult
import com.example.cerevia.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: AnalysisRepository
) : ViewModel() {
    val history: StateFlow<List<AnalysisResult>> = repository
        .getAllAnalysis()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController, viewModel: HistoryViewModel = hiltViewModel()) {
    val history by viewModel.history.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Riwayat Pemeriksaan, 1: Log Smartwatch
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
        containerColor = bgColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Latest Score Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(Primary)
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Skor Terakhir", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (history.isNotEmpty()) "${(history.first().combinedScore * 100).toInt()}%" else "-",
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val riskLabel = if (history.isNotEmpty()) history.first().riskLevel.label else "Belum ada"
                        val riskColor = if (history.isNotEmpty()) Color(history.first().riskLevel.color) else Color.Gray

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFA5D6A7)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(riskLabel, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Tab Selector
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0xFFEEEEEE))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(28.dp))
                            .background(if (selectedTab == 0) Color.White else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Riwayat\nPemeriksaan",
                            color = if (selectedTab == 0) Primary else Color.Gray,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(28.dp))
                            .background(if (selectedTab == 1) Color.White else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Analisis Tren",
                            color = if (selectedTab == 1) Primary else Color.Gray,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            if (selectedTab == 0) {
                if (history.isEmpty()) {
                    item { EmptyHistoryState() }
                } else {
                    itemsIndexed(history) { index, result ->
                        TimelineItem(
                            result = result,
                            isFirst = index == 0,
                            isLast = index == history.size - 1,
                            onClick = { navController.navigate("result/${result.id}") }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(88.dp)) } // Padding for bottom nav
                }
            } else {
                item {
                    TrendAnalysisContent(history = history)
                }
            }
        }
    }
}

@Composable
fun TimelineItem(result: AnalysisResult, isFirst: Boolean, isLast: Boolean, onClick: () -> Unit) {
    val riskColor = Color(result.riskLevel.color)
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
    val dateText = if (isFirst) "Hari ini, ${SimpleDateFormat("HH:mm", Locale("id", "ID")).format(Date(result.timestamp))}" else sdf.format(Date(result.timestamp))

    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Timeline Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isFirst) Primary else Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.BarChart,
                    contentDescription = null,
                    tint = if (isFirst) Color.White else Color.DarkGray,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .weight(1f)
                        .background(Color(0xFFE0E0E0))
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))

        // Content Card
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 24.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(text = dateText, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                    
                    // Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(riskColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(result.riskLevel.label, color = riskColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row {
                        Text("Skor Risiko: ", style = MaterialTheme.typography.bodyLarge, color = Color.DarkGray)
                        Text("${(result.combinedScore * 100).toInt()}%", style = MaterialTheme.typography.bodyLarge, color = riskColor, fontWeight = FontWeight.Bold)
                    }
                    
                    // Checkmarks based on completedStages
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (i in 1..3) {
                            if (i <= result.completedStages) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                            } else {
                                Icon(Icons.Outlined.RadioButtonUnchecked, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            tint = Primary.copy(alpha = 0.5f),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Belum Ada Riwayat",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = OnSurface
        )
    }
}

@Composable
fun TrendAnalysisContent(history: List<AnalysisResult>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Analisis Tren Risiko", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Pantau perkembangan risiko stroke Anda dari waktu ke waktu untuk mengambil tindakan preventif yang tepat.",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        // Chips: Mingguan, Bulanan, Tahunan
        var selectedChip by remember { mutableStateOf(1) } // 0: Mingguan, 1: Bulanan, 2: Tahunan
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Mingguan", "Bulanan", "Tahunan").forEachIndexed { index, title ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (selectedChip == index) Primary else Color.Transparent)
                        .border(1.dp, if (selectedChip == index) Primary else Color.LightGray, RoundedCornerShape(24.dp))
                        .clickable { selectedChip = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(title, color = if (selectedChip == index) Color.White else Color.DarkGray, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Chart Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Skor Risiko (6 Bulan Terakhir)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = Color(0xFF212121))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Primary))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Skor\nAnda", fontSize = 10.sp, color = Color.Gray, lineHeight = 12.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Chart area
                Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    val reversedHistory = history.take(6).reversed()
                    val points = if (reversedHistory.isNotEmpty()) reversedHistory.map { it.combinedScore } else listOf(0.1f, 0.2f, 0.25f, 0.4f, 0.3f, 0.15f)
                    val sdf = SimpleDateFormat("MMM", Locale("id", "ID"))
                    val months = if (reversedHistory.isNotEmpty()) reversedHistory.map { sdf.format(Date(it.timestamp)) } else listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun")
                    
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 20.dp)) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        
                        // Draw horizontal lines
                        val steps = 3
                        for (i in 0..steps) {
                            val y = canvasHeight * i / steps
                            drawLine(
                                color = Color(0xFFEEEEEE),
                                start = androidx.compose.ui.geometry.Offset(0f, y),
                                end = androidx.compose.ui.geometry.Offset(canvasWidth, y),
                                strokeWidth = 2f
                            )
                        }
                        
                        // Draw points
                        val xStep = canvasWidth / (points.size - 1).coerceAtLeast(1)
                        points.forEachIndexed { index, value ->
                            val x = index * xStep
                            // Inverse Y (0 risk is at bottom, 1.0 is at top)
                            // max scale is 0.5f for this mockup chart
                            val maxScale = 0.5f
                            val y = canvasHeight - ((value / maxScale) * canvasHeight)
                            
                            drawCircle(
                                color = Color(0xFF212121),
                                radius = 12f,
                                center = androidx.compose.ui.geometry.Offset(x, y)
                            )
                        }
                    }
                    
                    // Draw labels at the bottom
                    Row(
                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).offset(y = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        months.forEach { month ->
                            Text(month, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            border = BorderStroke(1.dp, Color(0xFFC8E6C9))
        ) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Primary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Ringkasan Analisis", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    val summaryText = if (history.size > 1) {
                        val latest = history.first().combinedScore
                        val previous = history[1].combinedScore
                        if (latest > previous) {
                            "Risiko Anda mengalami peningkatan pada pemeriksaan terakhir. Disarankan untuk segera melakukan langkah preventif."
                        } else if (latest < previous) {
                            "Risiko Anda mengalami penurunan! Pertahankan gaya hidup sehat Anda."
                        } else {
                            "Risiko Anda stabil."
                        }
                    } else {
                        "Belum ada cukup data untuk memberikan ringkasan spesifik. Lakukan pemeriksaan rutin untuk memantau tren risiko Anda."
                    }
                    Text(
                        summaryText,
                        color = Color(0xFF424242),
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Export button
        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(28.dp)
        ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ekspor Laporan PDF", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        
        Spacer(modifier = Modifier.height(88.dp)) // Navigation bar padding
    }
}
