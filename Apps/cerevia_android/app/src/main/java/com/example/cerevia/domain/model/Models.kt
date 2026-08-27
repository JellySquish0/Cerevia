package com.example.cerevia.domain.model

// ==================== MEDICAL RECORD (TAHAP 1) ====================
data class MedicalRecord(
    val usia: Int,
    val jenisKelamin: String,       // "Laki-laki" / "Perempuan"
    val tekananDarahSistolik: Int,  // mmHg
    val tekananDarahDiastolik: Int, // mmHg
    val kadarGulaDarah: Float,      // mg/dL
    val riwayatPenyakitJantung: Boolean,
    val kebiasaanMerokok: String,   // "Tidak Pernah" / "Pernah" / "Masih"
    val tempatTinggal: String,      // "Perkotaan" / "Pedesaan"
    val bmi: Float,
    val pekerjaan: String,
)

// ==================== HRV / PPI (TAHAP 2) ====================
data class HrvMetrics(
    val ppiIntervals: List<Float>,  // PPI dalam ms
    val sdnn: Float,                // Standard Deviation NN intervals
    val rmssd: Float,               // Root Mean Square Successive Differences
    val pnn50: Float,               // % NN intervals differing >50ms
    val meanPpi: Float,             // Mean PPI in ms
    val meanHr: Float,              // Mean HR from PPI
    val recordingDurationMs: Long,
)

// ==================== FACE ANALYSIS (TAHAP 3) ====================
data class FaceAnalysisData(
    val imagePath: String,
    val capturedAt: Long = System.currentTimeMillis(),
)

// ==================== ANALYSIS RESULT ====================
data class AnalysisResult(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    // Per-stage scores (0.0 – 1.0 probability of stroke risk)
    val stage1Score: Float? = null,   // Medical records
    val stage2Score: Float? = null,   // Heart rate signal
    val stage3Score: Float? = null,   // Face
    // Stage completion flags
    val stage1Done: Boolean = false,
    val stage2Done: Boolean = false,
    val stage3Done: Boolean = false,
    // Input data snapshots
    val medicalRecord: MedicalRecord? = null,
    val hrvMetrics: HrvMetrics? = null,
    val faceImagePath: String? = null,
) {
    /** Weighted average of completed stages — each contributes equally (33.3%) */
    val combinedScore: Float
        get() {
            val scores = listOfNotNull(stage1Score, stage2Score, stage3Score)
            return if (scores.isEmpty()) 0f else scores.average().toFloat()
        }

    val riskLevel: RiskLevel
        get() = when {
            combinedScore < 0.2f -> RiskLevel.VERY_LOW
            combinedScore < 0.4f -> RiskLevel.LOW
            combinedScore < 0.6f -> RiskLevel.MODERATE
            combinedScore < 0.8f -> RiskLevel.HIGH
            else -> RiskLevel.VERY_HIGH
        }

    val completedStages: Int
        get() = listOf(stage1Done, stage2Done, stage3Done).count { it }
}

enum class RiskLevel(val label: String, val color: Long, val description: String) {
    VERY_LOW("Sangat Rendah", 0xFF4CAF82, "Risiko stroke Anda sangat rendah. Pertahankan gaya hidup sehat."),
    LOW("Rendah", 0xFF8BC34A, "Risiko stroke Anda rendah. Tetap jaga kesehatan secara rutin."),
    MODERATE("Sedang", 0xFFFFB300, "Risiko stroke Anda sedang. Konsultasikan dengan dokter."),
    HIGH("Tinggi", 0xFFFF6D00, "Risiko stroke Anda tinggi. Segera periksakan diri ke dokter."),
    VERY_HIGH("Sangat Tinggi", 0xFFE53935, "PERINGATAN: Risiko stroke sangat tinggi! Segera hubungi tenaga medis!"),
}

// ==================== DOCTOR & HOSPITAL ====================
data class Doctor(
    val id: Int,
    val name: String,
    val specialization: String,
    val hospital: String,
    val rating: Float,
    val experience: String,
    val availableToday: Boolean,
    val schedule: String,
    val phone: String,
    val profilePhoto: String? = null,
)

data class Hospital(
    val id: Int,
    val name: String,
    val address: String,
    val distance: String,
    val rating: Float,
    val specialties: List<String>,
    val phone: String,
    val isEmergency: Boolean,
    val openHours: String,
)

// ==================== HEALTH TREND ====================
data class HealthTrendPoint(
    val timestamp: Long,
    val combinedScore: Float,
    val stage1Score: Float?,
    val stage2Score: Float?,
    val stage3Score: Float?,
    val label: String,
)
