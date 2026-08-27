"""
inference_image.py
============================================================
SCRIPT PENGUJIAN DARI SATU FILE GAMBAR (opsional, alternatif webcam)

Berguna untuk uji cepat / validasi model dengan foto yang sudah ada
di komputer, tanpa perlu menyalakan webcam.

Cara pakai:
    python src/inference_image.py path/ke/gambar.jpg

Contoh:
    python src/inference_image.py dataset_wajah/Stroke/img_0023.jpg
============================================================
"""

import os
import sys
import cv2
import joblib
import numpy as np

sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from feature_extraction import extract_face_features, crop_face_with_bbox, create_face_mesh_model
from feature_extraction import (
    extract_face_features_checked,
    crop_face_with_bbox,
    create_face_mesh_model,
)
from scoring import calculate_asymmetry_and_droop_scores

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODEL_PATH = os.path.join(PROJECT_ROOT, "model", "model_stroke_facial_asymmetry.joblib")


def predict_stroke_from_image(image_path, model_bundle, txt_bbox_path=None):
    image_bgr = cv2.imread(image_path)
    if image_bgr is None:
        raise ValueError(f"Gagal membaca gambar dari path: {image_path}")

    if txt_bbox_path is not None and os.path.exists(txt_bbox_path):
        image_bgr = crop_face_with_bbox(image_bgr, txt_bbox_path)

    with create_face_mesh_model(static_mode=True) as face_mesh_model:
        features, pose, is_frontal = extract_face_features_checked(image_bgr, face_mesh_model)

    if features is None:
        print("Wajah tidak terdeteksi pada gambar. Coba gunakan foto dengan "
              "pencahayaan lebih baik dan wajah menghadap kamera.")
        return None, None

    if not is_frontal:
        print("PERINGATAN: pose wajah kurang frontal (miring/menengok).")
        print(f"  roll_deg  : {pose['roll_deg']:.2f} (batas wajar +-25 derajat)")
        print(f"  yaw_ratio : {pose['yaw_ratio']:.2f} (batas wajar <= 2.0)")
        print("  Skor asimetri di bawah ini BISA jadi tidak akurat karena "
              "pose, bukan karena kondisi wajah sebenarnya.\n")

    model = model_bundle["model"]
    label_encoder = model_bundle["label_encoder"]
    feature_cols = model_bundle["feature_cols"]

    X_input = np.array([[features[col] for col in feature_cols]])
    proba = model.predict_proba(X_input)[0]
    class_names = label_encoder.classes_
    proba_dict = dict(zip(class_names, proba))

    stroke_probability = float(proba_dict.get("Stroke", 0.0)) * 100
    nonstroke_probability = float(proba_dict.get("NonStroke", 0.0)) * 100
    asymmetry_score, droop_score = calculate_asymmetry_and_droop_scores(features)

    result = {
        "Stroke Probability (%)": round(stroke_probability, 2),
        "NonStroke Probability (%)": round(nonstroke_probability, 2),
        "Facial Asymmetry Score (%)": round(asymmetry_score, 2),
        "Facial Droop Score (%)": round(droop_score, 2),
        "Pose Frontal": is_frontal,
    }
    return result, features


def print_prediction_result(result):
    print("=" * 50)
    print("HASIL PREDIKSI DETEKSI STROKE DARI WAJAH")
    print("=" * 50)
    for key, value in result.items():
        if isinstance(value, bool):
            print(f"{key:<32}: {value}")
        else:
            print(f"{key:<32}: {value:.2f}")
    print("=" * 50)
    if not result["Pose Frontal"]:
        print(">> CATATAN: pose wajah kurang frontal, hasil di atas kurang bisa diandalkan.")
    if result["Stroke Probability (%)"] >= 50:
        print(">> Indikasi: kemungkinan STROKE lebih tinggi.")
    else:
        print(">> Indikasi: kemungkinan NON-STROKE lebih tinggi.")
    print("Catatan: ini alat bantu skrining, BUKAN diagnosis medis.")


def main():
    if len(sys.argv) < 2:
        print("Cara pakai: python src/inference_image.py path/ke/gambar.jpg")
        sys.exit(1)

    image_path = sys.argv[1]
    if not os.path.exists(image_path):
        print(f"ERROR: File tidak ditemukan: {image_path}")
        sys.exit(1)

    if not os.path.exists(MODEL_PATH):
        print(f"ERROR: Model tidak ditemukan di {MODEL_PATH}")
        print("Jalankan dulu: python src/train_model.py")
        sys.exit(1)

    model_bundle = joblib.load(MODEL_PATH)

    # Cek apakah ada file .txt bbox dengan nama yang sama (opsional)
    txt_path = os.path.splitext(image_path)[0] + ".txt"

    result, raw_features = predict_stroke_from_image(image_path, model_bundle, txt_bbox_path=txt_path)
    if result is None:
        return

    print_prediction_result(result)
    print("\n(Detail) Nilai mentah fitur asimetri wajah:")
    for k, v in raw_features.items():
        print(f"  {k:<22}: {v:.4f}")


if __name__ == "__main__":
    main()
