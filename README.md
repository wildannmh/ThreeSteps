# Three Steps - Aplikasi E-Commerce Sepatu 🛍️
<img width="1024" height="1024" alt="logo" src="https://github.com/user-attachments/assets/a7a056e7-ce94-4ed9-b72e-a13c60e16229" />

Selamat datang di **Three Steps**! 👋 Ini adalah aplikasi e-commerce berbasis Android yang dirancang untuk memberikan pengalaman jual beli yang nyaman dan efisien. Proyek ini dibangun dengan cinta menggunakan Kotlin dan Android Jetpack, dengan fokus pada antarmuka yang bersih dan fungsionalitas yang solid baik untuk pembeli maupun penjual. ✨

<br>

## Daftar Isi Readme
1. Fitur Utama
2. Tampilan Aplikasi
3. Teknologi yang Digunakan
4. Cara Instalasi
5. Anggota kelompok
6. Lisensi

## Fitur Utama 🎯

Aplikasi ini menyediakan serangkaian fitur lengkap untuk mendukung aktivitas jual beli.

### Fitur Umum & Pembeli
- 🔑 **Autentikasi User**: Proses pendaftaran dan login yang aman untuk pengguna.
- 🚀 **OnBoarding Page**: Halaman perkenalan saat pertama kali membuka aplikasi.
- 🏠 **Home Page**: Menampilkan produk-produk unggulan dan terbaru.
- 🔍 **Search & Filter**: Memudahkan pengguna mencari produk berdasarkan kata kunci dan filter tertentu.
- 📄 **Product Details**: Halaman detail yang menampilkan informasi lengkap, deskripsi, dan gambar produk.
- 🛒 **Cart & Checkout**: Alur keranjang belanja dan proses checkout yang intuitif.
- ❤️ **Favorite List**: Fitur untuk menyimpan produk-produk yang disukai oleh pengguna.
- 👤 **Profile Management**: Pengguna dapat mengelola data profil dan melihat riwayat transaksi.

### Fitur Mode Penjual (Seller Mode)
- 📊 **Dashboard Produk**: Antarmuka khusus bagi seller untuk melihat dan mengelola semua produk mereka.
- 📦 **Manajemen Produk**: Kemampuan untuk menambah, mengedit, dan menghapus produk.
- 🧾 **Manajemen Order**: Melacak pesanan masuk dengan status seperti `Awaiting Payment`, `Paid`, dan `Expired`.

## Teknologi yang Digunakan 🛠️

Proyek ini dibangun dengan menggunakan teknologi modern untuk pengembangan aplikasi Android.

- 🤖 **Bahasa Pemrograman**: **Kotlin**
- 🚀 **Arsitektur**: Menggunakan komponen **Android Jetpack**
  - **Fragment**: Untuk membangun UI yang modular.
  - **ViewModel**: Untuk mengelola data terkait UI secara lifecycle-aware.
  - **RecyclerView**: Untuk menampilkan daftar data yang efisien.
- 🔥 **Database**: **Firebase** (untuk mengelola data produk, pesanan, dan pengguna).
- ☁️ **Storage**: **Cloudinary** (untuk hosting dan manajemen gambar produk).
- 🐙 **Version Control**: **Git & GitHub**.

## Cara Instalasi ⚙️

Ingin mencoba menjalankan proyek ini? Ikuti langkah-langkah berikut:

1.  **Clone repository ini:** 📂
    ```bash
    git clone [https://github.com/wildannmh/ThreeSteps.git]
    ```

2.  **Masuk ke direktori proyek:**
    ```bash
    cd ThreeSteps
    ```

3.  **Buka proyek menggunakan Android Studio.** 💻

4.  **Konfigurasi Firebase:** 🔥
    - Buat proyek baru di [Firebase Console](https://console.firebase.google.com/).
    - Tambahkan aplikasi Android ke proyek Firebase Anda dan ikuti petunjuk untuk menambahkan file `google-services.json` ke dalam direktori `app` proyek Anda.

5.  **Konfigurasi Cloudinary:** ☁️
    - Jika diperlukan, tambahkan kredensial API Cloudinary Anda ke dalam file `local.properties` atau di dalam kode sesuai dengan implementasi.

6.  **Build dan Jalankan Aplikasi:** ▶️
    - Tunggu hingga Gradle selesai melakukan sinkronisasi.
    - Jalankan aplikasi pada emulator atau perangkat fisik melalui Android Studio. Selamat mencoba! 🎉

## Manual Book 🔍
<img width="12128" height="14512" alt="Manual Book" src="https://github.com/user-attachments/assets/f33ef9b4-563c-4a1d-8613-b7f32d4154a8" />

## Anggota Kelompok 👥

Proyek ini adalah hasil kerja keras dari **Kelompok 11 Kelas A**:

| No| Nama | NIM | Peran |
| :---: | --- | --- | --- |
| 1| **Revalina Fidiya Anugrah** | `H1D023011` | 🎨 UI/UX Designer |
| 2| **Prima Dzaky Hibatulloh** | `H1D023040` | 💻 Programmer |
| 3| **Wildan Munawwar Habib** | `H1D023045` | 💻 Programmer |

## Lisensi 📜

Proyek ini dilisensikan di bawah [Lisensi MIT](LICENSE).
