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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.cerevia.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(navController: NavController) {
    val bgColor = Color(0xFFF9F9FC)
    val cardBg = Color.White
    var selectedMethod by remember { mutableStateOf("BCA Virtual Account") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Pembayaran", color = Primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = bgColor)
            )
        },
        bottomBar = {
            Surface(color = bgColor, shadowElevation = 0.dp) {
                Box(modifier = Modifier.padding(24.dp)) {
                    Button(
                        onClick = { 
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = false }
                            } 
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("Saya Sudah Bayar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
            // Amount Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, Color(0xFFF0F0F0))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Total Pembayaran", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Rp 150.000", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Selesaikan pembayaran dalam 14:59", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE53935), fontWeight = FontWeight.Medium)
                    }
                }
            }
            
            // Payment Methods
            item {
                Text("Pilih Metode Pembayaran", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
                
                val methods = listOf("BCA Virtual Account", "Mandiri Virtual Account", "BNI Virtual Account", "GoPay", "OVO")
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, Color(0xFFF0F0F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        methods.forEach { method ->
                            val isSelected = selectedMethod == method
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) Color(0xFFE8F5E9) else Color.Transparent)
                                    .clickable { selectedMethod = method }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = if (isSelected) Primary else Color.Gray)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(method, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) Primary else Color.DarkGray, modifier = Modifier.weight(1f))
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = Primary)
                                }
                            }
                        }
                    }
                }
            }
            
            // VA Number Card
            item {
                if (selectedMethod.contains("Virtual Account")) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("Nomor Virtual Account", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("8910 1234 5678 9012", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Black, letterSpacing = 2.sp)
                                IconButton(onClick = { /* Copy */ }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Primary)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Petunjuk Pembayaran:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("1. Buka aplikasi m-banking atau ATM\n2. Pilih menu Transfer > Virtual Account\n3. Masukkan nomor VA di atas\n4. Konfirmasi pembayaran sejumlah Rp 150.000", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, lineHeight = 20.sp)
                        }
                    }
                }
            }
        }
    }
}
