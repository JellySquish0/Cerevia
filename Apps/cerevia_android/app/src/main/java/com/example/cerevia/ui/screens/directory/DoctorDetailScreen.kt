package com.example.cerevia.ui.screens.directory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.cerevia.theme.*

// Mock data model extensions
data class DoctorReview(val id: Int, val name: String, val time: String, val rating: Int, val content: String, val avatarLetter: String)

val mockDoctorReviews = listOf(
    DoctorReview(1, "Bapak Budi Hartono", "2 hari yang lalu", 5, "Dokter Sarah sangat sabar menjelaskan kondisi jantung saya. Penjelasannya mudah dimengerti untuk orang tua seperti saya. Terima kasih dok.", "B"),
    DoctorReview(2, "Ibu Siti Aminah", "1 minggu yang lalu", 5, "Pemeriksaan sangat teliti. Ruang tunggu di RS Medika Permata juga sangat nyaman untuk menunggu.", "I")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDetailScreen(doctorId: Int, navController: NavController) {
    val doctor = mockDoctors.find { it.id == doctorId } ?: mockDoctors.first()
    
    val bgColor = Color(0xFFF9F9FC)
    val cardColor = Color.White
    val greenPrimary = Primary

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profil Dokter", color = greenPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Favorite", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = bgColor
                )
            )
        },
        bottomBar = {
            BottomActionBar(navController = navController)
        },
        containerColor = bgColor
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item { DoctorHeader(doctor) }
            item { HospitalLocationCard(doctor) }
            item {
                SectionTitle("Tentang Dokter")
                AboutDoctorCard(doctor)
            }
            item {
                SectionHeaderWithAction("Jadwal Praktik", "Lihat Semua")
                ScheduleCard()
            }
            item {
                SectionTitle("Asuransi & Pembayaran")
                InsuranceSection()
            }
            item {
                SectionTitle("Ulasan Pasien")
                ReviewsCard()
            }
        }
    }
}

@Composable
private fun DoctorHeader(doctor: Doctor) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomCenter) {
            AsyncImage(
                model = doctor.imageRes,
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFFE8E8E8), CircleShape)
            )
            Box(
                modifier = Modifier
                    .offset(y = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE8F5E9))
                    .border(1.dp, Primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Tersedia Hari Ini",
                        color = Primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Profil Dokter", // Text from screenshot
            style = MaterialTheme.typography.headlineSmall,
            color = Color.Black,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = doctor.specialty,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "${doctor.rating}",
                color = Color.Black,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                " (120 ulasan)",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )
            Text("  •  ", color = Color.LightGray)
            Icon(Icons.Default.Work, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "12 Tahun",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun HospitalLocationCard(doctor: Doctor) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocalHospital, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("RS Medika Permata", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(
                    "Jl. Kebon Jeruk Raya No. 27,\nJakarta Barat",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("2.4 km", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = Color.Black,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 12.dp)
    )
}

@Composable
private fun SectionHeaderWithAction(title: String, action: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Color.Black, fontWeight = FontWeight.Bold)
        Text(action, style = MaterialTheme.typography.labelMedium, color = Primary, modifier = Modifier.clickable { })
    }
}

@Composable
private fun AboutDoctorCard(doctor: Doctor) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0))
    ) {
        Text(
            text = doctor.about,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray,
            modifier = Modifier.padding(20.dp),
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun ScheduleCard() {
    val dates = remember {
        val list = mutableListOf<java.util.Date>()
        val cal = java.util.Calendar.getInstance()
        for (i in 0 until 30) {
            list.add(cal.time)
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        list
    }
    var selectedDate by remember { mutableStateOf(dates[0]) }
    var selectedTime by remember { mutableStateOf("09:00") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0))
    ) {
        Column(modifier = Modifier.padding(vertical = 20.dp)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(dates) { date ->
                    val isSelected = date == selectedDate
                    val dayFormat = java.text.SimpleDateFormat("EEE", java.util.Locale("id", "ID"))
                    val numFormat = java.text.SimpleDateFormat("dd", java.util.Locale("id", "ID"))
                    
                    Column(
                        modifier = Modifier
                            .width(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Primary else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.Transparent else Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedDate = date }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(dayFormat.format(date), color = if (isSelected) Color.White else Color.Gray, style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(numFormat.format(date), color = if (isSelected) Color.White else Color.Black, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            val cal = java.util.Calendar.getInstance().apply { time = selectedDate }
            val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
            
            val times = remember(selectedDate) {
                val startHour = 8 + (dayOfWeek % 3)
                val list = mutableListOf<Pair<String, Int>>()
                for (i in 0 until 6) {
                    val hour = startHour + i
                    val timeStr = String.format("%02d:00", hour)
                    val state = if (hour == 12) 2 else if (hour % 2 == 0) 0 else 1 // 2 = unavailable, 0/1 = available
                    list.add(timeStr to state)
                }
                list
            }
            
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                times.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { (time, state) ->
                            val isSelected = time == selectedTime
                            val bgColor = when {
                                state == 2 -> Color(0xFFF5F5F5)
                                isSelected -> Primary
                                else -> Color.White
                            }
                            val textColor = when {
                                state == 2 -> Color(0xFFBDBDBD)
                                isSelected -> Color.White
                                else -> Primary
                            }
                            val borderColor = when {
                                state == 2 -> Color(0xFFF5F5F5)
                                isSelected -> Primary
                                else -> Primary
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(bgColor)
                                    .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                                    .clickable(enabled = state != 2) { selectedTime = time }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(time, color = textColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsuranceSection() {
    val insurances = listOf("BPJS Kesehatan", "Allianz", "Prudential", "Mandiri Inhealth")
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        insurances.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { name ->
                    val isBpjs = name.contains("BPJS")
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (isBpjs) Color(0xFFF5F5F5) else Color.White)
                            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(24.dp))
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isBpjs) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(name, color = Color.Black, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(name, color = Color.Black, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            mockDoctorReviews.forEachIndexed { index, review ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (index == 0) Primary else Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(review.avatarLetter, color = if (index == 0) Color.White else Primary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(review.name, style = MaterialTheme.typography.labelLarge, color = Color.Black, fontWeight = FontWeight.Bold)
                                Text(review.time, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Row {
                                repeat(5) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(review.content, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray, lineHeight = 20.sp)
                    }
                }
                if (index < mockDoctorReviews.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFF0F0F0))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Lihat Semua Ulasan (120)",
                color = Primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { },
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BottomActionBar(navController: NavController) {
    Surface(
        color = Color.White,
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { navController.navigate("consultation") },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.dp, Primary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Konsultasi Online", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            
            Button(
                onClick = { navController.navigate("konfirmasi_booking") },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Booking Jadwal", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DoctorDetailPreview() {
    CereviaTheme {
        DoctorDetailScreen(doctorId = 1, navController = rememberNavController())
    }
}
