# KlarFinance Android App

Aplikasi Android untuk nasabah — pengajuan pinjaman & Pay Later berbasis QRIS, KYC (foto
KTP + selfie), tracking status pengajuan, dan pembayaran.

- **Package**: `com.klarfinance.app`
- **minSdk 26 / targetSdk 35 / compileSdk 35**

## Tech Stack

- **Kotlin**, **Jetpack Compose** (Material 3), **Hilt** (DI), **Room** (local DB)
- **OkHttp** + interceptor kustom (`HmacInterceptor`) untuk request signing
- **Coil** untuk image loading, **KSP**, Kotlin Serialization
- Firebase (`google-services`) untuk push notification (FCM)
- Struktur clean-ish: `core` / `data` / `di` / `domain` / `presentation`

## Peran dalam sistem

Satu dari tiga klien yang bicara ke [KlarFinance API](../backend/klarfinance/README.md) lewat
REST + JWT. Tiap request juga ditandatangani HMAC (`X-Timestamp` / `X-Signature` /
`X-Client-Type`, lihat `HmacInterceptor`) dengan secret key yang sama dengan
`application.security.jwt.secret-key` di backend.

## Getting Started

Prasyarat: JDK 21, Android SDK (compileSdk 35), akses ke backend (lokal atau remote).

```bash
cp .env.example .env   # lalu sesuaikan BASE_URL, API_KEY, SECRET_KEY, CLIENT_TYPE
./gradlew assembleDebug
```

`.env` (gitignored) dibaca oleh `build.gradle.kts` untuk generate
`res/xml/network_security_config.xml` (cleartext allowlist untuk `BASE_URL` dev/emulator) dan
untuk `BuildConfig` field HMAC. **Bukan** dibaca untuk signing config release (lihat di bawah).

- Emulator → host lokal: `BASE_URL=http://10.0.2.2:8080/`
- Device fisik di jaringan yang sama: `BASE_URL=http://<IP-LAN-mesin-dev>:8080/`


## Struktur

```
app/src/main/java/com/klarfinance/app/
  core/          # utilities (mis. ImageCompressor — compress foto ≤10MB sebelum upload)
  data/          # repository, network (Retrofit/OkHttp), local (Room)
  di/            # Hilt modules
  domain/        # use case / model domain
  presentation/  # Compose UI (screen, viewmodel, navigation)
```

## Catatan

- Foto KTP/selfie di-compress ke ≤10MB sebelum dikirim (`ImageCompressor`) — selaras dengan
  limit `spring.servlet.multipart.max-file-size=10MB` di backend.
- `SECRET_KEY` di `.env` sama persis dengan JWT secret backend dan `secretKey` di webadmin —
  satu shared secret dipakai lintas klien untuk HMAC signing.
