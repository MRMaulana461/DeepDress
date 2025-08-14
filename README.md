📈 LSTM Time Series Prediction App
Aplikasi ini memiliki dua komponen utama:

Backend (folder flask) — Flask API yang melakukan deployment model LSTM untuk prediksi tren dan model CNN untuk deteksi fashion dari gambar.

Mobile App (folder MyApplication) — Aplikasi Android berbasis Kotlin yang menampilkan hasil prediksi dan deteksi secara interaktif.

🚀 Fitur Utama
Prediksi tren hingga 60 hari ke depan menggunakan model LSTM.

Deteksi fashion dari gambar menggunakan model CNN.

Visualisasi hasil prediksi dalam bentuk line chart menggunakan MPAndroidChart.

Input jumlah hari (days) langsung dari aplikasi Android.

Validasi input di backend.

Endpoint health-check untuk memastikan API berjalan.

🏗️ Arsitektur Sistem
Struktur direktori proyek:

Fashion-AI/
│
├── flask/               # Backend Flask + Model CNN & LSTM
│   ├── app.py            # Entry point Flask API
│   ├── requirements.txt  # Dependencies backend
│   ├── models/           # Folder model
│   │   ├── cnn_model.h5  # Model CNN
│   │   └── lstm_model.h5 # Model LSTM
│   └── ...
│
├── MyApplication/        # Proyek Android Studio (Kotlin)
│   ├── app/              # Source code Android
│   ├── build.gradle
│   └── ...
│
└── README.md             # Dokumentasi proyek
