package com.example.cerevia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.location.LocationManager
import android.content.Context
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cerevia.theme.*
import androidx.compose.ui.composed

fun Modifier.glassmorphism(): Modifier = composed {
    this.shadow(elevation = 8.dp, shape = RoundedCornerShape(32.dp), ambientColor = Primary, spotColor = Primary)
        .clip(RoundedCornerShape(32.dp))
        .background(SurfaceContainerLowest.copy(alpha = 0.95f))
}

fun triggerSosMessage(context: Context, isWhatsApp: Boolean = true) {
    val phoneNumber = "6282129738928" 
    var message = "🚨 DARURAT 🚨\nTolong, saya membutuhkan bantuan medis segera!"
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: SecurityException) { null }
        
        if (location != null) {
            message += "\n\nLokasi saya saat ini:\nhttps://maps.google.com/?q=${location.latitude},${location.longitude}"
        } else {
            message += "\n\n(Lokasi GPS belum tersedia, silakan lacak nomor ini)"
        }
    } else {
        message += "\n\n(Izin lokasi belum diberikan di aplikasi)"
    }

    try {
        val intent = Intent(Intent.ACTION_VIEW)
        val url = if (isWhatsApp) {
            "https://wa.me/$phoneNumber?text=${java.net.URLEncoder.encode(message, "UTF-8")}"
        } else {
            "https://t.me/+$phoneNumber?text=${java.net.URLEncoder.encode(message, "UTF-8")}"
        }
        intent.data = Uri.parse(url)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal membuka aplikasi chat", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun SosFab(modifier: Modifier = Modifier) {
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { _ -> }
    )

    LaunchedEffect(showDialog) {
        if (showDialog) {
            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!hasFine && !hasCoarse) {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    FloatingActionButton(
        onClick = { showDialog = true },
        containerColor = Tertiary,
        contentColor = OnTertiary,
        shape = CircleShape,
        modifier = modifier.size(72.dp).shadow(12.dp, CircleShape),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Warning, contentDescription = "SOS", modifier = Modifier.size(24.dp))
            Text("SOS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Kontak Darurat (SOS)", fontWeight = FontWeight.Bold, color = Tertiary) },
            text = { 
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Pilih metode untuk menghubungi kontak darurat:")
                    
                    Button(
                        onClick = {
                            showDialog = false
                            triggerSosMessage(context, isWhatsApp = true)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Text("Chat via WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            showDialog = false
                            triggerSosMessage(context, isWhatsApp = false)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC))
                    ) {
                        Text("Chat via Telegram", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CereviaTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Kembali") }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceContainerLowest, titleContentColor = OnSurface),
    )
}

@Composable
fun AnalysisProgressStepper(
    currentStage: Int,
    stage1Done: Boolean,
    stage2Done: Boolean,
    stage3Done: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        StageStep(number = 1, label = "Rekam\nMedis", isCurrent = currentStage == 1, isDone = stage1Done)
        StageConnector(isActive = stage1Done)
        StageStep(number = 2, label = "Heart\nRate", isCurrent = currentStage == 2, isDone = stage2Done)
        StageConnector(isActive = stage2Done)
        StageStep(number = 3, label = "Wajah", isCurrent = currentStage == 3, isDone = stage3Done)
    }
}

@Composable
private fun StageStep(number: Int, label: String, isCurrent: Boolean, isDone: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp).clip(CircleShape).background(
                when { isDone -> Primary; isCurrent -> PrimaryContainer; else -> SurfaceContainerHigh }
            )
        ) {
            Text(
                text = if (isDone) "✓" else "$number",
                color = when { isDone -> OnPrimary; isCurrent -> OnPrimaryContainer; else -> OnSurfaceVariant },
                fontWeight = FontWeight.Bold, fontSize = 16.sp,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = if (isCurrent || isDone) Primary else OnSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun RowScope.StageConnector(isActive: Boolean) {
    HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        color = if (isActive) Primary else OutlineVariant, thickness = 2.dp)
}

@Composable
fun PillButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, color: Color = Primary,
) {
    Button(
        onClick = onClick, enabled = enabled, shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
        modifier = modifier.height(56.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HealthMetricCard(
    icon: ImageVector, label: String, value: String, unit: String = "",
    color: Color = Primary, modifier: Modifier = Modifier,
) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.12f))) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Text(unit, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OnBackground, modifier = modifier)
}

@Composable
fun RealTimeClock(modifier: Modifier = Modifier, color: Color = Primary) {
    var currentTime by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(System.currentTimeMillis()) }
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }
    
    val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    val formattedTime = sdf.format(java.util.Date(currentTime))
    
    Text(
        text = formattedTime,
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier
    )
}
