"""
inference_webcam.py
============================================================
"""

import os
import sys
import time
import cv2
import joblib
import numpy as np

sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from feature_extraction import extract_face_features_checked, create_face_mesh_model
from scoring import calculate_asymmetry_and_droop_scores

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODEL_PATH = os.path.join(PROJECT_ROOT, "model", "model_stroke_facial_asymmetry.joblib")

CAMERA_INDEX = 0
PREDICT_EVERY_N_FRAMES = 3  # jalankan prediksi tiap 3 frame (bukan tiap frame) biar gak lag
                             # naikkan angka ini kalau webcam masih terasa berat/patah-patah

def load_model_bundle():
    if not os.path.exists(MODEL_PATH):
        print(f"ERROR: Model tidak ditemukan di {MODEL_PATH}")
        print("Jalankan dulu: python src/train_model.py")
        sys.exit(1)
    return joblib.load(MODEL_PATH)


def predict_from_frame(frame_bgr, model_bundle, face_mesh_model):
    """
    Mengembalikan (result_dict, raw_features, is_frontal) atau
    (None, None, None) jika wajah tidak terdeteksi.
    """
    features, pose, is_frontal = extract_face_features_checked(frame_bgr, face_mesh_model)
    if features is None:
        return None, None, None

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
    }
    return result, features, is_frontal


def print_prediction_result(result):
    print("=" * 50)
    print("HASIL PREDIKSI DETEKSI STROKE DARI WAJAH")
    print("=" * 50)
    for key, value in result.items():
        print(f"{key:<32}: {value:.2f}")
    print("=" * 50)
    if result["Stroke Probability (%)"] >= 50:
        print(">> Indikasi: kemungkinan STROKE lebih tinggi.")
    else:
        print(">> Indikasi: kemungkinan NON-STROKE lebih tinggi.")
    print("Catatan: ini alat bantu skrining, BUKAN diagnosis medis.\n"
          "Segera hubungi tenaga medis profesional untuk evaluasi lebih lanjut.\n")


def draw_overlay_text(frame, lines, origin=(10, 30), line_height=25):
    """Menggambar beberapa baris teks hasil prediksi di atas frame webcam."""
    for i, line in enumerate(lines):
        y = origin[1] + i * line_height
        # outline hitam tipis supaya teks terbaca di latar belakang apapun
        cv2.putText(frame, line, (origin[0], y), cv2.FONT_HERSHEY_SIMPLEX,
                    0.6, (0, 0, 0), 3, cv2.LINE_AA)
        cv2.putText(frame, line, (origin[0], y), cv2.FONT_HERSHEY_SIMPLEX,
                    0.6, (0, 255, 0), 1, cv2.LINE_AA)
    return frame


def main():
    model_bundle = load_model_bundle()
    print("Model berhasil dimuat.")
    print("Kelas yang dikenali model:", list(model_bundle["label_encoder"].classes_))

    cap = cv2.VideoCapture(CAMERA_INDEX)
    if not cap.isOpened():
        print(f"ERROR: Tidak bisa membuka webcam index {CAMERA_INDEX}.")
        print("Coba ganti CAMERA_INDEX di bagian atas file ini ke 1 atau 2.")
        return

    print("\nWebcam aktif. Skor akan muncul otomatis secara real-time.")
    print("Tekan [Q] untuk keluar.\n")

    with create_face_mesh_model(static_mode=False) as face_mesh_model:
        last_result = None
        last_is_frontal = None
        no_face_counter = 0
        frame_counter = 0

        while True:
            ret, frame = cap.read()
            if not ret:
                print("ERROR: Gagal membaca frame dari webcam.")
                break

            frame = cv2.flip(frame, 1)
            display_frame = frame.copy()
            frame_counter += 1

            # Jalankan prediksi tiap N frame supaya tetap real-time tapi tidak lag
            if frame_counter % PREDICT_EVERY_N_FRAMES == 0:
                result, raw_features, is_frontal = predict_from_frame(frame, model_bundle, face_mesh_model)
                if result is None:
                    no_face_counter += 1
                    if no_face_counter >= 10:  # sekitar beberapa frame berturut-turut
                        last_result = None
                        last_is_frontal = None
                else:
                    no_face_counter = 0
                    last_result = result
                    last_is_frontal = is_frontal

            overlay_lines = ["[Q] = keluar"]
            if last_result is not None:
                if last_is_frontal is False:
                    overlay_lines.append("PERINGATAN: pose kurang frontal, skor kurang akurat")
                overlay_lines += [f"{k}: {v:.2f}" for k, v in last_result.items()]
            else:
                overlay_lines.append("Wajah tidak terdeteksi...")

            display_frame = draw_overlay_text(display_frame, overlay_lines)
            cv2.imshow("Deteksi Stroke - Facial Asymmetry (Webcam)", display_frame)

            key = cv2.waitKey(1) & 0xFF
            if key == ord("q"):
                break

    cap.release()
    cv2.destroyAllWindows()


if __name__ == "__main__":
    main()
