# Immich Server

Cross-platform Immich-compatible photo backup server.

## Overview

This project implements a lightweight, Immich API-compatible server that runs on multiple platforms:
- **Android** (API 16+): Native APK with two UI variants
  - **Modern** (API 21+): Jetpack Compose UI
  - **Legacy** (API 16+): Traditional XML UI
- **iOS**: Framework for integration with SwiftUI
- **JVM/Desktop**: Command-line minimal version

## Architecture

```
immich-server/
├── shared/              ← Core shared code (Kotlin Multiplatform)
│   ├── api/             ← Ktor REST API routes (CIO engine, no native deps)
│   ├── model/           ← Data models (kotlinx.serialization)
│   ├── service/         ← Business logic
│   ├── db/              ← SQLDelight database (cross-platform SQLite)
│   ├── discovery/       ← UDP discovery protocol (v1.0 - v3.0)
│   └── platform/        ← Platform abstractions (expect/actual)
│
├── androidApp/          ← Android application
│   ├── modern/          ← Compose UI (API 21+, Android 5.0+)
│   └── legacy/          ← XML UI (API 16+, Android 4.1+)
│
├── iosApp/              ← iOS application (SwiftUI)
│
├── minimal/             ← Command-line JVM version
│
└── docs/                ← Documentation
│   ├── discovery-protocol.md  ← UDP discovery protocol spec
│   └── api-spec.md            ← API specification (TODO)
```

## Features

- **API Compatible**: Implements core Immich REST API
- **Cross-Platform**: Share 90%+ code across platforms
- **Ultra-Low Android**: Supports Android 4.1 (API 16) via Ktor CIO engine
- **SQLite Database**: SQLDelight for type-safe SQL
- **File Storage**: Platform-specific file management
- **Background Service**: Keep server running
- **Secure Discovery**: Token-based signing for secure server identification

## Android Build Variants

| Variant | Min SDK | UI | Engine | Output |
|---------|:-------:|:---:|:------:|--------|
| **modernDebug** | 21 (5.0) | Compose | Ktor CIO | `app-modern-debug.apk` |
| **legacyDebug** | 16 (4.1) | XML | Ktor CIO | `app-legacy-debug.apk` |

## Implemented APIs

### Server Info
- `GET /api/server-info`
- `GET /api/server-info/ping`
- `GET /api/server-info/version`

### Authentication
- `POST /api/auth/login` - User login (first user becomes admin)
- `POST /api/auth/logout` - User logout
- `GET /api/auth/admin-sign-up` - Check if admin exists
- `POST /api/auth/admin-sign-up` - Create admin user
- `POST /api/auth/token-exchange` - Exchange server token (v3.0 discovery)

### Assets (TODO)
- `POST /api/assets` - Upload
- `GET /api/assets` - List
- `GET /api/assets/:id` - Download

### Albums (TODO)
- `GET /api/albums`
- `POST /api/albums`

## Discovery Protocol

The server supports three versions of the UDP discovery protocol:

| Version | Features | Security Level |
|---------|----------|----------------|
| **v1.0** | Basic discovery | None |
| **v2.0** | Server ID matching | Basic |
| **v3.0** | Token-based signing | Advanced |

### Key Concepts

| Concept | Description |
|---------|-------------|
| **Server ID** | Unique UUID generated on first startup, stored in database |
| **Server Token** | 256-bit secret token for signing discovery responses |
| **Server Name** | User-configurable display name |

### Protocol Flow (v3.0)

```
1. Client sends: DISCOVER_IMMICH_SERVER:<clientId>:<challengeNonce>
2. Server responds with signed JSON:
   {
     "serverId": "...",
     "serverName": "...",
     "serverUrl": "...",
     "timestamp": ...,
     "challengeNonce": "...",
     "signature": "HMAC-SHA256(serverToken, data)"
   }
3. Client verifies signature using saved serverToken
```

See [docs/discovery-protocol.md](docs/discovery-protocol.md) for full specification.

## Server Configuration

### Database Schema

```sql
-- User accounts
CREATE TABLE user (
    id TEXT PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    is_admin INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);

-- Server configuration (v2.0+ discovery)
CREATE TABLE server_config (
    id INTEGER PRIMARY KEY,
    server_id TEXT NOT NULL UNIQUE,
    server_name TEXT NOT NULL DEFAULT 'Immich Android Server',
    server_token TEXT NOT NULL,  -- Encrypted, for v3.0 signing
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);

-- API keys for authentication
CREATE TABLE api_key (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    name TEXT NOT NULL,
    key TEXT NOT NULL UNIQUE,
    created_at INTEGER NOT NULL DEFAULT 0
);
```

### First Startup Behavior

1. Server generates unique `serverId` (UUID v4)
2. Server generates `serverToken` (256-bit random)
3. Both stored in `server_config` table
4. First user to login is automatically created as admin

## Building

### Android Modern (Compose UI, API 21+)
```bash
./gradlew :androidApp:assembleModernDebug
```

### Android Legacy (XML UI, API 16+)
```bash
./gradlew :androidApp:assembleLegacyDebug
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
1. Install APK (modern or legacy variant)
2. Open app, tap "Start Server"
3. Server displays URL and unique server ID
4. Connect with Immich client using displayed URL
5. Client saves server ID for future auto-discovery

### JVM
```bash
./gradlew :minimal:run
# Server starts on http://localhost:2283
```

### IP Address Changes

When server IP changes (e.g., home network DHCP):
1. Client tries saved URL, connection fails
2. Client sends discovery broadcast
3. Server responds with new IP and signed response
4. Client verifies signature, updates saved URL
5. Automatic reconnect without user intervention

## Security

### Discovery Security (v3.0)

| Threat | Mitigation |
|--------|------------|
| Server spoofing | HMAC signature verification |
| Replay attacks | Challenge nonce matching |
| MITM on UDP | Token never sent over UDP |
| Unauthorized discovery | Token only after user auth |

### Token Storage

- Server: Encrypted in SQLite database
- Client: Platform secure storage (Keychain/Keystore)

## Development

### Adding a New Platform
1. Create new module (e.g., `desktop/`)
2. Implement `PlatformFileStorage`, `PlatformNotification`, `PlatformDatabaseDriverFactory`
3. Add platform-specific UI

### Adding an API Endpoint
1. Add route in `shared/src/commonMain/api/`
2. Add service method in `shared/src/commonMain/service/`
3. Update database schema if needed

### Adding Discovery Features
1. Update `DiscoveryProtocol.kt` for new message format
2. Update `DiscoveryServer.kt` for new response generation
3. Update `docs/discovery-protocol.md` with new version spec

## Documentation

- [Discovery Protocol Specification](docs/discovery-protocol.md)
- API Specification (TODO: docs/api-spec.md)
- Client-Server Communication (TODO: docs/client-server-comm.md)

## License

AGPL-3.0