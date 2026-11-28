# My Burung 🐦

Game 2D Android Native bergaya **Flappy Bird** yang ringan, responsif, dan berjalan mulus di **60fps/120fps** menggunakan **Kotlin**, **Jetpack Compose**, **Canvas**, **Coroutines**, dan **Android Photo Picker (`PickVisualMedia`) / Storage Access Framework (SAF)** untuk kustomisasi foto karakter burung dan background.

## Fitur Utama

### 1. Kustomisasi Foto Karakter Burung & Background
- **Karakter Burung:**
  - **Default:** Grafis burung kartun kuning bulat dengan sayap dan paruh animasi.
  - **Custom Foto User:** Tombol **Pilih Foto Burung** di Menu Utama menggunakan `ActivityResultContracts.PickVisualMedia`. Foto otomatis dipotong (*crop*) menjadi lingkaran dengan outline tipis serta dilengkapi **sayap kecil dan paruh animasi** di bagian luarnya.
- **Background:**
  - **Default:** Parallax pegunungan berlapis dan langit bergaya flat/pixel art 2D dengan awan bergeser + siklus Siang/Senja/Malam.
  - **Custom Foto User:** Tombol **Pilih Background** di Menu Utama untuk memilih foto dari galeri, otomatis di-resize/kompres di `Dispatchers.IO`, dipotong proporsional, dan di-*scroll*/*loop* secara horizontal ke kiri tanpa lag.
- **Save & Load Otomatis:**
  - Menyimpan URI foto pilihan ke `SharedPreferences` (dengan `takePersistableUriPermission` + cache lokal terkompresi) agar karakter & background otomatis dimuat kembali saat aplikasi dibuka.

### 2. Gameplay Loop & Menu Game
- **Menu Utama:** Logo pop **MyBird**, tampilan **High Score**, tombol **Mulai Game**, kartu **Ubah Karakter** (dengan preview bulat), dan kartu **Ubah Background** (dengan preview thumbnail).
- **Layar Gameplay:** Skor besar di tengah-atas, tombol pause & suara di pojok atas, serta kontrol tap instan (`awaitFirstDown`) di seluruh layar.
- **Deteksi Tabrakan Presisi:** *Circle-vs-AABB* antara lingkaran burung (termasuk foto custom) dengan pipa atas/bawah, batas atas layar, dan tanah.
- **Game Over Pop-up:** Dialog bergaya kartun menampilkan skor akhir, High Score, medali, tombol **Main Lagi**, dan tombol **Kembali ke Menu Utama**.

### 3. Performa 60fps/120fps & Anti-OOM
- Loop game berbasis `LaunchedEffect` + `withFrameNanos` (*Choreographer/VSync*) dengan *delta time* dan *object pooling* (pipa, partikel, popup skor).
- Downsampling (`inJustDecodeBounds` + `inSampleSize`), koreksi EXIF, serta kompresi gambar berjalan sepenuhnya di `Dispatchers.IO` untuk menghindari *OutOfMemoryError*.
- Efek suara arcade (*flap*, *score*, *hit*) disintesis secara prosedural ke `SoundPool`.

## Spesifikasi Teknis & Build

- **JDK:** 17 (Temurin)
- **SDK:** `compileSdk = 35`, `minSdk = 26`, `targetSdk = 35`
- **CI/CD:** GitHub Actions workflow di `.github/workflows/build-apk.yml` otomatis menjalankan unit test, membangun Debug APK, dan mengunggah artefak APK.

### Perintah Build Lokal
```bash
chmod +x gradlew
./gradlew testDebugUnitTest
./gradlew assembleDebug
```
APK Debug dihasilkan di `app/build/outputs/apk/debug/app-debug.apk`.
