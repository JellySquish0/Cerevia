package com.example.cerevia.ui.screens.trends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.cerevia.theme.Primary

import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cerevia.ui.navigation.Routes
import com.example.cerevia.bluetooth.BleState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsScreen(navController: NavController, viewModel: TrendsViewModel = hiltViewModel()) {
    val bgColor = Color(0xFFF9F9FC)
    var selectedFilter by remember { mutableStateOf("Bulanan") }
    val bleState by viewModel.bleState.collectAsState()
    val ppiIntervals by viewModel.ppiIntervals.collectAsState()

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
        containerColor = bgColor,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { /* Export to PDF */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ekspor Laporan PDF", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Analisis Tren Risiko",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Pantau perkembangan risiko stroke Anda dari waktu ke waktu untuk mengambil tindakan preventif yang tepat.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // BLE Connection Button & PPI Display
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
                        Spacer(modifier = Modifier.height(12.dp))
                        if (ppiIntervals.isNotEmpty() && ppiIntervals.last() > 0f) {
                            Text("${ppiIntervals.last().toInt()} ms", color = Primary, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                            Text("PPI Terakhir", color = Primary.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                        } else {
                            Text("-- ms", color = Primary, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                            Text("Menunggu Data PPI...", color = Primary.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                        }
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
                    Text("Hubungkan Smartwatch (BLE)", fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val filters = listOf("Mingguan", "Bulanan", "Tahunan")
                filters.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (isSelected) Primary else Color.White)
                            .border(if (isSelected) 0.dp else 1.dp, if (isSelected) Color.Transparent else Color(0xFFE0E0E0), RoundedCornerShape(24.dp))
                            .clickable { selectedFilter = filter }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Color.White else Color.DarkGray,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Chart Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Text("Skor Risiko (6 Bulan\nTerakhir)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.Black)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Primary))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Skor\nAnda", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray, fontSize = 10.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Simple Scatter Plot Simulation using Canvas
                    Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            
                            // Draw horizontal lines
                            val numLines = 4
                            for (i in 0 until numLines) {
                                val y = i * (h / (numLines - 1))
                                drawLine(
                                    color = Color(0xFFE0E0E0),
                                    start = Offset(0f, y),
                                    end = Offset(w, y),
                                    strokeWidth = 2f,
                                )
                            }
                            
                            // Data points (simulated Jan-Jun)
                            // Values roughly based on screenshot (y=0 is top, y=h is bottom)
                            val points = listOf(
                                Offset(w * 0.05f, h * 0.85f), // Jan
                                Offset(w * 0.23f, h * 0.65f), // Feb
                                Offset(w * 0.41f, h * 0.55f), // Mar
                                Offset(w * 0.59f, h * 0.25f), // Apr (Spike)
                                Offset(w * 0.77f, h * 0.15f), // Mei (Actually May in image has no dot, but screenshot shows a dot)
                                Offset(w * 0.95f, h * 0.35f)  // Jun
                            )
                            // Correcting points to match screenshot better
                            val actPoints = listOf(
                                Offset(w * 0.02f, h * 0.85f), // Jan
                                Offset(w * 0.2f, h * 0.7f),  // Feb
                                Offset(w * 0.4f, h * 0.6f),  // Mar
                                Offset(w * 0.6f, h * 0.3f),  // Apr (red in description)
                                Offset(w * 0.98f, h * 0.4f)   // Jun (cut off on right)
                            )
                            
                            actPoints.forEachIndexed { index, point ->
                                val color = if (index == 3) Color(0xFFD32F2F) else Color.Black // April spike is red
                                drawCircle(
                                    color = color,
                                    radius = 12f,
                                    center = point
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // X-Axis Labels
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val months = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun")
                        months.forEach { month ->
                            Text(month, style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Analysis Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Insights, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Ringkasan Analisis", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Terjadi lonjakan risiko pada bulan April (titik merah) yang bertepatan dengan laporan tekanan darah tinggi Anda. Namun, risiko berhasil diturunkan signifikan di bulan Juni berkat rutinitas olahraga jalan pagi yang konsisten.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF424242),
                            lineHeight = 22.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
