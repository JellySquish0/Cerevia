"""
feature_extraction.py
============================================================
Modul ini berisi semua fungsi untuk mendeteksi landmark wajah
(MediaPipe Face Landmarker - Tasks API) dan menghitung 6 fitur
asimetri wajah.

CATATAN MIGRASI API:
MediaPipe versi lama (<=0.10.9) punya API sederhana "mp.solutions.
face_mesh" yang dipanggil dengan model.process(image). Versi tersebut
TIDAK tersedia untuk Python 3.12/3.13, sehingga modul ini memakai API
BARU MediaPipe yaitu "Tasks API" / FaceLandmarker, yang dipanggil
dengan model.detect(image) atau model.detect_for_video(image, ts).

Topologi landmark (468 titik & index tiap titik wajah) TETAP SAMA
antara API lama dan API baru, jadi index landmark kunci di bawah ini
tidak berubah sama sekali.

Modul ini dipakai BERSAMA oleh train_model.py, inference_webcam.py,
dan inference_image.py, supaya logika ekstraksi fitur SELALU SAMA
persis antara saat training dan saat inferensi.
============================================================
"""

import os
import urllib.request
import time

import cv2
import numpy as np
import mediapipe as mp
from mediapipe.tasks import python as mp_python
from mediapipe.tasks.python import vision as mp_vision

# ------------------------------------------------------------
# Model Face Landmarker (.task) akan didownload otomatis dan
# disimpan di folder model/ supaya tidak perlu download ulang
# setiap kali script dijalankan.
# ------------------------------------------------------------
PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODEL_DIR = os.path.join(PROJECT_ROOT, "model")
FACE_LANDMARKER_MODEL_PATH = os.path.join(MODEL_DIR, "face_landmarker.task")
FACE_LANDMARKER_MODEL_URL = (
    "https://storage.googleapis.com/mediapipe-models/face_landmarker/"
    "face_landmarker/float16/1/face_landmarker.task"
)


def ensure_face_landmarker_model():
    """
    Memastikan file model face_landmarker.task tersedia di folder model/.
    Jika belum ada, download otomatis dari server resmi MediaPipe/Google.
    """
    os.makedirs(MODEL_DIR, exist_ok=True)
    if os.path.exists(FACE_LANDMARKER_MODEL_PATH) and os.path.getsize(FACE_LANDMARKER_MODEL_PATH) > 100_000:
        return FACE_LANDMARKER_MODEL_PATH

    print("Model face_landmarker.task belum ada, mendownload otomatis...")
    print(f"Sumber: {FACE_LANDMARKER_MODEL_URL}")
    print("(Ukuran sekitar 3-4 MB, mungkin perlu beberapa detik)")
    try:
        # Pakai header User-Agent supaya tidak dianggap bot oleh server
        request = urllib.request.Request(
            FACE_LANDMARKER_MODEL_URL,
            headers={"User-Agent": "Mozilla/5.0 (compatible; stroke-detection-project/1.0)"},
        )
        with urllib.request.urlopen(request, timeout=30) as response, \
                open(FACE_LANDMARKER_MODEL_PATH, "wb") as out_file:
            out_file.write(response.read())

        downloaded_size = os.path.getsize(FACE_LANDMARKER_MODEL_PATH)
        if downloaded_size < 100_000:
            # File terlalu kecil untuk berupa model asli -> kemungkinan
            # yang terunduh adalah halaman error, bukan file model.
            raise RuntimeError(
                f"File hasil download cuma {downloaded_size} bytes, "
                "kemungkinan bukan file model yang valid."
            )
        print(f"Model berhasil didownload ke: {FACE_LANDMARKER_MODEL_PATH} ({downloaded_size:,} bytes)")
    except Exception as e:
        # Bersihkan file partial/invalid jika gagal di tengah jalan
        if os.path.exists(FACE_LANDMARKER_MODEL_PATH):
            os.remove(FACE_LANDMARKER_MODEL_PATH)
        raise RuntimeError(
            "Gagal mendownload model face_landmarker.task secara otomatis.\n"
            f"Error: {e}\n\n"
            "Solusi manual:\n"
            "  1. Buka URL berikut di browser (akan otomatis download file):\n"
            f"     {FACE_LANDMARKER_MODEL_URL}\n"
            "  2. Simpan file dengan nama PERSIS 'face_landmarker.task'\n"
            f"  3. Pindahkan file tersebut ke folder:\n"
            f"     {MODEL_DIR}\n"
            "  4. Jalankan ulang script ini."
        ) from e

    return FACE_LANDMARKER_MODEL_PATH


# ------------------------------------------------------------
# Index landmark kunci MediaPipe Face Mesh (468 titik wajah)
# Index ini SELALU SAMA posisinya secara anatomis di tiap wajah,
# karena MediaPipe sudah memetakan tiap titik ke bagian wajah
# tertentu (misal index 61 SELALU sudut mulut kiri).
# ------------------------------------------------------------
LM_MOUTH_LEFT = 61
LM_MOUTH_RIGHT = 291
LM_EYE_LEFT_OUTER = 33
LM_EYE_LEFT_INNER = 133
LM_EYE_RIGHT_OUTER = 263
LM_EYE_RIGHT_INNER = 362
LM_EYE_LEFT_UPPER = 159
LM_EYE_LEFT_LOWER = 145
LM_EYE_RIGHT_UPPER = 386
LM_EYE_RIGHT_LOWER = 374
LM_NOSE_TIP = 1
LM_NOSE_TOP = 168          # antara kedua alis, acuan garis tengah wajah
LM_CHIN = 152
LM_JAW_LEFT = 234
LM_JAW_RIGHT = 454
LM_FACE_TOP = 10           # titik atas dahi, acuan garis tengah vertikal


FEATURE_COLUMNS = [
    "mouth_slope_deg",
    "mouth_height_diff",
    "eye_symmetry_diff",
    "eye_opening_diff",
    "nose_deviation",
    "left_right_face_ratio",
    "jaw_asymmetry",
]


def _to_xy(landmarks, idx, img_w, img_h):
    """Ambil koordinat (x, y) piksel dari satu landmark MediaPipe."""
    lm = landmarks[idx]
    return np.array([lm.x * img_w, lm.y * img_h])


def _perpendicular_distance(point, line_origin, line_unit):
    """Jarak tegak lurus sebuah titik terhadap garis tengah wajah."""
    v = point - line_origin
    proj_len = np.dot(v, line_unit)
    proj_point = line_origin + proj_len * line_unit
    return np.linalg.norm(point - proj_point)


class FaceMeshModel:
    """
    Wrapper di sekitar mediapipe.tasks.python.vision.FaceLandmarker
    (Tasks API baru), dibuat supaya pemanggilannya tetap sederhana
    persis seperti API lama: cukup panggil .process(image_bgr) dan
    dapatkan list landmark (atau None jika wajah tidak terdeteksi).

    static_mode=True  -> mode IMAGE, dipakai untuk gambar diam
                          (dataset / foto upload). Tiap gambar dideteksi
                          dari nol tanpa info frame sebelumnya.
    static_mode=False -> mode VIDEO, dipakai untuk webcam/video.
                          MediaPipe memakai info frame sebelumnya untuk
                          tracking yang lebih stabil & cepat.
    """

    def __init__(self, static_mode=True):
        model_path = ensure_face_landmarker_model()
        with open(model_path, "rb") as f:
            model_bytes = f.read()
        base_options = mp_python.BaseOptions(model_asset_buffer=model_bytes)

        running_mode = (
            mp_vision.RunningMode.IMAGE if static_mode else mp_vision.RunningMode.VIDEO
        )

        options = mp_vision.FaceLandmarkerOptions(
            base_options=base_options,
            running_mode=running_mode,
            num_faces=1,
            min_face_detection_confidence=0.5,
            min_face_presence_confidence=0.5,
            min_tracking_confidence=0.5,
            output_face_blendshapes=False,
            output_facial_transformation_matrixes=False,
        )

        self._landmarker = mp_vision.FaceLandmarker.create_from_options(options)
        self._static_mode = static_mode
        self._start_time = time.monotonic()

    def process(self, image_rgb):
        """
        Menjalankan deteksi landmark wajah pada satu gambar RGB (numpy array).
        Mengembalikan list landmark (punya atribut .x, .y, .z) dari wajah
        pertama yang terdeteksi, atau None jika tidak ada wajah.
        """
        mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=image_rgb)

        if self._static_mode:
            result = self._landmarker.detect(mp_image)
        else:
            timestamp_ms = int((time.monotonic() - self._start_time) * 1000)
            result = self._landmarker.detect_for_video(mp_image, timestamp_ms)

        if not result.face_landmarks:
            return None

        return result.face_landmarks[0]  # list landmark wajah pertama

    def close(self):
        self._landmarker.close()

    # Mendukung pemakaian "with create_face_mesh_model() as model:"
    # persis seperti API lama, supaya file lain tidak perlu berubah.
    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()


def create_face_mesh_model(static_mode=True):
    """
    Factory function untuk membuat objek deteksi landmark wajah.

    static_mode=True  -> dipakai untuk gambar diam (dataset/foto upload).
    static_mode=False -> dipakai untuk video/webcam (mode tracking).
    """
    return FaceMeshModel(static_mode=static_mode)


def extract_face_features(image_bgr, face_mesh_model):
    """
    Mengambil satu gambar wajah (format BGR, hasil cv2.imread atau
    cv2.VideoCapture) dan mengembalikan dictionary berisi 6 fitur
    asimetri wajah.

    Mengembalikan None jika wajah tidak terdeteksi.
    """
    img_h, img_w = image_bgr.shape[:2]
    image_rgb = cv2.cvtColor(image_bgr, cv2.COLOR_BGR2RGB)
    landmarks = face_mesh_model.process(image_rgb)

    if landmarks is None:
        return None

    return _compute_features_from_landmarks(landmarks, img_w, img_h)


def _compute_features_from_landmarks(landmarks, img_w, img_h):
    """
    Fungsi inti yang menghitung 6 fitur asimetri wajah dari list
    landmark MediaPipe yang SUDAH terdeteksi. Dipisah dari
    extract_face_features() supaya logikanya bisa dipakai bersama
    oleh extract_face_features() dan extract_face_features_with_pose()
    tanpa duplikasi kode.
    """
    mouth_left = _to_xy(landmarks, LM_MOUTH_LEFT, img_w, img_h)
    mouth_right = _to_xy(landmarks, LM_MOUTH_RIGHT, img_w, img_h)
    eye_left_outer = _to_xy(landmarks, LM_EYE_LEFT_OUTER, img_w, img_h)
    eye_left_inner = _to_xy(landmarks, LM_EYE_LEFT_INNER, img_w, img_h)
    eye_right_outer = _to_xy(landmarks, LM_EYE_RIGHT_OUTER, img_w, img_h)
    eye_right_inner = _to_xy(landmarks, LM_EYE_RIGHT_INNER, img_w, img_h)
    eye_left_upper = _to_xy(landmarks, LM_EYE_LEFT_UPPER, img_w, img_h)
    eye_left_lower = _to_xy(landmarks, LM_EYE_LEFT_LOWER, img_w, img_h)
    eye_right_upper = _to_xy(landmarks, LM_EYE_RIGHT_UPPER, img_w, img_h)
    eye_right_lower = _to_xy(landmarks, LM_EYE_RIGHT_LOWER, img_w, img_h)
    nose_tip = _to_xy(landmarks, LM_NOSE_TIP, img_w, img_h)
    chin = _to_xy(landmarks, LM_CHIN, img_w, img_h)
    jaw_left = _to_xy(landmarks, LM_JAW_LEFT, img_w, img_h)
    jaw_right = _to_xy(landmarks, LM_JAW_RIGHT, img_w, img_h)
    face_top = _to_xy(landmarks, LM_FACE_TOP, img_w, img_h)

    # Lebar wajah sebagai faktor normalisasi skala (supaya wajah besar/
    # kecil di gambar tetap fair dibandingkan)
    face_width = np.linalg.norm(jaw_right - jaw_left)
    if face_width < 1e-6:
        return None

    # Garis tengah wajah (vertical midline): dari atas dahi ke dagu.
    # Dipakai sebagai acuan "lurus" untuk mengukur deviasi & asimetri.
    midline_vec = chin - face_top
    midline_len = np.linalg.norm(midline_vec)
    if midline_len < 1e-6:
        return None
    midline_unit = midline_vec / midline_len

    # --- FITUR 1: Kemiringan mulut ---
    # Sudut kemiringan garis sudut-mulut-kiri ke sudut-mulut-kanan.
    # Wajah normal -> garis hampir horizontal. Droop -> miring.
    mouth_vec = mouth_right - mouth_left
    mouth_slope_deg = np.degrees(np.arctan2(mouth_vec[1], mouth_vec[0]))

    # --- FITUR 2: Perbedaan tinggi sudut bibir kiri vs kanan ---
    mouth_height_diff = abs(mouth_left[1] - mouth_right[1]) / face_width

    # --- FITUR 3: Simetri mata kiri vs kanan ---
    # Didekati dari perbedaan lebar bukaan mata kiri-kanan.
    eye_left_width = np.linalg.norm(eye_left_outer - eye_left_inner)
    eye_right_width = np.linalg.norm(eye_right_outer - eye_right_inner)
    eye_symmetry_diff = abs(eye_left_width - eye_right_width) / face_width
    eye_left_opening = np.linalg.norm(eye_left_upper - eye_left_lower)
    eye_right_opening = np.linalg.norm(eye_right_upper - eye_right_lower)
    eye_opening_diff = abs(eye_left_opening - eye_right_opening) / face_width

    # --- FITUR 4: Deviasi hidung terhadap garis tengah wajah ---
    nose_deviation = _perpendicular_distance(nose_tip, face_top, midline_unit) / face_width

    # --- FITUR 5: Rasio wajah kiri dan kanan ---
    jaw_left_dist = _perpendicular_distance(jaw_left, face_top, midline_unit)
    jaw_right_dist = _perpendicular_distance(jaw_right, face_top, midline_unit)
    if min(jaw_left_dist, jaw_right_dist) < 1e-6:
        left_right_ratio = 1.0
    else:
        left_right_ratio = max(jaw_left_dist, jaw_right_dist) / min(jaw_left_dist, jaw_right_dist)

    # --- FITUR 6: Jaw asymmetry ---
    jaw_left_to_chin = np.linalg.norm(jaw_left - chin)
    jaw_right_to_chin = np.linalg.norm(jaw_right - chin)
    jaw_asymmetry = abs(jaw_left_to_chin - jaw_right_to_chin) / face_width

    return {
    "mouth_slope_deg": mouth_slope_deg,
    "mouth_height_diff": mouth_height_diff,
    "eye_symmetry_diff": eye_symmetry_diff,
    "eye_opening_diff": eye_opening_diff,
    "nose_deviation": nose_deviation,
    "left_right_face_ratio": left_right_ratio,
    "jaw_asymmetry": jaw_asymmetry,
    }


# ============================================================
# FILTER POSE WAJAH (FRONTAL CHECK)
# ============================================================
# Kenapa filter ini penting (penting untuk dijelaskan saat sidang):
# Fitur asimetri wajah (terutama left_right_face_ratio & nose_deviation)
# mengasumsikan wajah menghadap LURUS ke kamera. Kalau wajah miring
# (roll) atau menengok ke samping (yaw), wajah akan TERLIHAT asimetris
# secara geometris padahal orangnya sehat -- ini disebut "false
# asymmetry akibat pose", bukan asimetri medis yang sesungguhnya.
#
# Maka sebelum data dipakai untuk training, kita filter dulu hanya
# foto dengan pose "cukup frontal" (menghadap kamera).

# Ambang batas (threshold) hasil eksperimen pada dataset:
# - ROLL_THRESHOLD_DEG : toleransi kemiringan kepala (derajat)
# - YAW_RATIO_THRESHOLD: toleransi "menengok" (1.0 = lurus sempurna,
#   makin besar makin menengok jauh dari kamera)
ROLL_THRESHOLD_DEG = 25.0
YAW_RATIO_THRESHOLD = 2.0


def estimate_head_pose(landmarks, img_w, img_h):
    """
    Estimasi sederhana orientasi kepala dari landmark 2D MediaPipe.
    Ini BUKAN head pose 3D yang presisi (seperti solvePnP), melainkan
    proxy geometris sederhana yang cukup untuk menyaring foto yang
    posenya terlalu miring/menengok -- dan jauh lebih mudah dijelaskan
    saat sidang dibanding head pose 3D penuh.

    Mengembalikan dict:
    - roll_deg  : sudut kemiringan kepala (garis mata kiri-kanan
                  terhadap garis horizontal). 0 derajat = kepala tegak.
    - yaw_ratio : rasio jarak horizontal hidung ke rahang kiri vs kanan.
                  1.0 = wajah lurus menghadap kamera. Semakin jauh dari
                  1.0, semakin wajah menengok ke salah satu sisi.
    """
    eye_left = _to_xy(landmarks, LM_EYE_LEFT_OUTER, img_w, img_h)
    eye_right = _to_xy(landmarks, LM_EYE_RIGHT_OUTER, img_w, img_h)
    nose_tip = _to_xy(landmarks, LM_NOSE_TIP, img_w, img_h)
    jaw_left = _to_xy(landmarks, LM_JAW_LEFT, img_w, img_h)
    jaw_right = _to_xy(landmarks, LM_JAW_RIGHT, img_w, img_h)

    # --- Roll: kemiringan garis mata kiri-kanan terhadap horizontal ---
    eye_vec = eye_right - eye_left
    roll_deg = float(np.degrees(np.arctan2(eye_vec[1], eye_vec[0])))

    # --- Yaw: rasio jarak horizontal hidung ke jaw kiri vs jaw kanan ---
    dist_nose_to_jawleft = abs(nose_tip[0] - jaw_left[0])
    dist_nose_to_jawright = abs(jaw_right[0] - nose_tip[0])
    if min(dist_nose_to_jawleft, dist_nose_to_jawright) < 1e-6:
        yaw_ratio = 1.0
    else:
        yaw_ratio = max(dist_nose_to_jawleft, dist_nose_to_jawright) / min(
            dist_nose_to_jawleft, dist_nose_to_jawright
        )

    return {"roll_deg": roll_deg, "yaw_ratio": float(yaw_ratio)}


def is_frontal_pose(pose: dict, roll_threshold=ROLL_THRESHOLD_DEG, yaw_threshold=YAW_RATIO_THRESHOLD):
    """
    Mengecek apakah pose hasil estimate_head_pose() cukup "frontal"
    (menghadap kamera) untuk dipakai sebagai data training yang valid.

    Mengembalikan True jika wajah cukup frontal, False jika terlalu
    miring/menengok sehingga berisiko menghasilkan fitur asimetri
    yang menyesatkan.
    """
    return abs(pose["roll_deg"]) <= roll_threshold and pose["yaw_ratio"] <= yaw_threshold

def extract_face_features_checked(image_bgr, face_mesh_model, check_pose=True):
    """
    Sama seperti extract_face_features(), tapi SEKALIGUS mengecek apakah
    pose wajah cukup frontal. Dipakai di inference (webcam & gambar)
    supaya perilakunya KONSISTEN dengan filter yang dipakai saat training
    -- kalau tidak, fitur asimetri bisa salah tinggi hanya karena wajah
    menengok/miring, bukan karena kondisi medis (lihat catatan di bagian
    "FILTER POSE WAJAH" di atas).

    Mengembalikan tuple (features, pose, is_frontal):
    - features   : dict 6 fitur asimetri, atau None jika wajah tidak
                   terdeteksi
    - pose       : dict {roll_deg, yaw_ratio}, atau None
    - is_frontal : True/False, apakah pose cukup frontal untuk dipercaya.
                   Selalu True jika check_pose=False.
    """
    features, pose = extract_face_features_with_pose(image_bgr, face_mesh_model)
    if features is None:
        return None, None, None

    is_frontal = is_frontal_pose(pose) if check_pose else True
    return features, pose, is_frontal

def extract_face_features_with_pose(image_bgr, face_mesh_model):
    """
    Sama seperti extract_face_features(), tapi SEKALIGUS mengembalikan
    info pose wajah (roll_deg, yaw_ratio). Dipakai khusus saat membangun
    dataset training, supaya proses deteksi landmark hanya dijalankan
    SEKALI per gambar (efisien), lalu hasilnya dipakai untuk dua hal:
    menghitung fitur asimetri DAN mengecek apakah pose-nya frontal.

    Mengembalikan tuple (features_dict, pose_dict), atau (None, None)
    jika wajah tidak terdeteksi.
    """
    img_h, img_w = image_bgr.shape[:2]
    image_rgb = cv2.cvtColor(image_bgr, cv2.COLOR_BGR2RGB)
    landmarks = face_mesh_model.process(image_rgb)

    if landmarks is None:
        return None, None

    features = _compute_features_from_landmarks(landmarks, img_w, img_h)
    if features is None:
        return None, None

    pose = estimate_head_pose(landmarks, img_w, img_h)
    return features, pose


def crop_face_with_bbox(image_bgr, txt_path, margin=0.15):
    """
    Crop gambar memakai bounding box dari file .txt format YOLO:
    class_id x_center y_center width height (semua ternormalisasi 0-1).

    margin: persentase tambahan area di sekitar bbox supaya bagian
    pinggir wajah (rahang/dagu) tidak terpotong.

    Jika file .txt tidak ada / gagal dibaca, mengembalikan gambar asli.
    """
    if not os.path.exists(txt_path):
        return image_bgr

    img_h, img_w = image_bgr.shape[:2]
    try:
        with open(txt_path, "r") as f:
            line = f.readline().strip()
        parts = line.split()
        if len(parts) < 5:
            return image_bgr
        _, xc, yc, bw, bh = parts[:5]
        xc, yc, bw, bh = float(xc), float(yc), float(bw), float(bh)
    except Exception:
        return image_bgr

    bw *= (1 + margin)
    bh *= (1 + margin)

    x1 = max(0, int((xc - bw / 2) * img_w))
    y1 = max(0, int((yc - bh / 2) * img_h))
    x2 = min(img_w, int((xc + bw / 2) * img_w))
    y2 = min(img_h, int((yc + bh / 2) * img_h))

    if x2 <= x1 or y2 <= y1:
        return image_bgr

    return image_bgr[y1:y2, x1:x2]
