📈 LSTM Time Series Prediction App
Aplikasi ini memiliki dua komponen utama:
1. Backend (folder flask) — Flask API yang melakukan deployment model LSTM untuk prediksi tren dan model CNN untuk deteksi fashion dari gambar.
2. Mobile App (folder MyApplication) — Aplikasi Android berbasis Kotlin yang menampilkan hasil prediksi dan deteksi secara interaktif.

🚀 Fitur Utama
- Prediksi tren hingga 60 hari ke depan menggunakan model LSTM.
- Deteksi fashion dari gambar menggunakan model CNN.
- Visualisasi hasil prediksi dalam bentuk line chart menggunakan MPAndroidChart.
- Input jumlah hari (days) langsung dari aplikasi Android.
- Validasi input di backend.
- Endpoint health-check untuk memastikan API berjalan.

🏗️ Arsitektur Sistem
Struktur direktori proyek:
<p align="center">
<img width="450" alt="Arsitektur Proyek" src="https://github.com/user-attachments/assets/59940850-9fae-43f6-be99-69099c1532b7" />
</p>
