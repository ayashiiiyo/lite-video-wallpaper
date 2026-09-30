# Lite Video Wallpaper

Aplikasi Android Wallpaper Video Live yang sangat ringan, dirancang khusus untuk menghemat RAM dan baterai pada perangkat Android 32-bit maupun 64-bit.

---

## Fitur Utama

- **Sangat Hemat RAM**: Menggunakan framework native `android.media.MediaPlayer` dengan akselerasi perangkat keras bawaan ponsel (Stagefright/MediaCodec). Tidak menggunakan library pihak ketiga yang berat (tanpa ExoPlayer, Unity, atau ffmpeg), sehingga pemakaian memori tetap minim (hanya sekitar 15 - 25 MB RAM).
- **Nol Baterai di Latar Belakang**: Siklus hidup rendering wallpaper dikontrol ketat melalui `onVisibilityChanged`. Saat membuka aplikasi lain atau saat layar mati, proses pemutaran video dihentikan total (0% CPU & GPU).
- **Mendukung Layar Utama dan Layar Kunci**: Kompatibel dengan selektor Live Wallpaper Android 7.0+ (API 24+) untuk diterapkan pada layar utama saja atau sekaligus dengan layar kunci.
- **Kompatibilitas Universal 32-bit & 64-bit**: Kode murni Java/Android SDK tanpa dependensi library C++ JNI khusus, sehingga langsung kompatibel dengan arsitektur CPU 32-bit (`armeabi-v7a`, `x86`) dan 64-bit (`arm64-v8a`, `x86_64`).
- **Penyimpanan Lokal Mandiri**: Berkas video yang dipilih disalin ke direktori berkas privat aplikasi (`getFilesDir()`), menjamin wallpaper tetap aktif setelah reboot tanpa memerlukan izin runtime storage berlebih.

---

## Panduan Penggunaan

1. **Pilih Video**: Buka aplikasi dan tekan tombol **Pilih Video dari Galeri**.
2. **Format Disarankan**:
   - Durasi: 4 sampai 9 detik (video di-loop terus menerus).
   - Format: MP4 (H.264 / AVC).
   - Resolusi: 720p (720x1280) atau sesuaikan dengan resolusi layar HP.
3. **Terapkan**: Tekan tombol **Terapkan Jadi Wallpaper**.
4. **Pilih Layar**: Pada jendela pratinjau sistem yang muncul, tekan **Terapkan / Setel Wallpaper**, kemudian pilih **Layar utama dan layar kunci** (atau **Layar utama** saja sesuai keinginan).

---

## Cara Unduh APK (GitHub Actions)

Aplikasi ini sudah dilengkapi alur kerja otomatis (CI/CD) via GitHub Actions:

1. Buka tab **Actions** di repositori GitHub ini: [GitHub Actions Workflow](https://github.com/ayashiiiyo/lite-video-wallpaper/actions).
2. Pilih workflow **Build Android APK** terbaru.
3. Di bagian bawah (Artifacts), unduh berkas **LiteVideoWallpaper-debug.zip**.
4. Ekstrak dan pasang berkas APK di HP Anda.

---

## Struktur Proyek

```
lite-video-wallpaper/
├── .github/workflows/
│   └── build-apk.yml           # Alur kerja otomatis build APK di GitHub
├── app/
│   ├── src/main/
│   │   ├── java/com/ayashii/wallpaper/
│   │   │   ├── MainActivity.java           # Antarmuka pemilih video dan status
│   │   │   └── VideoWallpaperService.java   # WallpaperService engine pemutar video
│   │   ├── res/layout/activity_main.xml    # Desain UI minimalis & responsif
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

---

## Lisensi

Proyek ini dibuat untuk penggunaan pribadi dan sumber terbuka.
