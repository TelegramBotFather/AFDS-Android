# AFDS Android

Android client for **AFDS (Advanced File Discovery System)** — search, browse, and manage files from a massive media archive.

## Features

- 🔐 **Authentication** — Email + OTP login with Telegram/Email delivery
- 🔍 **Search** — Search across Media, Music, NSFW, and Mix Media categories
- 📂 **Browse** — Paginated file browsing by category
- 📋 **File Actions** — Download links (copied to clipboard), Telegram bot integration, save to My Files
- ℹ️ **File Details** — View file name, size, MIME type, and caption
- 👤 **Profile Management** — Change password, email, Telegram ID, content preferences
- 📁 **My Files** — Access saved files collection
- 🔄 **Auto-Update** — Checks for new versions on launch, downloads and installs APK updates
- 🎨 **Material 3** — Modern dark/light theme with purple gradient branding

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
├── MainActivity.kt                 # Entry point with Compose setup
├── data/
│   ├── local/SessionManager.kt     # DataStore preferences (auth, settings)
│   ├── model/Models.kt             # API data models (FileItem, SearchResponse, etc.)
│   ├── model/UpdateModels.kt       # Auto-update model
│   └── remote/ApiClient.kt         # Ktor HTTP client for all API calls
├── ui/
│   ├── components/SharedComponents.kt  # FileCard, FileDetailDialog, pagination
│   ├── navigation/Navigation.kt        # Routes & NavHost
│   ├── screens/
│   │   ├── LoginScreen.kt          # Email + OTP authentication
│   │   ├── HomeScreen.kt           # Search, browse, update check
│   │   ├── SearchScreen.kt         # Search results with pagination
│   │   ├── BrowseScreen.kt         # Category file browsing
│   │   ├── ProfileScreen.kt        # Account & content settings
│   │   └── MyFilesScreen.kt        # Saved files list
│   └── theme/Theme.kt              # Material 3 color scheme
└── util/
    ├── Utils.kt                    # formatBytes, normalizeEmail
    └── UpdateManager.kt            # Version check, APK download & install
```

## API Integration

**Base URL**: `https://tga-hd.api.hashhackers.com`

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
| Save File | `POST /user/save-file` |
| My Files | `GET /my-files/index?page=` |

## Auto-Update System

**Update endpoint**: `https://afds.apks.zindex.eu.org/com.afds.app/app.json`

```json
{
  "version": "1.0.0",
  "version_code": 1,
  "changelog": "Initial release",
  "force_update": false
}
```

**APK download**: `https://afds.apks.zindex.eu.org/com.afds.app/{version}.apk`

The app compares its built-in `versionCode` with the remote `version_code`. If the remote is higher, an update dialog is shown. Set `force_update: true` for mandatory updates.

## Building

1. Open in Android Studio
2. Sync Gradle
3. Run on device/emulator (API 26+)

**Release build:**
```bash
./gradlew assembleRelease
```

## Version

- **Current**: 1.0.0 (versionCode 1)
- **Package**: `com.afds.app`

## License

Private — CloudflareHackers