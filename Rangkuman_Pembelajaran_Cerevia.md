# Rangkuman Pembelajaran Arsitektur Proyek Cerevia

Dokumen ini berisi rangkuman dari semua yang telah kita bahas mengenai alur kerja aplikasi Cerevia, mulai dari struktur folder Android, komunikasi API, hingga integrasi dengan Model Machine Learning (Python).

---

## 1. Struktur Folder Android (Kotlin)
Proyek Android Cerevia menggunakan arsitektur modern (mirip Clean Architecture/MVVM) dengan pembagian tugas yang jelas:

*   **`bluetooth/`**: Mengatur komunikasi BLE ke perangkat luar. Contohnya `GarminBleManager.kt` untuk menyambung dan mengambil data detak jantung dari smartwatch Garmin.
*   **`data/`**: Pusat pengelolaan sumber data.
    *   **`local/`**: Mengatur database offline di HP (menggunakan Room DB), misalnya untuk riwayat tes di `Database.kt`.
    *   **`remote/`**: Jalur komunikasi ke server/API. Berisi antarmuka `CereviaApi.kt` dan folder `dto/` (Data Transfer Object) sebagai "amplop" data untuk dikirim/diterima dari server.
    *   **`repository/`**: Jembatan yang merapikan data dari *local* maupun *remote* sebelum dikirim ke tampilan UI.
*   **`di/` (Dependency Injection)**: Menggunakan Hilt untuk menyiapkan "alat-alat" secara otomatis di seluruh aplikasi. Contoh: `NetworkModule.kt` untuk merakit koneksi internet (Retrofit).
*   **`domain/`**: Jantung aturan bisnis, berisi kerangka data asli (Model) seperti bentuk data `MedicalRecord`, `Doctor`, atau `Hospital` (ada di `Models.kt`).
*   **`ui/`**: Tampilan antarmuka pengguna (menggunakan Jetpack Compose).
    *   **`components/`**: Desain kecil yang bisa dipakai berulang (Tombol SOS, Kartu, dsb).
    *   **`screens/`**: Halaman utuh (Beranda, Direktori, Tahap Analisis).

---

## 2. Bagian yang Bisa Dikustomisasi (Nomor HP & Data Dokter)

Saat ini aplikasi menggunakan data statis (*hardcoded* / *mock data*) untuk keperluan desain UI. Anda bisa mengubahnya di bagian berikut:
*   **Data Dokter & Rumah Sakit**: Berada di file `DirectoryScreen.kt` (mulai baris 357). Terdapat variabel `mockDoctors` dan `mockHospitals`. Anda bisa mengubah nama, spesialis, hingga foto. (Jika ingin menambahkan nomor HP dokter, cukup tambahkan format `phone` yang sudah ada di class `Doctor` pada `Models.kt`).
*   **Nomor HP Darurat (Tombol SOS)**: Berada di file `Components.kt` baris 48.
    ```kotlin
    val phoneNumber = "6282129738928" // Ganti dengan nomor yang dituju
    ```
    Saat tombol SOS ditekan, aplikasi akan otomatis merakit pesan darurat lengkap dengan titik koordinat Google Maps dan mengirimkannya ke WhatsApp nomor tersebut.

---

## 3. Konsep Transfer Data (Wi-Fi Lokal vs Bluetooth)

*   **Bluetooth (BLE)**: Digunakan HANYA untuk mengambil data sensor mentah (detak jantung) dari Smartwatch Garmin ke HP Android.
*   **Wi-Fi Lokal**: Digunakan untuk mengirim data yang sudah dibungkus dari HP Android ke Server Python (Laptop) untuk diprediksi oleh AI. 
    Keduanya harus berada di jaringan Wi-Fi yang sama, karena API di Android diarahkan ke IP laptop Anda: `http://192.168.110.196:8000/`.

---

## 4. Cara Kerja API (Retrofit di Android)
Bagaimana Android mengirim data ke Server? Retrofit bekerja di belakang layar melalui 3 pilar utama:

1.  **Cetakan / Kerangka (CereviaApi.kt)**: 
    Mendefinisikan URL tujuan dan bentuk data (DTO).
    ```kotlin
    @POST("predict/stroke-risk")
    suspend fun predictStrokeRisk(@Body request: StrokePredictionRequestDto): Response<StrokePredictionResponseDto>
    ```
2.  **Pabrik Pembuat Mesin (NetworkModule.kt)**:
    Memberitahu Hilt cara membuat Retrofit dan menghubungkannya ke IP Laptop.
    ```kotlin
    @Provides
    fun provideCereviaApi(retrofit: Retrofit): CereviaApi {
        return retrofit.create(CereviaApi::class.java) // Retrofit membuat kode internet otomatis di sini
    }
    ```
3.  **Eksekusi (ViewModel, misal: Stage1ViewModel.kt)**:
    Ketika tombol submit diklik, data UI dibungkus ke dalam DTO, lalu fungsi API dipanggil.
    ```kotlin
    val response = api.predictStrokeRisk(request)
    ```

---

## 5. Cara Kerja Backend Python (FastAPI & Machine Learning)

Di sisi server (laptop), ada proyek terpisah berbasis **Python (FastAPI)** yang berjalan di file `Apps/backend/main.py`.

### Alur Analisis Tahap 2 (Deteksi AFib dari Detak Jantung):
1.  **Pengambilan Data dari Jam (GarminBleManager.kt di Android)**:
    Jam Garmin mengirimkan interval antar detak jantung (RR/PPI Interval). Android membacanya, mengubah satuannya menjadi *milliseconds*, lalu mengumpulkannya menjadi daftar angka, misalnya `[850.0, 862.0, 834.0]`.
2.  **Pengiriman ke Python**:
    Array angka tersebut dikirim via Wi-Fi ke rute `@app.post("/predict/ppg-afib")` di `main.py`.
3.  **Windowing (Pemotongan Data)**:
    Python memotong deretan detak jantung itu per 30 detak (`WINDOW_SIZE = 30`) dan menghitung selisih jarak antar detak. Jika jaraknya tidak beraturan, itu indikasi *Atrial Fibrillation* (AFib).
4.  **Prediksi oleh Model (CNN + Transformer)**:
    Data dimasukkan ke model AI Keras yang Anda buat (`model_tahap_2.predict`). Model ini menggunakan arsitektur **Transformer** (dibuktikan dengan adanya `ClassToken` di `main.py`). `[CLS]` token berfungsi untuk menyimpulkan dari keseluruhan urutan 30 detak tadi apakah terdapat pola AFib.
5.  **Keputusan Akhir (Threshold 0.5)**:
    Python akan menghitung rata-rata probabilitas prediksi. Jika rata-ratanya `>= 0.5` (50%), maka Python memutuskan itu adalah **AFib**. Angka 0.5 adalah ambang batas standar (konservatif) agar aplikasi lebih sensitif dalam screening klinis (lebih baik salah mendiagnosa orang sehat, daripada salah membiarkan orang sakit).
6.  **Kembali ke Android**:
    Python membalas ke Android dengan format JSON berisi status (AFib/NSR), lalu HP Anda akan menampilkan hasilnya ke pengguna.
