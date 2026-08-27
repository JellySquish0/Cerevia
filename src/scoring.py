"""
scoring.py
============================================================
Modul ini menghitung Facial Asymmetry Score dan Facial Droop Score
(keduanya dalam skala 0-100%) dari dictionary fitur mentah hasil
feature_extraction.extract_face_features().

Dipisah dari feature_extraction.py supaya logika "skor untuk manusia"
(yang nilainya bisa disesuaikan/dikalibrasi) terpisah dari logika
"ekstraksi geometri wajah" (yang sifatnya tetap).
============================================================
"""

import numpy as np

# Nilai referensi (batas atas wajar) untuk normalisasi tiap fitur
# ke skala 0-100%. Nilai ini diambil dari observasi rentang fitur
# pada dataset training (lihat statistik di train_model.py).
# Sesuaikan nilai ini jika dataset kamu punya rentang yang berbeda.
NORMALIZATION_REF = {
    "mouth_slope_deg": 15.0,
    "mouth_height_diff": 0.15,
    "eye_symmetry_diff": 0.15,
    "eye_opening_diff": 0.15,
    "nose_deviation": 0.20,
    "jaw_asymmetry": 0.15,
    "left_right_face_ratio_diff": 1.0,
}

ASYMMETRY_WEIGHTS = {
    "mouth_slope_deg": 1.0,
    "mouth_height_diff": 2.0,       
    "eye_symmetry_diff": 3.0,
    "eye_opening_diff": 2.0,       
    "nose_deviation": 0.75,         
    "jaw_asymmetry": 3.0,          
    "left_right_face_ratio_diff": 1.5,  # lebih rentan false-positive akibat pose/yaw
}

def _normalize_0_100(value, max_ref):
    """Mengubah nilai fitur mentah jadi skor 0-100%, dibatasi (clip) di 100%."""
    if max_ref <= 0: return 0.0
    score = (value / max_ref) * 100
    return float(np.clip(score, 0, 100))

def _weighted_mean(named_scores: dict, weights: dict):
    """
    Menghitung rata-rata berbobot dari dict {nama_fitur: skor_0_100}.
    named_scores dan weights harus punya key yang sama.
    """
    total_weight = sum(weights[name] for name in named_scores)
    if total_weight <= 0:
        return 0.0
    weighted_sum = sum(named_scores[name] * weights[name] for name in named_scores)
    return float(weighted_sum / total_weight)

def calculate_asymmetry_and_droop_scores(features: dict):
    """
    Menghitung Facial Asymmetry Score dan Facial Droop Score.

    Facial Asymmetry Score sekarang memakai WEIGHTED mean (lihat
    ASYMMETRY_WEIGHTS) supaya fitur yang lebih relevan secara klinis
    (mouth_height_diff, jaw_asymmetry, eye_opening_diff) punya
    pengaruh lebih besar ke skor akhir, dan tidak "diredam" oleh
    fitur lain yang kebetulan stabil.
    """
    asym_named_scores = {
        "mouth_slope_deg": _normalize_0_100(abs(features["mouth_slope_deg"]), NORMALIZATION_REF["mouth_slope_deg"]),
        "mouth_height_diff": _normalize_0_100(features["mouth_height_diff"], NORMALIZATION_REF["mouth_height_diff"]),
        "eye_symmetry_diff": _normalize_0_100(features["eye_symmetry_diff"], NORMALIZATION_REF["eye_symmetry_diff"]),
        "eye_opening_diff": _normalize_0_100(features["eye_opening_diff"], NORMALIZATION_REF["eye_opening_diff"]),
        "nose_deviation": _normalize_0_100(features["nose_deviation"], NORMALIZATION_REF["nose_deviation"]),
        "jaw_asymmetry": _normalize_0_100(features["jaw_asymmetry"], NORMALIZATION_REF["jaw_asymmetry"]),
        "left_right_face_ratio_diff": _normalize_0_100(
            abs(features["left_right_face_ratio"] - 1.0),
            NORMALIZATION_REF["left_right_face_ratio_diff"],
        ),
    }
    facial_asymmetry_score = _weighted_mean(asym_named_scores, ASYMMETRY_WEIGHTS)

    # Droop score tetap rata-rata biasa (equal weight), karena memang sengaja
    # cuma fokus 2 fitur yang paling relevan -- gak perlu dibobot lagi.
    droop_components = [
        _normalize_0_100(features["mouth_height_diff"], NORMALIZATION_REF["mouth_height_diff"]),
        _normalize_0_100(features["jaw_asymmetry"], NORMALIZATION_REF["jaw_asymmetry"]),
    ]
    facial_droop_score = float(np.mean(droop_components))

    return facial_asymmetry_score, facial_droop_score

