# Immich Server

Cross-platform Immich-compatible photo backup server.

## Overview

This project implements a lightweight, Immich API-compatible server that runs on multiple platforms:
- **Android** (API 21+): Native APK with Compose UI
- **iOS**: Framework for integration with SwiftUI
- **JVM/Desktop**: Command-line minimal version

## Architecture

```
immich-server/
├── shared/              ← Core shared code (Kotlin Multiplatform)
│   ├── api/             ← Ktor REST API routes
│   ├── model/           ← Data models (kotlinx.serialization)
│   ├── service/         ← Business logic
│   ├── db/              ← SQLDelight database
│   └── platform/        ← Platform abstractions (expect/actual)
│
├── androidApp/          ← Android application
│   ├── Compose UI
│   └── Foreground Service
│
├── iosApp/              ← iOS application (SwiftUI)
│
└── minimal/             ← Command-line JVM version
```

## Features

- **API Compatible**: Implements core Immich REST API
- **Cross-Platform**: Share 90%+ code across platforms
- **SQLite Database**: SQLDelight for type-safe SQL
- **File Storage**: Platform-specific file management
- **Background Service**: Keep server running

## Implemented APIs

### Server Info
- `GET /api/server-info`
- `GET /api/server-info/ping`
- `GET /api/server-info/version`

### Authentication
- `POST /api/auth/login`
- `POST /api/auth/admin-sign-up`

### Assets (TODO)
- `POST /api/assets` - Upload
- `GET /api/assets` - List
- `GET /api/assets/:id` - Download

### Albums (TODO)
- `GET /api/albums`
- `POST /api/albums`

## Building

### Android
```bash
./gradlew :androidApp:assembleDebug
```

### JVM (Minimal)
```bash
./gradlew :minimal:run
```

### iOS Framework
```bash
./gradlew :shared:linkDebugFrameworkIosArm64
```

## Usage

### Android
1. Install APK
2. Open app, tap "Start Server"
3. Connect with Immich app using displayed URL

### JVM
```bash
./gradlew :minimal:run
# Server starts on http://localhost:2283
```

## Development

### Adding a New Platform
1. Create new module (e.g., `desktop/`)
2. Implement `PlatformFileStorage`, `PlatformNotification`, `PlatformDatabaseDriverFactory`
3. Add platform-specific UI

### Adding an API Endpoint
1. Add route in `shared/src/commonMain/api/`
2. Add service method in `shared/src/commonMain/service/`
3. Update database schema if needed

## License

AGPL-3.0
