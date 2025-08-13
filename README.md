# 📈 LSTM Time Series Prediction App

Aplikasi Android untuk memprediksi data time series hingga 60 hari ke depan menggunakan model **LSTM** yang di-deploy di backend **Flask API**.  
Frontend Android dibangun menggunakan **Kotlin** dan menampilkan hasil prediksi dalam bentuk **line chart** menggunakan **MPAndroidChart**.

---

## 🚀 Fitur Utama
- Prediksi maksimal 60 hari ke depan berdasarkan data historis.
- Backend Flask dengan model LSTM (`world_best_model2.h5`).
- Visualisasi hasil prediksi dalam grafik.
- Input jumlah hari (`days`) langsung dari aplikasi Android.
- Validasi input pada backend.
- Endpoint health-check untuk memastikan API berjalan.

---

## 🏗️ Arsitektur Sistem
Fashion-AI/
│
├── flask/            -> Backend Flask + CNN & LSTM
│   ├── app.py
│   ├── requirements.txt
│   ├── models/
│   │   ├── cnn_model.h5
│   │   └── lstm_model.h5
│   └── ...
│
├── MyApplication/    -> Proyek Android Studio (Kotlin)
│   ├── app/
│   ├── build.gradle
│   └── ...
│
└── README.md         -> Dokumentasi proyek
