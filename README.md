# AFDS Android

Android client for **AFDS (Advanced File Discovery System)** — search, browse, and manage files from a massive media archive with direct Telegram channel delivery.

## Features

- 🔐 **Authentication** — Email + OTP login with Telegram/Email delivery
- 🔍 **Search** — Search across Media, Music, and Mix Media categories
- 📂 **Browse** — Paginated file browsing by category
- 📋 **File Actions** — Download, copy link, send to Telegram channel, save to My Files
- 📡 **Telegram Channel Delivery** — Send files directly to your Telegram channel via @LinkerXHelperbot
- 📥 **1DM Integration** — Download with 1DM, 1DM+, or 1DM Lite (configurable per device)
- ℹ️ **File Details** — View file name, size, MIME type, and caption
- 👤 **Profile Management** — Change password, email, Telegram ID, channel ID, downloader app
- 📁 **My Files** — Access saved files collection
- 🔄 **Auto-Update** — Checks for new versions on launch, downloads and installs APK updates
- 📶 **No Internet Detection** — Real-time connectivity monitoring with offline screen
- ⚡ **60s Cache** — Search/browse results cached for 60 seconds to reduce API calls
- 🎨 **Material 3** — Purple gradient theme matching AFDS website branding
- 🛠️ **Forced Setup** — New users must configure Telegram User ID + Channel ID before using the app

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Networking**: Ktor Client (OkHttp engine)
- **Serialization**: kotlinx.serialization
- **Storage**: DataStore Preferences
- **Navigation**: Jetpack Navigation Compose
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35

## Project Structure

```
app/src/main/java/com/afds/app/
├── AFDSApplication.kt              # App singleton with ApiClient & SessionManager
├── MainActivity.kt                 # Entry point, no-internet detection
├── data/
│   ├── local/
│   │   ├── CacheManager.kt         # 60-second in-memory cache
│   │   └── SessionManager.kt       # DataStore preferences (auth, settings, profile)
│   ├── model/
│   │   ├── Models.kt               # API data models
│   │   └── UpdateModels.kt         # Auto-update model
│   └── remote/
│       └── ApiClient.kt            # Ktor HTTP client for all API calls
├── ui/
│   ├── components/
│   │   └── SharedComponents.kt     # FileCard, FileDetailDialog, pagination, loading
│   ├── navigation/
│   │   └── Navigation.kt           # Routes & NavHost (LOGIN→SETUP→HOME)
│   ├── screens/
│   │   ├── LoginScreen.kt          # Email + OTP authentication
│   │   ├── SetupScreen.kt          # 4-step setup wizard (User ID + Channel)
│   │   ├── HomeScreen.kt           # Search, browse, update check, refresh
│   │   ├── SearchScreen.kt         # Search results with pagination
│   │   ├── BrowseScreen.kt         # Category file browsing
│   │   ├── ProfileScreen.kt        # Account, Telegram, channel, downloader, preferences
│   │   └── MyFilesScreen.kt        # Saved files list
│   └── theme/
│       └── Theme.kt                # Material 3 color scheme
└── util/
    ├── DownloadHelper.kt            # 1DM / built-in download manager
    ├── NetworkObserver.kt           # Real-time connectivity monitoring
    ├── UpdateManager.kt             # Version check, APK download & install
    └── Utils.kt                     # formatBytes, normalizeEmail
```

## API Integration

**Main API**: `https://tga-hd.api.hashhackers.com`
**File Delivery**: `https://tgarchiveapifilecopyandlinkgen.hashhackersapi.workers.dev`

| Feature | Endpoint |
|---------|----------|
| Request OTP | `POST /request-login-otp` |
| Verify OTP | `POST /verify-login-otp` |
| Email OTP | `POST /request-login-otp-email` |
| Search | `GET /{category}/search?q=&page=` |
| Browse | `GET /{category}/index?page=` |
| File Details | `GET /{category}/id?id=` |
| Download Link | `GET /genLink?type=&id=` |
| Profile | `GET /profile` |
| Change Password | `POST /profile/change-password` |
| Change Email | `POST /profile/change-email` |
| Set Telegram ID | `POST /profile/set-user-id` |
| Update Telegram ID | `PUT /profile/update-user-id` |
| Set Channel ID | `POST /profile/set-channel-id` |
| Remove Channel ID | `DELETE /profile/remove-channel-id` |
| Save File | `POST /user/save-file` |
| My Files | `GET /user/my-files?page=` |
| Send to Channel | `POST /sendToChannel` (File Delivery API) |

## Telegram Channel Setup

1. Add **@LinkerXHelperbot** to your channel as **admin** with full permissions
2. Run the **/setup** command in your channel
3. Enter your Channel ID in the app (e.g., `-1001234567890`)

## 1DM Download Integration

In **Profile → Download App**, select your preferred downloader:
- **Built-in (Default)** — Android DownloadManager
- **1DM** — `idm.internet.download.manager`
- **1DM+** — `idm.internet.download.manager.plus`
- **1DM Lite** — `idm.internet.download.manager.lite`

Setting is stored locally per device.

## Auto-Update System

**Update endpoint**: `https://afds.apks.zindex.eu.org/com.afds.app/app.json`
**APK download**: `https://afds.apks.zindex.eu.org/com.afds.app/{version}.apk`

## Building

1. Open in Android Studio
2. Sync Gradle
3. Run on device/emulator (API 26+)

**Release build:**
```bash
./gradlew assembleRelease
```

## Version

- **Current**: 1.0.4 (versionCode 5)
- **Package**: `com.afds.app`

## License

Private — CloudflareHackers