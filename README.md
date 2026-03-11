# Fastt (F) — TikTok Downloader (Android)

**Qué hace**
- Pegas un link de TikTok
- La app consulta: `https://api.ananta.qzz.io/api/v2/tiktok?url=...` con header `x-api-key`
- Te muestra metadata + miniatura
- Descarga el `videoUrl` usando `DownloadManager` al folder **Downloads**

## Build (local)
- Requiere Android Studio (o Gradle + Android SDK)
- Task: `assembleDebug`

## Build (GitHub Actions)
- El workflow está en `.github/workflows/android-debug-apk.yml`
- Se ejecuta en push a `main` o manual con `workflow_dispatch`
- Descarga el APK desde **Actions → run → Artifacts → fastt-debug-apk**
