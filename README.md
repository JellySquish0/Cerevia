# Cerevia 🫀🤖

Cerevia adalah aplikasi pemantauan kesehatan kardiovaskular berbasis Android yang memadukan teknologi *wearable* (IoT) dan *Artificial Intelligence* (AI). Aplikasi ini dirancang untuk melakukan *screening* dini terhadap anomali detak jantung seperti *Atrial Fibrillation* (AFib) serta memprediksi risiko stroke.

## ✨ Fitur Utama

*   ⌚ **Integrasi Smartwatch (BLE):** Terhubung langsung dengan perangkat Garmin menggunakan Bluetooth Low Energy untuk mengambil data sensor mentah (RR/PPI interval) secara *real-time*.
*   🧠 **Analisis AI Mutakhir:** Menggunakan arsitektur *Deep Learning* (CNN + Transformer) pada sisi *backend* untuk mendeteksi pola detak jantung yang tidak beraturan dengan tingkat sensitivitas yang dioptimalkan untuk klinis.
*   🚑 **Sistem Darurat (SOS):** Fitur SOS satu klik yang secara otomatis merakit pesan darurat beserta koordinat lokasi (Google Maps) lalu mengirimkannya ke kontak darurat via WhatsApp.
*   🏥 **Direktori Medis:** Kemudahan mencari informasi dokter dan rumah sakit untuk konsultasi lebih lanjut.
*   📱 **UI/UX Modern & Responsif:** Dibangun sepenuhnya menggunakan Jetpack Compose dengan pendekatan *Clean Architecture* & MVVM untuk performa aplikasi yang cepat dan mulus.

## 🛠️ Tech Stack

**Frontend (Android/Kotlin):**
*   [Jetpack Compose](https://developer.android.com/jetpack/compose) - *Modern UI Toolkit*
*   [Hilt](https://dagger.dev/hilt/) - *Dependency Injection*
*   [Retrofit](https://square.github.io/retrofit/) & OkHttp - *REST API Networking*
*   [Room Database](https://developer.android.com/training/data-storage/room) - *Local Data Persistence*
*   Coroutines & Flow - *Asynchronous Programming*

**Backend (Python):**
*   [FastAPI](https://fastapi.tiangolo.com/) - *High-performance API Server*
*   [TensorFlow/Keras](https://www.tensorflow.org/) - *Machine Learning Framework (CNN + Transformer)*
*   NumPy & Pandas - *Data Preprocessing & Windowing*

## ⚙️ Cara Kerja Analisis (Tahap 2 - AFib Detection)
1. **Pengumpulan Data:** Aplikasi Android menarik data interval detak jantung dari jam tangan pintar Garmin.
2. **Transmisi:** Data mentah dikirim ke server FastAPI lokal melalui jaringan Wi-Fi.
3. **Pemrosesan (Windowing):** Python membagi data detak jantung ke dalam rentang spesifik (*window size* = 30 detak) untuk melihat variasi interval.
4. **Prediksi Model:** Model *Transformer* mengekstrak fitur dan menggunakan `[CLS]` token untuk menyimpulkan probabilitas *Atrial Fibrillation*.
5. **Hasil:** Jika probabilitas $\ge 0.5$, sistem mengklasifikasikannya sebagai indikasi AFib, dan hasilnya langsung ditampilkan di layar *smartphone* pengguna.
