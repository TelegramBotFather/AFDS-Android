# AFDS Android

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.0.5-blue.svg)](https://github.com/CloudflareHackers/AFDS-Android/releases/latest)
[![Min SDK](https://img.shields.io/badge/minSdk-26-green.svg)]()

**AFDS (Advanced File Discovery System)** — Android client for searching, browsing, and managing files from a massive media archive. Files are delivered directly to your personal Telegram channel via a bot helper system.

---

## Features

- **Authentication** — Email + OTP login; Google Sign-In support
- **Search** — Full-text search across Media, Music, NSFW, and Mix Media categories
- **Browse** — Paginated file browsing by category
- **File Actions** — Download, copy link, send to Telegram channel, save to My Files
- **Telegram Channel Delivery** — Send files directly to your Telegram channel via bot
- **Remove Files** — Remove saved files from My Files
- **1DM Integration** — Download with 1DM, 1DM+, or 1DM Lite (configurable)
- **File Details** — View file name, size, MIME type, and caption
- **Profile Management** — Telegram ID, channel ID, downloader preference
- **Daily Usage Limits** — See your remaining downloads and sends for today (100 downloads / 50 sends per day)
- **Auto-Update** — Checks GitHub Releases on launch, downloads and installs APK in-app
- **Auto Telegram Setup** — WebView-guided channel creation and bot setup
- **Offline Detection** — Real-time connectivity monitoring with offline screen
- **60s Cache** — Search/browse results cached for 60 seconds to reduce API calls
- **Material 3** — Dark theme with purple gradient matching AFDS branding

---

## Screenshots

_Coming soon_

---

## Requirements

- Android 8.0+ (API 26)
- Telegram account with a personal channel
- AFDS account (register via the app)

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Networking | Ktor Client (OkHttp engine) |
| Serialization | kotlinx.serialization |
| Storage | DataStore Preferences |
| Navigation | Jetpack Navigation Compose |
| Auth | CredentialManager (Google Sign-In) |
| WebView | WebViewAssetLoader (Telegram setup) |

---

## Project Structure

```
app/src/main/java/com/afds/app/
├── AFDSApplication.kt              # App singleton — ApiClient + SessionManager
├── MainActivity.kt                 # Entry point — network monitor, update check
├── data/
│   ├── local/
│   │   ├── SessionManager.kt       # DataStore auth token + prefs (28-day expiry)
│   │   └── CacheManager.kt         # 60s in-memory cache
│   ├── model/
│   │   ├── Models.kt               # All data classes + FileCategory enum
│   │   └── UpdateModels.kt         # GitHub release + update models
│   └── remote/
│       └── ApiClient.kt            # All API calls
├── ui/
│   ├── components/
│   │   └── SharedComponents.kt     # FileCard, FileDetailDialog, pagination
│   ├── navigation/
│   │   └── Navigation.kt           # Routes & NavHost
│   ├── screens/
│   │   ├── LoginScreen.kt          # Email + OTP login
│   │   ├── GoogleLoginScreen.kt    # Google Sign-In
│   │   ├── SetupScreen.kt          # 4-step Telegram setup wizard
│   │   ├── TelegramSetupScreen.kt  # Automated WebView channel setup
│   │   ├── HomeScreen.kt           # Search hub + category cards
│   │   ├── SearchScreen.kt         # Paginated search results
│   │   ├── BrowseScreen.kt         # Category file browsing
│   │   ├── ProfileScreen.kt        # Account, Telegram, usage, updates
│   │   └── MyFilesScreen.kt        # Saved files with remove support
│   └── theme/
│       └── Theme.kt                # Material 3 color scheme
└── util/
    ├── DownloadHelper.kt           # 1DM / Android DownloadManager
    ├── NetworkObserver.kt          # Real-time connectivity Flow
    ├── UpdateManager.kt            # Semver check, APK download & install
    └── Utils.kt                    # normalizeEmail, helpers
```

---

## Telegram Setup

### Manual Setup
1. Create a Telegram channel (private or public)
2. Add **@TGID1OO1Bot** as admin
3. Add **@LinkerXHelperbot** as admin, then run `/setup` in the channel
4. Get your numeric Telegram User ID from **@userinfobot**
5. Enter your User ID and Channel ID (e.g. `-1001234567890`) in the app

### Auto Setup
Use **Profile → Auto Telegram Setup** for a guided WebView flow that handles bot setup automatically.

> All listed bots must have admin permissions in your channel or file delivery will fail.

---

## Download Integration

In **Profile → Download App**, select your preferred downloader:

| Option | Package |
|--------|---------|
| Built-in (Default) | Android DownloadManager |
| 1DM | `idm.internet.download.manager` |
| 1DM+ | `idm.internet.download.manager.plus` |
| 1DM Lite | `idm.internet.download.manager.lite` |

---

## API

**Base URL:** `https://tga-hd.api.hashhackers.com`

| Endpoint | Auth | Description |
|----------|------|-------------|
| `POST /request-login-otp` | No | Request OTP |
| `POST /verify-login-otp` | No | Verify OTP → token |
| `POST /auth/google` | No | Google Sign-In |
| `GET /profile` | Bearer | Get profile + daily usage |
| `POST /profile/set-user-id` | Bearer | Set Telegram user ID |
| `PUT /profile/update-user-id` | Bearer | Update Telegram user ID |
| `POST /profile/set-channel-id` | Bearer | Set channel ID |
| `DELETE /profile/remove-channel-id` | Bearer | Remove channel ID |
| `GET /{category}/search?q=&page=` | Bearer | Search files |
| `GET /{category}/index?page=` | Bearer | Browse category |
| `GET /{category}/id?id=` | No | File details |
| `GET /genLink?type=&id=` | No | Generate download link |
| `POST /user/save-file` | Bearer | Save to My Files |
| `GET /user/my-files?page=` | Bearer | Get saved files |
| `DELETE /user/remove-file` | Bearer | Remove from My Files |
| `POST /sendToChannel` | Bearer | Send file to Telegram channel |

---

## Building

**Requirements:** Android Studio Ladybug or newer, JDK 17

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires keystore)
./gradlew assembleRelease
```

Release signing uses environment variables — no secrets in source:

```
KEYSTORE_PASSWORD=...
KEY_ALIAS=...
KEY_PASSWORD=...
```

CI builds are handled by `.github/workflows/build.yml`.

---

## Version

**Current:** 1.0.5 (versionCode 6)
**Package:** `com.afds.app`
**Releases:** [GitHub Releases](https://github.com/CloudflareHackers/AFDS-Android/releases)

---

## License

MIT — see [LICENSE](LICENSE)
