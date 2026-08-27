package com.example.cerevia.ui.screens.bluetooth

import android.Manifest
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.cerevia.bluetooth.BleState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.example.cerevia.theme.Primary

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DeviceScanScreen(
    navController: NavController,
    viewModel: DeviceScanViewModel = hiltViewModel()
) {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    val permissionState = rememberMultiplePermissionsState(permissions = permissions)
    val bleState by viewModel.bleManager.bleState.collectAsState()
    val scannedDevices by viewModel.bleManager.scannedDevices.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Pindai Perangkat", color = Primary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!permissionState.allPermissionsGranted) {
                Text("Aplikasi membutuhkan izin Bluetooth dan Lokasi untuk mencari smartwatch.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                    Text("Berikan Izin")
                }
                return@Column
            }

            when (bleState) {
                is BleState.Scanning -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Mencari perangkat...")
                }
                is BleState.Connecting -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Menghubungkan...")
                }
                is BleState.Connected -> {
                    Text("Terhubung!", color = Primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { 
                        navController.popBackStack() // kembali ke halaman sebelumnya
                    }) {
                        Text("Kembali")
                    }
                    Button(onClick = { viewModel.bleManager.disconnect() }) {
                        Text("Putuskan Koneksi", color = Color.Red)
                    }
                }
                is BleState.Recording -> {
                    Text("Sedang Merekam Data PPI...", color = Primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { 
                        navController.popBackStack() // go back to trends screen
                    }) {
                        Text("Lihat Grafik")
                    }
                    Button(onClick = { viewModel.bleManager.stopRecording() }) {
                        Text("Berhenti Rekam")
                    }
                }
                is BleState.Error -> {
                    Text("Error: ${(bleState as BleState.Error).message}", color = Color.Red)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.bleManager.startScan() }) {
                        Text("Coba Lagi")
                    }
                }
                else -> {
                    Button(onClick = { viewModel.bleManager.startScan() }) {
                        Text("Mulai Pindai")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (scannedDevices.isNotEmpty() && bleState is BleState.Scanning) {
                Text("Perangkat Ditemukan:", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(scannedDevices) { device ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { viewModel.bleManager.connect(device.device) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(device.name, fontWeight = FontWeight.Bold)
                                Text(device.device.address, style = MaterialTheme.typography.bodySmall)
                                Text("RSSI: ${device.rssi} dBm", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
