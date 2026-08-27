package com.example.cerevia.ui.screens.education

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import com.example.cerevia.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BEFastDetailScreen(navController: NavController) {
    val bgColor = Color(0xFFF9F9FC)
    val redColor = Color(0xFFD32F2F)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cerevia", color = Primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        containerColor = bgColor
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "Mengenal Gejala\nStroke dengan\nBE-FAST",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Stroke adalah keadaan darurat medis. Ketahui tanda-tandanya dan bertindak cepat untuk menyelamatkan nyawa.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Cards
            item { BeFastCard("Balance", "(Keseimbangan)", "Apakah ada masalah dengan keseimbangan? Apakah penderita tiba-tiba pusing, kehilangan keseimbangan, atau kesulitan berjalan?", Primary, Icons.Default.Accessibility) }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            item { BeFastCard("Eyes", "(Mata)", "Apakah ada masalah penglihatan? Apakah penglihatan tiba-tiba kabur, ganda, atau hilang pada satu atau kedua mata?", Primary, Icons.Default.Visibility) }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            item { BeFastCard("Face", "(Wajah)", "Minta penderita untuk tersenyum. Apakah salah satu sisi wajahnya tampak turun atau tidak simetris?", Primary, Icons.Default.Face) }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            item { BeFastCard("Arm", "(Lengan)", "Minta penderita mengangkat kedua lengannya. Apakah satu lengan merosot ke bawah atau tidak bisa diangkat sama sekali?", Primary, Icons.Default.PanTool) }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            item { BeFastCard("Speech", "(Bicara)", "Minta penderita mengulang kalimat sederhana. Apakah bicaranya terdengar cadel, tidak jelas, atau aneh?", Primary, Icons.Default.RecordVoiceOver) }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            item { 
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = redColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFB71C1C)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Time (Waktu)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Jika Anda melihat salah satu tanda di atas, segera hubungi layanan darurat. Waktu sangat penting!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BeFastCard(title: String, subtitle: String, description: String, color: Color, icon: ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0))
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "$title\n$subtitle",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}
