"""
train_model.py
============================================================
"""

import os
import sys
import cv2
import joblib
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from tqdm import tqdm

from sklearn.model_selection import train_test_split
from xgboost import XGBClassifier
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    roc_auc_score,
    confusion_matrix,
    classification_report,
)
# Tambahkan folder src ke path supaya bisa import modul lokal
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from feature_extraction import (
    extract_face_features_with_pose,
    crop_face_with_bbox,
    create_face_mesh_model,
    is_frontal_pose,
    FEATURE_COLUMNS,
)

# ============================================================
# KONFIGURASI PATH (sesuaikan jika struktur foldermu berbeda)
# ============================================================
PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATASET_ROOT = os.path.join(PROJECT_ROOT, "dataset_wajah")
MODEL_DIR = os.path.join(PROJECT_ROOT, "model")
OUTPUT_CSV = os.path.join(PROJECT_ROOT, "fitur_facial_asymmetry.csv")
MODEL_PATH = os.path.join(MODEL_DIR, "model_stroke_facial_asymmetry.joblib")
CONFUSION_MATRIX_PATH = os.path.join(MODEL_DIR, "confusion_matrix.png")
FEATURE_IMPORTANCE_PATH = os.path.join(MODEL_DIR, "feature_importance.png")

USE_BBOX_CROP = True  # set False jika tidak mau crop pakai file .txt

# Filter pose wajah: buang foto yang posenya terlalu miring/menengok,
# karena fitur asimetri jadi tidak valid kalau wajah tidak menghadap
# kamera secara frontal (lihat penjelasan lengkap di feature_extraction.py).
# Berdasarkan eksperimen pada dataset, threshold ini menyaring sekitar
# 40-50% data yang posenya kurang frontal -- sisanya jauh lebih bersih
# untuk training. Set False untuk membandingkan hasil tanpa filter ini.
USE_POSE_FILTER = True


# ============================================================
# BAGIAN 1 - BANGUN DATAFRAME FITUR DARI SELURUH DATASET
# ============================================================
def build_dataset_dataframe(dataset_root, use_bbox_crop=True, use_pose_filter=True):
    rows = []
    skipped_no_face = 0
    skipped_not_frontal = 0

    with create_face_mesh_model(static_mode=True) as face_mesh_model:
        for label_name in ["Stroke", "NonStroke"]:
            folder = os.path.join(dataset_root, label_name)
            if not os.path.isdir(folder):
                print(f"PERINGATAN: folder {folder} tidak ditemukan, dilewati.")
                continue

            image_files = sorted(f for f in os.listdir(folder) if f.lower().endswith(".jpg"))

            for fname in tqdm(image_files, desc=f"Memproses {label_name}"):
                img_path = os.path.join(folder, fname)
                txt_path = os.path.join(folder, fname.rsplit(".", 1)[0] + ".txt")

                image_bgr = cv2.imread(img_path)
                if image_bgr is None:
                    skipped_no_face += 1
                    continue

                if use_bbox_crop:
                    image_bgr = crop_face_with_bbox(image_bgr, txt_path)

                feats, pose = extract_face_features_with_pose(image_bgr, face_mesh_model)
                if feats is None:
                    skipped_no_face += 1
                    continue

                # Buang foto yang posenya terlalu miring/menengok, karena
                # fitur asimetri jadi tidak valid (lihat catatan di
                # feature_extraction.py bagian "FILTER POSE WAJAH").
                if use_pose_filter and not is_frontal_pose(pose):
                    skipped_not_frontal += 1
                    continue

                feats["filename"] = fname
                feats["label"] = label_name
                rows.append(feats)

    print(f"\nTotal gambar berhasil diproses        : {len(rows)}")
    print(f"Dilewati - wajah tidak terdeteksi/gagal baca : {skipped_no_face}")
    if use_pose_filter:
        print(f"Dilewati - pose tidak frontal (miring/menengok) : {skipped_not_frontal}")

    df = pd.DataFrame(rows)
    df = df[FEATURE_COLUMNS + ["label", "filename"]]
    return df


# ============================================================
# BAGIAN 2 - TRAINING XGBOOST
# ============================================================
def train_xgboost(df_features):
    X = df_features[FEATURE_COLUMNS].values

    # Label encoding eksplisit: 0 = NonStroke, 1 = Stroke
    label_encoder = LabelEncoder()
    label_encoder.fit(["NonStroke", "Stroke"])
    y = label_encoder.transform(df_features["label"].values)

    print("Mapping label:", dict(zip(label_encoder.classes_, label_encoder.transform(label_encoder.classes_))))

    # Split 80:20, stratify=y supaya proporsi kelas terjaga
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )
    print(f"Jumlah data latih : {len(X_train)}")
    print(f"Jumlah data uji    : {len(X_test)}")

    # scale_pos_weight: versi XGBoost dari class_weight="balanced" di
    # Random Forest -- mengompensasi jumlah kelas yang tidak seimbang
    # (di dataset kita, NonStroke biasanya lebih banyak dari Stroke).
    # Rumus dasarnya: jumlah_kelas_negatif / jumlah_kelas_positif.
    # Dikalikan 1.3 berdasarkan eksperimen tuning -- nilai ini memberi
    # keseimbangan terbaik antara precision dan recall pada dataset.
    n_nonstroke = (y_train == 0).sum()
    n_stroke = (y_train == 1).sum()
    scale_pos_weight = (n_nonstroke / n_stroke) * 1.3
    print(f"scale_pos_weight yang dipakai: {scale_pos_weight:.3f}")

    # XGBoost (Extreme Gradient Boosting):
    # - n_estimators=200    : jumlah pohon yang dibangun berurutan
    # - max_depth=5         : kedalaman tiap pohon (lebih dangkal dari RF
    #                         karena XGBoost membangun pohon secara
    #                         bertahap/boosting, bukan paralel/bagging)
    # - learning_rate=0.1   : seberapa besar kontribusi tiap pohon baru
    #                         terhadap prediksi akhir (kecil = lebih hati-hati)
    # - scale_pos_weight    : kompensasi kelas tidak seimbang (lihat atas)
    xgb_model = XGBClassifier(
        n_estimators=200,
        max_depth=5,
        learning_rate=0.1,
        scale_pos_weight=scale_pos_weight,
        random_state=42,
        eval_metric="logloss",
        n_jobs=-1,
    )
    xgb_model.fit(X_train, y_train)
    print("Training selesai.")

    return xgb_model, label_encoder, X_test, y_test



# ============================================================
# BAGIAN 3 - EVALUASI MODEL
# ============================================================
def evaluate_model(xgb_model, label_encoder, X_test, y_test):
    y_pred = xgb_model.predict(X_test)
    y_proba = xgb_model.predict_proba(X_test)[:, 1]
    accuracy = accuracy_score(y_test, y_pred)
    precision = precision_score(y_test, y_pred)
    recall = recall_score(y_test, y_pred)
    f1 = f1_score(y_test, y_pred)
    auc = roc_auc_score(y_test, y_proba)
    print("\n===== HASIL EVALUASI MODEL =====")
    print(f"Accuracy  : {accuracy:.4f} ({accuracy*100:.2f}%)")
    print(f"Precision : {precision:.4f} ({precision*100:.2f}%)")
    print(f"Recall    : {recall:.4f} ({recall*100:.2f}%)")
    print(f"F1 Score  : {f1:.4f} ({f1*100:.2f}%)")
    print(f"ROC-AUC   : {auc:.4f}")
    print("\n===== CLASSIFICATION REPORT =====")
    print(classification_report(y_test, y_pred, target_names=label_encoder.classes_))

    cm = confusion_matrix(y_test, y_pred)
    plt.figure(figsize=(6, 5))
    sns.heatmap(
        cm, annot=True, fmt="d", cmap="Blues",
        xticklabels=label_encoder.classes_, yticklabels=label_encoder.classes_,
    )
    plt.xlabel("Prediksi Model")
    plt.ylabel("Label Sebenarnya")
    plt.title("Confusion Matrix - Deteksi Stroke dari Asimetri Wajah")
    plt.tight_layout()
    os.makedirs(MODEL_DIR, exist_ok=True)
    plt.savefig(CONFUSION_MATRIX_PATH, dpi=150)
    print(f"\nConfusion matrix disimpan ke: {CONFUSION_MATRIX_PATH}")
    plt.show()


def show_feature_importance(xgb_model):
    importances = xgb_model.feature_importances_
    feat_importance_df = pd.DataFrame({
        "fitur": FEATURE_COLUMNS,
        "importance": importances,
    }).sort_values("importance", ascending=False)

    print("\n===== FEATURE IMPORTANCE =====")
    print(feat_importance_df.to_string(index=False))

    plt.figure(figsize=(8, 5))
    sns.barplot(data=feat_importance_df, x="importance", y="fitur", palette="viridis")
    plt.title("Feature Importance - XGBoost")
    plt.xlabel("Tingkat Kepentingan Fitur")
    plt.ylabel("Fitur")
    plt.tight_layout()
    plt.savefig(FEATURE_IMPORTANCE_PATH, dpi=150)
    print(f"Feature importance disimpan ke: {FEATURE_IMPORTANCE_PATH}")
    plt.show()


# ============================================================
# MAIN
# ============================================================
def main():
    if not os.path.isdir(DATASET_ROOT):
        print(f"ERROR: folder dataset tidak ditemukan di: {DATASET_ROOT}")
        print("Pastikan kamu sudah mengekstrak dataset ke folder 'dataset_wajah/' "
              "di root project, dengan subfolder 'Stroke/' dan 'NonStroke/'.")
        return

    print("=== TAHAP 1: Ekstraksi fitur dari dataset ===")
    df_features = build_dataset_dataframe(
        DATASET_ROOT, use_bbox_crop=USE_BBOX_CROP, use_pose_filter=USE_POSE_FILTER
    )

    df_features.to_csv(OUTPUT_CSV, index=False)
    print(f"\nDataframe fitur disimpan ke: {OUTPUT_CSV}")
    print("\nDistribusi kelas:")
    print(df_features["label"].value_counts())
    print("\nStatistik deskriptif tiap fitur:")
    print(df_features.describe())

    print("\n=== TAHAP 2: Training XGBoost ===")
    xgb_model, label_encoder, X_test, y_test = train_xgboost(df_features)

    print("\n=== TAHAP 3: Evaluasi model ===")
    evaluate_model(xgb_model, label_encoder, X_test, y_test)

    print("\n=== TAHAP 4: Feature importance ===")
    show_feature_importance(xgb_model)

    print("\n=== TAHAP 5: Simpan model ===")
    os.makedirs(MODEL_DIR, exist_ok=True)
    model_bundle = {
        "model": xgb_model,
        "label_encoder": label_encoder,
        "feature_cols": FEATURE_COLUMNS,
    }

    # Simpan .joblib (utama)
    joblib.dump(model_bundle, MODEL_PATH)
    print(f"Model (.joblib) disimpan ke: {MODEL_PATH}")

    # Simpan .pkl (bundle lengkap)
    import pickle
    pkl_path = os.path.join(MODEL_DIR, "model_stroke_facial_asymmetry.pkl")
    with open(pkl_path, "wb") as f:
        pickle.dump(model_bundle, f)
    print(f"Model (.pkl) disimpan ke: {pkl_path}")

    # Simpan .h5 (hanya bobot XGBoost)
    h5_path = os.path.join(MODEL_DIR, "model_stroke_facial_asymmetry.h5")
    xgb_model.save_model(h5_path)
    print(f"Model (.h5) disimpan ke: {h5_path}")

    print("\nSelesai! Sekarang kamu bisa jalankan src/inference_webcam.py untuk demo.")

    


if __name__ == "__main__":
    main()
