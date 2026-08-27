from fastapi import FastAPI, HTTPException, UploadFile, File
from pydantic import BaseModel
from typing import List
import pandas as pd
import pickle
import joblib
import xgboost
import os
import cv2
import mediapipe as mp
import numpy as np
import math
import json

# ── TensorFlow / Keras ───────────────────────────────────────────────────────
import tensorflow as tf
from tensorflow import keras

# ── Helper path ──────────────────────────────────────────────────────────────
BASE_DIR         = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# ── Load New Facial Modules ──────────────────────────────────────────────────
import sys
sys.path.append(os.path.join(BASE_DIR, "src"))

try:
    from feature_extraction import create_face_mesh_model, extract_face_features_checked
    from scoring import calculate_asymmetry_and_droop_scores
    face_mesh_model = create_face_mesh_model(static_mode=True)
except Exception as e:
    print(f"Error loading new facial modules from src: {e}")
    face_mesh_model = None

app = FastAPI(title="Cerevia ML Backend")
MODEL_TAHAP_1_DIR  = os.path.join(BASE_DIR, "Model", "Tahap 1")
MODEL_TAHAP_2_DIR  = os.path.join(BASE_DIR, "Model", "Tahap 2")  # model_ppg_datanew.keras
MODEL_TAHAP_3_DIR  = os.path.join(BASE_DIR, "Model", "Tahap 3")

# ── Tahap-2 configuration (must match training) ─────────────────────────────
# Smartwatch (Garmin via BLE) provides PPI/RR intervals directly in ms.
# No raw PPG signal processing is needed.
WINDOW_SIZE = 30   # jumlah RR-diff per window (sama dengan EKG meta)
N_KELAS     = 2    # 0 = NSR, 1 = AFib

# Try to load EKG meta for consistent config
_ekg_meta_path = os.path.join(MODEL_TAHAP_2_DIR, "ekg_datanew_meta.json")
if os.path.exists(_ekg_meta_path):
    with open(_ekg_meta_path) as _f:
        _meta = json.load(_f)
    WINDOW_SIZE = _meta.get("window_size", WINDOW_SIZE)
    N_KELAS     = _meta.get("n_kelas", N_KELAS)

# ── Custom Keras layer (required to load PPG model) ──────────────────────────
class ClassToken(keras.layers.Layer):
    def build(self, input_shape):
        self.cls = self.add_weight(
            name='cls_token',
            shape=(1, 1, input_shape[-1]),
            initializer='random_normal',
            trainable=True
        )

    def call(self, x):
        b = tf.shape(x)[0]
        return tf.concat([tf.broadcast_to(self.cls, [b, 1, x.shape[-1]]), x], axis=1)

    def get_config(self):
        return super().get_config()

# ── Load columns ─────────────────────────────────────────────────────────────
try:
    with open(os.path.join(MODEL_TAHAP_1_DIR, "kolom_training.pkl"), "rb") as f:
        training_cols = pickle.load(f)
except Exception as e:
    print(f"Error loading training columns: {e}")
    training_cols = []

# ── Load model tahap 1 ───────────────────────────────────────────────────────
try:
    model_tahap_1 = joblib.load(os.path.join(MODEL_TAHAP_1_DIR, "model_stroke_risk.pkl"))
except Exception as e:
    print(f"Error loading stroke model: {e}")
    model_tahap_1 = None

# ── Load model tahap 2 (PPG — AF Detection) ──────────────────────────────────
_ppg_model_path = os.path.join(MODEL_TAHAP_2_DIR, "model_ppg_datanew.keras")
try:
    model_tahap_2 = tf.keras.models.load_model(
        _ppg_model_path,
        custom_objects={"ClassToken": ClassToken}
    )
    print(f"Model PPG (Tahap 2) berhasil dimuat dari: {_ppg_model_path}")
except Exception as e:
    print(f"Error loading PPG model: {e}")
    model_tahap_2 = None

# ── Load model tahap 3 (Facial Asymmetry) ────────────────────────────────────
try:
    with open(os.path.join(MODEL_TAHAP_3_DIR, "model_stroke_facial_asymmetry.pkl"), "rb") as f:
        tahap_3_dict = pickle.load(f)
        model_tahap_3    = tahap_3_dict.get('model')
        tahap_3_features = tahap_3_dict.get('feature_cols', [])
except Exception as e:
    print(f"Error loading facial asymmetry model: {e}")
    model_tahap_3    = None
    tahap_3_features = []


# ════════════════════════════════════════════════════════════════════════════
#  PPI → RR-diff WINDOWS  (Tahap 2 — Smartwatch input)
# ════════════════════════════════════════════════════════════════════════════

def ppi_ms_to_windows(ppi_ms_list: list, window_size: int = WINDOW_SIZE):
    """
    Mengubah list PPI intervals (ms) dari smartwatch menjadi array
    window RR-diff siap prediksi.

    Smartwatch (Garmin via BLE) sudah menyediakan PPI/RR interval secara
    langsung — tidak perlu deteksi puncak dari sinyal raw PPG.

    Pipeline:
        PPI (ms) → RR (s) → RR-diff (s) → windowing (30 sampel)

    Filter yang sama dengan saat training:
        - Buang window dengan NaN
        - Buang window dengan |RR-diff| > 1.0 s

    Returns:
        windows  : np.ndarray shape (N, window_size, 1) — input model
        n_ppi    : jumlah PPI interval yang diterima
    """
    ppi_arr = np.array(ppi_ms_list, dtype=np.float64)
    n_ppi   = len(ppi_arr)

    min_required = window_size + 2   # butuh minimal window_size+2 PPI untuk window_size RR-diff
    if n_ppi < min_required:
        raise ValueError(
            f"Terlalu sedikit PPI interval ({n_ppi}). "
            f"Minimal {min_required} interval diperlukan (merekam lebih lama)."
        )

    # Konversi ms → detik (sama dengan satuan saat training)
    rr_s    = ppi_arr / 1000.0      # (s)
    rr_diff = np.diff(rr_s)         # (s)

    windows = []
    for i in range(window_size, len(rr_diff)):
        window = rr_diff[i - window_size: i]
        if np.any(np.isnan(window)):
            continue
        if np.any(np.abs(window) > 1.0):
            continue
        windows.append(window.astype(np.float32))

    if not windows:
        raise ValueError(
            "Tidak ada window RR-diff valid. "
            "Pastikan sinyal PPG bersih dan rekaman cukup panjang."
        )

    arr = np.array(windows, dtype=np.float32).reshape(-1, window_size, 1)
    return arr, n_ppi


def aggregate_predictions(proba_matrix: np.ndarray):
    """
    Agregasi prediksi per-window menjadi satu keputusan.

    Strategi: rata-rata probabilitas AFib per window; jika >= 0.5 → AFib.
    Ini konservatif (sensitif terhadap AF) — sesuai tujuan klinis screening.
    """
    mean_proba = float(np.mean(proba_matrix, axis=0)[1])   # indeks 1 = AFib
    prediction = 1 if mean_proba >= 0.5 else 0
    label      = "AFib" if prediction == 1 else "NSR"
    return prediction, mean_proba, label


# ════════════════════════════════════════════════════════════════════════════
#  PYDANTIC MODELS
# ════════════════════════════════════════════════════════════════════════════

class StrokePredictionRequest(BaseModel):
    age: float
    hypertension: int        # 1 for Yes, 0 for No
    heart_disease: int       # 1 for Yes, 0 for No
    avg_glucose_level: float
    bmi: float
    gender: str              # e.g. "Male", "Female", "Other"
    ever_married: str        # e.g. "Yes", "No"
    work_type: str           # e.g. "Private", "Self-employed", "Govt_job", "children", "Never_worked"
    residence_type: str      # e.g. "Urban", "Rural"
    smoking_status: str      # e.g. "formerly smoked", "never smoked", "smokes", "Unknown"


class PpiAfibRequest(BaseModel):
    """
    Input Tahap 2: PPI intervals dari smartwatch (Garmin via BLE).

    ppi_intervals_ms : List PPI/RR intervals dalam **milliseconds**
                       langsung dari HR Measurement GATT characteristic.
                       Contoh: [850.0, 862.0, 834.0, ...]
    """
    ppi_intervals_ms: List[float]


# ════════════════════════════════════════════════════════════════════════════
#  ENDPOINTS
# ════════════════════════════════════════════════════════════════════════════

@app.get("/")
def read_root():
    return {"message": "Cerevia ML API is running"}


# ── Tahap 1: Stroke Risk ─────────────────────────────────────────────────────
@app.post("/predict/stroke-risk")
def predict_stroke_risk(request: StrokePredictionRequest):
    if not model_tahap_1 or not training_cols:
        raise HTTPException(status_code=500, detail="Model is not loaded properly on the server.")

    # Inisialisasi semua kolom dengan 0
    input_data = {col: 0 for col in training_cols}

    # Numerik / Biner
    input_data['age']               = request.age
    input_data['hypertension']      = request.hypertension
    input_data['heart_disease']     = request.heart_disease
    input_data['avg_glucose_level'] = request.avg_glucose_level
    input_data['bmi']               = request.bmi

    # Categorical One-Hot Encoding
    if f'gender_{request.gender}' in input_data:
        input_data[f'gender_{request.gender}'] = 1

    if f'ever_married_{request.ever_married}' in input_data:
        input_data[f'ever_married_{request.ever_married}'] = 1

    if f'work_type_{request.work_type}' in input_data:
        input_data[f'work_type_{request.work_type}'] = 1

    if f'Residence_type_{request.residence_type}' in input_data:
        input_data[f'Residence_type_{request.residence_type}'] = 1

    if f'smoking_status_{request.smoking_status}' in input_data:
        input_data[f'smoking_status_{request.smoking_status}'] = 1

    # Ubah menjadi DataFrame dengan urutan kolom yang sesuai saat training
    df = pd.DataFrame([input_data])[training_cols]

    # Prediksi
    try:
        prediction_val = model_tahap_1.predict(df)[0]
        pred_str = str(prediction_val)

        if pred_str == "Low Risk":
            pred_int = 0
        elif pred_str == "Caution":
            pred_int = 1
        elif pred_str == "High Risk":
            pred_int = 2
        else:
            pred_int = 1

        # Random Forest return proba
        proba = model_tahap_1.predict_proba(df)[0]
        # classes_ are ['Caution', 'High Risk', 'Low Risk']
        # 'High Risk' is at index 1
        risk_probability = float(proba[1])  # Probabilitas kelas High Risk

        return {
            "prediction": pred_int,
            "risk_probability": risk_probability,
            "message": "Prediction successful"
        }
    except Exception as e:
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))


# ── Tahap 2: PPI — Atrial Fibrillation Detection (from Smartwatch) ──────────
@app.post("/predict/ppg-afib")
def predict_ppg_afib(request: PpiAfibRequest):
    """
    Deteksi Atrial Fibrillation (AFib) dari PPI intervals smartwatch.

    Data PPI (Peak-to-Peak Interval / RR Interval) dikirim langsung dari
    aplikasi Android setelah merekam data dari smartwatch Garmin via BLE.
    **Tidak perlu sinyal PPG raw** — smartwatch sudah menyediakan interval.

    **Input JSON:**
    ```json
    { "ppi_intervals_ms": [850.0, 862.0, 834.0, 901.0, ...] }
    ```

    **Output:**
    - `prediction`        : 0 = NSR (Normal Sinus Rhythm), 1 = AFib
    - `label`             : "NSR" atau "AFib"
    - `afib_probability`  : probabilitas AFib rata-rata dari semua window (0–1)
    - `n_ppi`             : jumlah PPI interval yang diterima
    - `n_windows`         : jumlah window yang dianalisis
    - `message`           : pesan status
    """
    if model_tahap_2 is None:
        raise HTTPException(status_code=500, detail="Model AFib (Tahap 2) belum dimuat di server.")

    if not request.ppi_intervals_ms:
        raise HTTPException(status_code=400, detail="ppi_intervals_ms tidak boleh kosong.")

    # ── Preprocessing: PPI (ms) → RR-diff windows ───────────────────────────
    try:
        windows, n_ppi = ppi_ms_to_windows(request.ppi_intervals_ms, window_size=WINDOW_SIZE)
    except ValueError as e:
        raise HTTPException(status_code=422, detail=str(e))
    except Exception as e:
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Error preprocessing PPI: {e}")

    # ── Inferensi model ──────────────────────────────────────────────────────
    try:
        proba_matrix = model_tahap_2.predict(windows, verbose=0)   # shape (N, 2)
    except Exception as e:
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Error prediksi model AFib: {e}")

    # ── Agregasi hasil ───────────────────────────────────────────────────────
    prediction, afib_probability, label = aggregate_predictions(proba_matrix)

    return {
        "prediction":       prediction,
        "label":            label,
        "afib_probability": round(afib_probability, 6),
        "n_ppi":            n_ppi,
        "n_windows":        int(len(windows)),
        "message":          "AFib prediction from smartwatch PPI successful"
    }


# ── Tahap 3: Facial Asymmetry ────────────────────────────────────────────────
@app.post("/predict/facial-asymmetry")
async def predict_facial_asymmetry(image: UploadFile = File(...)):
    if not model_tahap_3 or not tahap_3_features:
        raise HTTPException(status_code=500, detail="Tahap 3 model is not loaded properly on the server.")

    if not face_mesh_model:
        raise HTTPException(status_code=500, detail="Face mesh landmarker model not loaded properly.")

    try:
        image_bytes = await image.read()
        nparr = np.frombuffer(image_bytes, np.uint8)
        image_bgr = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        if image_bgr is None:
            raise Exception("Invalid image data")
            
        # Gunakan modul dari src untuk mengekstraksi fitur dan mengecek pose
        features, pose, is_frontal = extract_face_features_checked(image_bgr, face_mesh_model)
        if features is None:
            raise Exception("No face detected in the image")
            
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

    # Convert to DataFrame in exact column order
    df = pd.DataFrame([features])[tahap_3_features]

    try:
        # Prediksi AI (XGBoost)
        prediction = model_tahap_3.predict(df)[0]
        proba = model_tahap_3.predict_proba(df)[0]
        raw_probability = float(proba[1])

        min_expected = 0.85
        max_expected = 0.97
        scaled_prob = (raw_probability - min_expected) / (max_expected - min_expected)
        xgboost_probability = max(0.01, min(0.99, scaled_prob))

        # Skor Heuristik Baru (dari src/scoring.py)
        asymmetry_score_pct, droop_score_pct = calculate_asymmetry_and_droop_scores(features)
        
        # Bagi 100 agar menjadi probabilitas 0.0 - 1.0 (sesuai ekspektasi Android CereviaApi)
        math_probability = max(asymmetry_score_pct, droop_score_pct) / 100.0

        # Ambil skor tertinggi antara AI dan perhitungan matematis
        asymmetry_probability = max(xgboost_probability, math_probability)

        return {
            "prediction":             int(prediction),
            "asymmetry_probability":  asymmetry_probability,
            "raw_probability":        raw_probability,
            "features_extracted":     features,
            "pose":                   pose,
            "is_frontal":             is_frontal,
            "message":                "Facial asymmetry prediction successful"
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
