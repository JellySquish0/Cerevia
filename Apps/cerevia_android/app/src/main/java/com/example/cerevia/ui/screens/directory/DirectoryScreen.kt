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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.cerevia.R
import com.example.cerevia.theme.Primary
import com.example.cerevia.ui.components.SosFab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryScreen(navController: NavController) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Dokter, 1: Rumah Sakit
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<String?>(null) }
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
                                .background(Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) // fallback avatar
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Cerevia", color = Primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Color.DarkGray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        floatingActionButton = { SosFab() },
        containerColor = bgColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Cari dokter neurologi, rumah saki...", color = Color.Gray, style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                shape = RoundedCornerShape(32.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedBorderColor = Primary
                ),
                singleLine = true
            )

            // Segmented Control Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFFE8E8E8))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(32.dp))
                        .background(if (selectedTab == 0) Color.White else Color.Transparent)
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Dokter", color = if (selectedTab == 0) Primary else Color.DarkGray, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(32.dp))
                        .background(if (selectedTab == 1) Color.White else Color.Transparent)
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Rumah Sakit", color = if (selectedTab == 1) Primary else Color.DarkGray, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }

            // Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                item {
                    FilterChip(
                        text = "Jarak terdekat", 
                        icon = Icons.Default.LocationOn,
                        isSelected = selectedFilter == "Jarak terdekat",
                        onClick = { selectedFilter = if (selectedFilter == "Jarak terdekat") null else "Jarak terdekat" }
                    )
                }
                item {
                    FilterChip(
                        text = "Rating > 4.5", 
                        icon = Icons.Default.Star,
                        isSelected = selectedFilter == "Rating > 4.5",
                        onClick = { selectedFilter = if (selectedFilter == "Rating > 4.5") null else "Rating > 4.5" }
                    )
                }
                item {
                    FilterChip(
                        text = "Biaya", 
                        icon = null,
                        isSelected = selectedFilter == "Biaya",
                        onClick = { selectedFilter = if (selectedFilter == "Biaya") null else "Biaya" }
                    )
                }
            }

            Text(
                text = if (selectedTab == 0) "Dokter Spesialis Saraf terdekat" else "Rumah Sakit terdekat",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            val filteredDoctors = mockDoctors.filter { 
                it.name.contains(searchQuery, ignoreCase = true) || 
                it.specialty.contains(searchQuery, ignoreCase = true) ||
                it.hospital.contains(searchQuery, ignoreCase = true)
            }
            val filteredHospitals = mockHospitals.filter { 
                it.name.contains(searchQuery, ignoreCase = true) || 
                it.address.contains(searchQuery, ignoreCase = true) 
            }

            // List Content
            LazyColumn(
                contentPadding = PaddingValues(bottom = 88.dp, start = 16.dp, end = 16.dp),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedTab == 0) {
                    items(filteredDoctors) { doctor ->
                        DoctorCard(navController = navController, doctor = doctor) {
                            navController.navigate("directory/doctor/${doctor.id}")
                        }
                    }
                } else {
                    items(filteredHospitals) { hospital ->
                        HospitalCard(hospital = hospital) {
                            navController.navigate("directory/hospital/${hospital.id}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChip(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) Primary else Color(0xFFF0F5F2)
    val contentColor = if (isSelected) Color.White else Color.DarkGray
    val iconColor = if (isSelected) Color.White else Color.Gray

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, if (isSelected) Primary else Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelSmall, color = contentColor)
    }
}

@Composable
fun DoctorCard(navController: NavController, doctor: Doctor, onClick: () -> Unit) {
    val isAvailableToday = doctor.id % 2 != 0 // mock logic for "Tersedia Hari Ini" vs "Besok"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("directory/doctor/${doctor.id}") },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = doctor.imageRes,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(doctor.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.weight(1f))
                        Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, tint = Color.Gray)
                    }
                    Text(doctor.specialty, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(doctor.rating.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Text("  •  ", color = Color.LightGray)
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("2.5km", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFF0F0F0))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isAvailableToday) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tersedia Hari Ini", color = Primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF5F5F5))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Besok, 09:00", color = Color.DarkGray, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
                
                Button(
                    onClick = { onClick() }, // Go to detail for now, or booking screen
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text("Buat Janji", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HospitalCard(hospital: Hospital, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = hospital.imageRes,
                    contentDescription = "Hospital Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(hospital.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(hospital.address, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${hospital.rating}", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                        Text("  •  ", color = Color.LightGray)
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(hospital.distance, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}

// Data Models
data class Doctor(val id: Int, val name: String, val specialty: String, val rating: Double, val reviews: Int, val hospital: String, val about: String, val imageRes: Int)
data class Hospital(val id: Int, val name: String, val address: String, val hasStrokeUnit: Boolean, val distance: String, val rating: Double, val description: String, val imageRes: Int)

// Mock Data
val mockDoctors = listOf(
    Doctor(1, "Dr. Andi Pratama, Sp.N", "Spesialis Neurologi", 4.9, 124, "RS Medika Utama", "Dr. Andi Pratama adalah dokter spesialis saraf dengan pengalaman lebih dari 10 tahun dalam menangani kasus stroke dan gangguan saraf pusat.", R.drawable.doc_male),
    Doctor(2, "Dr. Siti Aminah, Sp.N", "Spesialis Neurologi (Stroke)", 4.8, 89, "Klinik Saraf Sehat", "Dr. Siti Aminah berfokus pada rehabilitasi pasca stroke dan penanganan darurat stroke. Beliau sangat peduli pada pemulihan pasien.", R.drawable.doc_female),
    Doctor(3, "Dr. Ahmad Faisal, Sp.JP", "Spesialis Jantung & Pembuluh Darah", 4.7, 210, "RS Harapan Bunda", "Dr. Ahmad Faisal adalah spesialis jantung dan pembuluh darah dengan jam terbang tinggi. Sering menangani pasien stroke iskemik dengan komplikasi jantung.", R.drawable.doc_male)
)

val mockHospitals = listOf(
    Hospital(1, "RS Pusat Otak Nasional", "Cawang, Jakarta Timur", true, "4.2 km", 4.9, "Pusat rujukan nasional untuk penyakit otak dan saraf, dilengkapi dengan unit penanganan stroke darurat tingkat lanjut.", R.drawable.hosp),
    Hospital(2, "RS Medika Utama", "Jl. Jendral Sudirman No. 45", true, "2.1 km", 4.7, "Rumah sakit umum modern dengan fasilitas IGD siaga 24 jam dan unit perawatan intensif yang komprehensif.", R.drawable.hosp),
    Hospital(3, "Klinik Saraf Sehat", "Jl. Melati No. 12", false, "1.5 km", 4.5, "Klinik spesialis saraf yang melayani rawat jalan dan rehabilitasi pasca stroke untuk pasien.", R.drawable.hosp)
)
