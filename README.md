# Cerevia 🫀🤖

Cerevia adalah aplikasi pemantauan kesehatan kardiovaskular berbasis Android yang memadukan teknologi *wearable* (IoT) dan *Artificial Intelligence* (AI). Aplikasi ini dirancang untuk melakukan *screening* dini terhadap anomali detak jantung seperti *Atrial Fibrillation* (AFib) serta memprediksi risiko stroke.

---

## ✨ Fitur Utama

*   ⌚ **Integrasi Smartwatch (BLE):** Terhubung langsung dengan perangkat Garmin menggunakan Bluetooth Low Energy untuk mengambil data sensor mentah (RR/PPI interval) secara *real-time*.
*   🧠 **Analisis AI Mutakhir:** Menggunakan arsitektur *Deep Learning* (CNN + Transformer) pada sisi *backend* untuk mendeteksi pola detak jantung yang tidak beraturan dengan tingkat sensitivitas yang dioptimalkan untuk klinis.
*   🚑 **Sistem Darurat (SOS):** Fitur SOS satu klik yang secara otomatis merakit pesan darurat beserta koordinat lokasi (Google Maps) lalu mengirimkannya ke kontak darurat via WhatsApp.
*   🏥 **Direktori Medis:** Kemudahan mencari informasi dokter dan rumah sakit untuk konsultasi lebih lanjut.
*   📱 **UI/UX Modern & Responsif:** Dibangun sepenuhnya menggunakan Jetpack Compose dengan pendekatan *Clean Architecture* & MVVM untuk performa aplikasi yang cepat dan mulus.

---

## 🏗️ Arsitektur Sistem

Proyek ini terdiri dari dua komponen utama:

1.  **Aplikasi Android (`Apps/cerevia_android`)**: Antarmuka pengguna yang berinteraksi dengan sensor (Bluetooth) dan menampilkan hasil prediksi.
2.  **API Backend (`Apps/backend`)**: Server Python FastAPI yang menampung model *Machine Learning* untuk memproses dan menganalisis data sensor.

### ⚙️ Cara Kerja Analisis (Tahap 2 - AFib Detection)
1.  **Pengumpulan Data:** Aplikasi Android menarik data interval detak jantung dari jam tangan pintar Garmin.
2.  **Transmisi:** Data mentah dikirim ke server FastAPI lokal melalui jaringan Wi-Fi.
3.  **Pemrosesan (Windowing):** Python membagi data detak jantung ke dalam rentang spesifik (*window size* = 30 detak) untuk melihat variasi interval.
4.  **Prediksi Model:** Model *Transformer* mengekstrak fitur dan menggunakan `[CLS]` token untuk menyimpulkan probabilitas *Atrial Fibrillation*.
5.  **Hasil:** Jika probabilitas $\ge 0.5$, sistem mengklasifikasikannya sebagai indikasi AFib, dan hasilnya langsung dikembalikan ke Android dalam format JSON.

---

## 🛠️ Tech Stack

**Frontend (Android/Kotlin):**
*   Jetpack Compose (*UI*)
*   Hilt (*Dependency Injection*)
*   Retrofit & OkHttp (*REST API*)
*   Room Database (*Local Storage*)
*   Coroutines & Flow (*Asynchronous*)

**Backend (Python):**
*   FastAPI (*API Server*)
*   TensorFlow/Keras (*Machine Learning*)
*   Uvicorn (*ASGI Server*)
*   NumPy & Pandas (*Data Processing*)

---

## 🚀 Cara Menjalankan Proyek (Local Development)

Karena sistem ini melibatkan komunikasi antara HP Android dan Laptop (sebagai server), **pastikan kedua perangkat terhubung pada jaringan Wi-Fi yang sama.**

### 1. Menjalankan Backend (Python)
1. Buka terminal dan arahkan ke folder backend:
   ```bash
   cd Apps/backend
   ```
2. Pastikan Anda sudah menginstal *library* yang dibutuhkan (FastAPI, Uvicorn, TensorFlow, dll). Direkomendasikan menggunakan *virtual environment*.
   ```bash
   pip install fastapi uvicorn tensorflow numpy pandas
   ```
3. Jalankan server menggunakan Uvicorn. Pastikan menggunakan `--host 0.0.0.0` agar bisa diakses oleh perangkat lain di jaringan Wi-Fi.
   ```bash
   uvicorn main:app --host 0.0.0.0 --port 8000 --reload
   ```
4. Catat **IP Address Wi-Fi laptop Anda** (misal: `192.168.1.15`). Anda bisa mengeceknya dengan menjalankan perintah `ipconfig` di Command Prompt Windows.

### 2. Konfigurasi Aplikasi Android
1. Buka folder `Apps/cerevia_android` menggunakan **Android Studio**.
2. Cari file `NetworkModule.kt` (berada di folder `di/`).
3. Ubah `BASE_URL` Retrofit agar mengarah ke IP Address laptop Anda.
   ```kotlin
   // Ganti dengan IP Address laptop Anda yang menjalankan backend
   val BASE_URL = "http://192.168.1.15:8000/"
   ```
4. Hubungkan HP Android ke laptop dengan kabel USB, lalu *Run* aplikasi dari Android Studio.

---

## 💡 Kustomisasi (Mock Data)

Untuk keperluan *testing* dan presentasi, beberapa data masih menggunakan data statis (*hardcoded*). Anda bisa mengubahnya di file berikut pada kode Android:
*   **Data Dokter & Rumah Sakit**: `DirectoryScreen.kt` (variabel `mockDoctors` dan `mockHospitals`).
*   **Nomor Tujuan SOS (WhatsApp)**: `Components.kt` (variabel `phoneNumber`).

---
*Proyek ini dikembangkan sebagai prototipe sistem kesehatan terintegrasi Cerevia.*
