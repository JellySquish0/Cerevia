package com.example.cerevia.ui.screens.analysis.stage1

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.example.cerevia.ui.navigation.Routes
import com.example.cerevia.theme.Primary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.cerevia.data.remote.CereviaApi
import com.example.cerevia.data.remote.dto.StrokePredictionRequestDto
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewModelScope

import com.example.cerevia.data.repository.AnalysisRepository
import com.example.cerevia.domain.model.AnalysisResult
import com.example.cerevia.domain.model.MedicalRecord

@HiltViewModel
class Stage1ViewModel @Inject constructor(
    private val api: CereviaApi,
    private val repository: AnalysisRepository
) : ViewModel() {
    var usia by mutableStateOf("")
    var jenisKelamin by mutableStateOf("Pria")
    var pekerjaan by mutableStateOf("")
    var statusPernikahan by mutableStateOf("Belum Menikah")
    var tempatTinggal by mutableStateOf("Perkotaan (Urban)")
    var riwayatHipertensi by mutableStateOf(false)
    var gulaDarah by mutableStateOf("")
    var tinggi by mutableStateOf("")
    var berat by mutableStateOf("")
    var merokok by mutableStateOf("Tidak Pernah")
    var riwayatJantung by mutableStateOf(false)

    var isLoading by mutableStateOf(false)
    var predictionResult by mutableStateOf<Int?>(null)

    fun submitData(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val ageFloat = usia.toFloatOrNull() ?: 0f
        val glucoseFloat = gulaDarah.toFloatOrNull() ?: 0f
        val tinggiCm = tinggi.toFloatOrNull() ?: 1f
        val beratKg = berat.toFloatOrNull() ?: 0f
        val bmiValue = if (tinggiCm > 0) beratKg / ((tinggiCm / 100) * (tinggiCm / 100)) else 0f

        val genderMapped = if (jenisKelamin == "Pria") "Male" else "Female"
        val marriedMapped = if (statusPernikahan == "Sudah Menikah") "Yes" else "No"
        val residenceMapped = if (tempatTinggal == "Perkotaan (Urban)") "Urban" else "Rural"
        
        val workMapped = when (pekerjaan) {
            "Wiraswasta" -> "Self-employed"
            "PNS" -> "Govt_job"
            "Karyawan" -> "Private"
            "Pelajar/Anak" -> "children"
            else -> "Never_worked"
        }

        val smokingMapped = when (merokok) {
            "Tidak Pernah" -> "never smoked"
            "Pernah" -> "formerly smoked"
            "Perokok Aktif" -> "smokes"
            else -> "Unknown"
        }

        val request = StrokePredictionRequestDto(
            age = ageFloat,
            hypertension = if (riwayatHipertensi) 1 else 0,
            heartDisease = if (riwayatJantung) 1 else 0,
            avgGlucoseLevel = glucoseFloat,
            bmi = bmiValue,
            gender = genderMapped,
            everMarried = marriedMapped,
            workType = workMapped,
            residenceType = residenceMapped,
            smokingStatus = smokingMapped
        )

        viewModelScope.launch {
            isLoading = true
            try {
                val response = api.predictStrokeRisk(request)
                if (response.isSuccessful && response.body() != null) {
                    val pred = response.body()?.prediction ?: 0
                    predictionResult = pred
                    
                    // Use actual probability from the ML model, fallback to 0f if not available
                    val riskScore = response.body()?.riskProbability ?: 0f
                    
                    val medRec = MedicalRecord(
                        usia = ageFloat.toInt(),
                        jenisKelamin = if (jenisKelamin == "Pria") "Laki-laki" else "Perempuan",
                        kadarGulaDarah = glucoseFloat,
                        tekananDarahSistolik = if (riwayatHipertensi) 140 else 110,
                        tekananDarahDiastolik = 80,
                        riwayatPenyakitJantung = riwayatJantung,
                        kebiasaanMerokok = merokok,
                        bmi = bmiValue,
                        tempatTinggal = if (tempatTinggal == "Perkotaan (Urban)") "Perkotaan" else "Pedesaan",
                        pekerjaan = pekerjaan
                    )

                    repository.currentSessionResult = AnalysisResult(
                        stage1Score = riskScore,
                        stage1Done = true,
                        medicalRecord = medRec,
                        stage2Score = 0.0f,
                        stage3Score = 0.0f
                    )
                    
                    onSuccess()
                } else {
                    onError("Gagal mengirim data: ${response.code()}")
                }
            } catch (e: Exception) {
                onError("Terjadi kesalahan jaringan: ${e.localizedMessage}")
            } finally {
                isLoading = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Stage1Screen(navController: NavController, viewModel: Stage1ViewModel = hiltViewModel()) {
    val bgColor = Color(0xFFF9F9FC)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cerevia", color = Primary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.DarkGray)
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0E0E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        containerColor = bgColor,
        bottomBar = {
            val context = LocalContext.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor)
                    .padding(24.dp)
            ) {
                Button(
                    onClick = { 
                        viewModel.submitData(
                            onSuccess = { navController.navigate(Routes.ANALYSIS_STAGE2) },
                            onError = { errorMsg -> Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show() }
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(28.dp),
                    enabled = !viewModel.isLoading
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Simpan & Lanjut ke Tahap 2", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            // Top Progress Indicator
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tahap 1 dari 3", style = MaterialTheme.typography.labelMedium, color = Primary, fontWeight = FontWeight.Bold)
                Text("Data Dasar", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Primary, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFE0E0E0), RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFE0E0E0), RoundedCornerShape(2.dp)))
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Lengkapi data rekam medis di bawah ini untuk memulai analisis risiko kesehatan Anda secara akurat.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Profil Dasar Card
            SectionCard(
                title = "Profil Dasar",
                icon = { Icon(Icons.Outlined.Person, null, tint = Primary) }
            ) {
                CereviaTextField(label = "Usia (Tahun)", value = viewModel.usia, onValueChange = { viewModel.usia = it }, placeholder = "Misal: 65", isNumber = true)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Jenis Kelamin", style = MaterialTheme.typography.labelMedium, color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().height(48.dp).background(Color(0xFFF0F0F0), RoundedCornerShape(24.dp))
                ) {
                    val isPria = viewModel.jenisKelamin == "Pria"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp)
                            .background(if (isPria) Color.White else Color.Transparent, RoundedCornerShape(20.dp))
                            .shadow(if (isPria) 2.dp else 0.dp, RoundedCornerShape(20.dp))
                            .clickable { viewModel.jenisKelamin = "Pria" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Pria", color = if (isPria) Primary else Color.Gray, fontWeight = if (isPria) FontWeight.Bold else FontWeight.Normal)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp)
                            .background(if (!isPria) Color.White else Color.Transparent, RoundedCornerShape(20.dp))
                            .shadow(if (!isPria) 2.dp else 0.dp, RoundedCornerShape(20.dp))
                            .clickable { viewModel.jenisKelamin = "Wanita" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Wanita", color = if (!isPria) Primary else Color.Gray, fontWeight = if (!isPria) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = viewModel.pekerjaan,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status / Pekerjaan Utama") },
                        placeholder = { Text("Pilih status pekerjaan") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            focusedLabelColor = Primary,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("Wiraswasta", "PNS", "Karyawan", "Pelajar/Anak", "Lainnya").forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    viewModel.pekerjaan = selectionOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Status Pernikahan
                var expandedNikah by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expandedNikah, onExpandedChange = { expandedNikah = it }) {
                    OutlinedTextField(
                        value = viewModel.statusPernikahan,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status Pernikahan") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedNikah) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expandedNikah, onDismissRequest = { expandedNikah = false }) {
                        listOf("Belum Menikah", "Sudah Menikah").forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    viewModel.statusPernikahan = selectionOption
                                    expandedNikah = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Tempat Tinggal
                var expandedTinggal by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expandedTinggal, onExpandedChange = { expandedTinggal = it }) {
                    OutlinedTextField(
                        value = viewModel.tempatTinggal,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Lingkungan Tempat Tinggal") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTinggal) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expandedTinggal, onDismissRequest = { expandedTinggal = false }) {
                        listOf("Perkotaan (Urban)", "Pedesaan (Rural)").forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    viewModel.tempatTinggal = selectionOption
                                    expandedTinggal = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tanda Vital & Fisik
            SectionCard(
                title = "Tanda Vital & Fisik",
                icon = { Icon(Icons.Outlined.FavoriteBorder, null, tint = Primary) }
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Riwayat Hipertensi", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Apakah Anda memiliki riwayat tekanan darah tinggi (hipertensi)?", style = MaterialTheme.typography.bodySmall, color = Color.Gray, lineHeight = 16.sp)
                        }
                        Switch(
                            checked = viewModel.riwayatHipertensi,
                            onCheckedChange = { viewModel.riwayatHipertensi = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Primary)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                CereviaTextField(label = "Gula Darah Sewaktu Puasa (mg/dL)", value = viewModel.gulaDarah, onValueChange = { viewModel.gulaDarah = it }, placeholder = "Misal: 110", isNumber = true)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Pengukuran Fisik", style = MaterialTheme.typography.labelMedium, color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CereviaTextField(label = "", value = viewModel.tinggi, onValueChange = { viewModel.tinggi = it }, placeholder = "Tinggi   cm", isNumber = true, modifier = Modifier.weight(1f))
                    CereviaTextField(label = "", value = viewModel.berat, onValueChange = { viewModel.berat = it }, placeholder = "Berat    kg", isNumber = true, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gaya Hidup & Riwayat
            SectionCard(
                title = "Gaya Hidup & Riwayat",
                icon = { Icon(Icons.Outlined.Coffee, null, tint = Primary) }
            ) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = viewModel.merokok,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kebiasaan Merokok") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            focusedLabelColor = Primary,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("Tidak Pernah", "Pernah", "Perokok Aktif").forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    viewModel.merokok = selectionOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Riwayat Penyakit\nJantung Keluarga", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Apakah ada anggota keluarga inti yang memiliki riwayat penyakit jantung?", style = MaterialTheme.typography.bodySmall, color = Color.Gray, lineHeight = 16.sp)
                        }
                        Switch(
                            checked = viewModel.riwayatJantung,
                            onCheckedChange = { viewModel.riwayatJantung = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Primary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun SectionCard(title: String, icon: @Composable () -> Unit, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary)
            }
            Spacer(modifier = Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
fun CereviaTextField(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String = "", isNumber: Boolean = false, modifier: Modifier = Modifier) {
    if (label.isNotEmpty()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color.Gray) },
        keyboardOptions = KeyboardOptions(keyboardType = if (isNumber) KeyboardType.Number else KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = Color(0xFFE0E0E0),
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
        singleLine = true
    )
}
