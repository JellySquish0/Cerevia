package com.example.cerevia.ui.screens.directory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.cerevia.theme.CereviaTheme
import com.example.cerevia.theme.Primary
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat

import com.example.cerevia.ui.screens.directory.mockDoctors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingConfirmationScreen(navController: NavController) {
    val doctor = mockDoctors.first()
    val bgColor = Color(0xFFF9F9FC)
    val cardBg = Color.White
    var catatan by remember { mutableStateOf("") }
    
    // Generate next 30 days
    val dates = remember {
        (0..30).map {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, it)
            cal.time
        }
    }
    
    var selectedDate by remember { mutableStateOf(dates.first()) }
    var selectedTime by remember { mutableStateOf("13:00") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Konfirmasi Booking", color = Primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = bgColor)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { },
                containerColor = Color(0xFFC62828),
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Text("*", color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.displaySmall)
            }
        },
        bottomBar = {
            Surface(color = bgColor, shadowElevation = 0.dp) {
                Box(modifier = Modifier.padding(24.dp)) {
                    Button(
                        onClick = { navController.navigate(com.example.cerevia.ui.navigation.Routes.PAYMENT) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("Konfirmasi Booking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        },
        containerColor = bgColor
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Calendar Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, Color(0xFFF0F0F0))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pilih Tanggal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFF0F0F0)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = Color.Gray)
                                }
                                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFF0F0F0)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
                        Text(monthFormat.format(selectedDate), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.DarkGray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(dates) { date ->
                                val isSelected = date == selectedDate
                                val dayFormat = SimpleDateFormat("EEE", Locale("id", "ID"))
                                val numFormat = SimpleDateFormat("dd", Locale("id", "ID"))
                                
                                val itemBgColor = if (isSelected) Primary else Color.Transparent
                                val itemTextColor = if (isSelected) Color.White else Color.DarkGray
                                
                                Column(
                                    modifier = Modifier
                                        .width(56.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(itemBgColor)
                                        .border(1.dp, if(isSelected) Primary else Color(0xFFF0F0F0), RoundedCornerShape(24.dp))
                                        .clickable { selectedDate = date }
                                        .padding(vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(dayFormat.format(date), color = itemTextColor.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(numFormat.format(date), color = itemTextColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            
            // Time Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, Color(0xFFF0F0F0))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Waktu Tersedia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val cal = Calendar.getInstance().apply { time = selectedDate }
                        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                        
                        val times = remember(selectedDate) {
                            val startHour = 8 + (dayOfWeek % 3)
                            val list = mutableListOf<Pair<String, Int>>()
                            for (i in 0 until 8) {
                                val hour = startHour + i
                                val timeStr = String.format("%02d:00", hour)
                                val state = if (hour == 12) 2 else if (hour % 2 == 0) 0 else 1 // 2 = unavailable, 0/1 = available
                                list.add(timeStr to state)
                            }
                            list
                        }
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(times) { (time, state) ->
                                val isSelected = time == selectedTime
                                val itemBgColor = when {
                                    state == 2 -> Color(0xFFF5F5F5)
                                    isSelected -> Color(0xFFE8F5E9)
                                    else -> Color.White
                                }
                                val itemTextColor = when {
                                    state == 2 -> Color(0xFFBDBDBD)
                                    isSelected -> Primary
                                    else -> Color.DarkGray
                                }
                                val itemBorderColor = when {
                                    state == 2 -> Color(0xFFF5F5F5)
                                    isSelected -> Primary
                                    else -> Color(0xFFE0E0E0)
                                }
                                Box(
                                    modifier = Modifier
                                        .width(80.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(itemBgColor)
                                        .border(1.dp, itemBorderColor, RoundedCornerShape(20.dp))
                                        .clickable(enabled = state != 2) { selectedTime = time }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(time, color = itemTextColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            
            // Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFEFEF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Ringkasan Janji Temu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary)
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = doctor.imageRes,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(64.dp).clip(CircleShape).border(2.dp, Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(doctor.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text(doctor.specialty, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Details
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Tanggal", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    val fullDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
                                    Text(fullDateFormat.format(selectedDate), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Waktu", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text("$selectedTime WIB", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Lokasi", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text("RS Medika Utama, Lt. 3 Ruang 302", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text("Catatan untuk Dokter (Opsional)", style = MaterialTheme.typography.labelMedium, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = catatan,
                            onValueChange = { catatan = it },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            placeholder = { Text("Tuliskan keluhan utama Anda di sini...", color = Color.Gray, style = MaterialTheme.typography.bodyMedium) },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White,
                                focusedContainerColor = Color.White,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Primary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun BookingConfirmationPreview() {
    CereviaTheme {
        BookingConfirmationScreen(rememberNavController())
    }
}
