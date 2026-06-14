# Client-Server Communication

This document describes the communication flow between the Immich mobile client and the Immich Android Server.

## Overview

The client-server communication involves three main phases:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Communication Flow                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Phase 1: Discovery                                                      │
│  ┌─────────────┐                        ┌─────────────┐                │
│  │   Client    │ ──── UDP Broadcast ───>│   Server    │                │
│  │             │                        │             │                │
│  │             │ <─── Signed Response ──│             │                │
│  └─────────────┘                        └─────────────┘                │
│                                                                          │
│  Phase 2: Authentication                                                 │
│  ┌─────────────┐                        ┌─────────────┐                │
│  │   Client    │ ──── HTTPS POST ──────>│   Server    │                │
│  │             │     /auth/login        │             │                │
│  │             │                        │             │                │
│  │             │ <─── Access Token ─────│             │                │
│  │             │     + Server Token     │             │                │
│  └─────────────┘                        └─────────────┘                │
│                                                                          │
│  Phase 3: API Operations                                                 │
│  ┌─────────────┐                        ┌─────────────┐                │
│  │   Client    │ ──── HTTPS Request ───>│   Server    │                │
│  │             │     Authorization      │             │                │
│  │             │                        │             │                │
│  │             │ <─── JSON Response ────│             │                │
│  └─────────────┘                        └─────────────┘                │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Phase 1: Server Discovery

### Purpose

- Find server on local network
- Obtain server URL, ID, and security token
- Handle IP address changes automatically

### Protocol Versions

| Version | Use Case |
|---------|----------|
| **v1.0** | Basic discovery, no security |
| **v2.0** | Server ID matching for auto-reconnect |
| **v3.0** | Full security with token-based signing |

### v3.0 Discovery Flow (Recommended)

#### Step 1: Client Sends Discovery Request

```
UDP Broadcast to 255.255.255.255:2284
Payload: DISCOVER_IMMICH_SERVER:<clientId>:<challengeNonce>
```

Example:
```
DISCOVER_IMMICH_SERVER:client-550e8400:nonce-a1b2c3d4
```

#### Step 2: Server Responds

```
UDP Response from server-ip:2284
Payload: IMMICH_SERVER_RESPONSE:<JSON>
```

Example:
```json
{
  "serverId": "550e8400-e29b-41d4-a716-446655440000",
  "serverName": "My Home Server",
  "serverUrl": "http://192.168.1.4:2283/api",
  "version": "3.0.0",
  "timestamp": 1781052924,
  "challengeNonce": "nonce-a1b2c3d4",
  "signature": "a1b2c3d4e5f6..."
}
```

#### Step 3: Client Verifies

```dart
// 1. Check challenge nonce matches
if (response.challengeNonce != requestNonce) {
  reject("Nonce mismatch");
}

// 2. Check timestamp is fresh (±30 seconds)
if (abs(now - response.timestamp) > 30) {
  reject("Stale response");
}

// 3. Verify signature (if serverToken is saved)
if (savedServerToken != null) {
  expectedSig = HMAC-SHA256(savedServerToken, data);
  if (expectedSig != response.signature) {
    reject("Invalid signature");
  }
}

// 4. Accept response
accept(response);
```

### Auto-Reconnect Flow

When server IP changes:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Auto-Reconnect Flow                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  1. Client starts, has saved server info:                               │
│     - serverId: "550e8400..."                                            │
│     - serverUrl: "http://192.168.1.4:2283/api" (old IP)                  │
│     - serverToken: "..."                                                 │
│                                                                          │
│  2. Client tries to connect to saved URL                                │
│     ┌─────────────┐                        ┌─────────────┐              │
│     │   Client    │ ──── HTTP Request ────>│  (timeout)   │              │
│     │             │     (fails)            │              │              │
│     └─────────────┘                        └─────────────┘              │
│                                                                          │
│  3. Client sends discovery broadcast                                     │
│     ┌─────────────┐                        ┌─────────────┐              │
│     │   Client    │ ──── UDP Broadcast ───>│   Server    │              │
│     │             │                        │ (new IP)    │              │
│     │             │                        │             │              │
│     │             │ <─── Signed Response ──│             │              │
│     │             │     serverId match!    │             │              │
│     └─────────────┘                        └─────────────┘              │
│                                                                          │
│  4. Client verifies signature, updates URL                              │
│     - serverUrl: "http://192.168.1.8:2283/api" (new IP)                  │
│                                                                          │
│  5. Client connects successfully                                        │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Phase 2: Authentication

### Login Flow

#### Request

```
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "user-password"
}
```

#### Response (Success)

```json
{
  "accessToken": "token_userId_timestamp",
  "userId": "17813089452298",
  "userEmail": "user@example.com",
  "name": "User Name",
  "isAdmin": true,
  "isOnboarded": true,
  "profileImagePath": "",
  "shouldChangePassword": false
}
```

#### Response (Error)

```json
{
  "message": "Invalid credentials"
}
```

Status: 401 Unauthorized

### First User Behavior

When no users exist in database:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    First User Auto-Admin                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  1. Client sends login request                                          │
│                                                                          │
│  2. Server checks database                                              │
│     - SELECT COUNT(*) FROM user                                         │
│     - Result: 0 (no users)                                              │
│                                                                          │
│  3. Server creates new user                                             │
│     - INSERT INTO user (id, email, name, password_hash, is_admin)       │
│     - is_admin = 1 (admin)                                              │
│                                                                          │
│  4. Server returns login response                                       │
│     - isAdmin: true                                                     │
│     - accessToken: "..."                                                │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### Token Exchange (v3.0 Discovery)

After successful login, client obtains server token for secure discovery:

#### Request

```
POST /api/auth/token-exchange
Authorization: Bearer <accessToken>

{
  "clientId": "client-550e8400"
}
```

#### Response

```json
{
  "serverToken": "base64-encoded-256-bit-token",
  "serverId": "550e8400-e29b-41d4-a716-446655440000",
  "expiresAt": null
}
```

#### Client Storage

```dart
// Store in platform secure storage
await secureStorage.write(
  key: 'server_token_${serverId}',
  value: serverToken
);

await secureStorage.write(
  key: 'saved_server',
  value: jsonEncode({
    'serverId': serverId,
    'serverName': serverName,
    'serverUrl': serverUrl,
    'lastConnected': DateTime.now().toIso8601String()
  })
);
```

---

## Phase 3: API Operations

### Authorization

All API requests (except public endpoints) require authorization:

```
Authorization: Bearer <accessToken>
```

Or using API key:

```
x-api-key: <apiKey>
```

### Server Info Endpoints

#### Get Server Version

```
GET /api/server-info/version

Response:
{
  "major": 3,
  "minor": 0,
  "patch": 0
}
```

#### Ping Server

```
GET /api/server-info/ping

Response:
{
  "res": "pong"
}
```

#### Get Server Features

```
GET /api/server-info/features

Response:
{
  "passwordLogin": true,
  "oauthEnabled": false,
  "configFile": false,
  ...
}
```

#### Get Server Config

```
GET /api/server-info/config

Response:
{
  "oauthButtonText": "",
  "loginPageMessage": "",
  ...
}
```

### User Endpoints

#### Get Current User

```
GET /api/users/me
Authorization: Bearer <accessToken>

Response:
{
  "id": "17813089452298",
  "email": "user@example.com",
  "name": "User Name",
  "profileImagePath": "",
  "isAdmin": true
}
```

---

## Data Models

### Server Configuration (Server Side)

```kotlin
@Serializable
data class ServerConfig(
    val serverId: String,       // UUID v4
    val serverName: String,     // User configurable
    val serverToken: String,    // 256-bit, encrypted
    val createdAt: Long,
    val updatedAt: Long
)
```

### Saved Server (Client Side)

```dart
class SavedServer {
  final String serverId;       // UUID v4
  final String serverName;     // Display name
  final String serverUrl;      // Last known URL
  final String? serverToken;   // Secure token (v3.0)
  final DateTime lastConnected;
}
```

### Login Response

```kotlin
@Serializable
data class LoginResponse(
    val accessToken: String,
    val userId: String,
    val userEmail: String,
    val name: String,
    val isAdmin: Boolean,
    val isOnboarded: Boolean,
    val profileImagePath: String,
    val shouldChangePassword: Boolean
)
```

---

## Error Handling

### HTTP Status Codes

| Code | Meaning | Client Action |
|------|---------|---------------|
| 200 | Success | Process response |
| 400 | Bad Request | Check request format |
| 401 | Unauthorized | Re-login or get new token |
| 403 | Forbidden | Check permissions |
| 404 | Not Found | Check endpoint path |
| 500 | Server Error | Show error, retry later |

### Error Response Format

```json
{
  "message": "Error description",
  "code": "ERROR_CODE",
  "details": {}
}
```

### Network Errors

| Error | Client Action |
|-------|---------------|
| Connection timeout | Try discovery to find new IP |
| Connection refused | Server may be offline, retry |
| SSL error | Check certificate, warn user |
| DNS failure | Use discovery fallback |

---

## Security Considerations

### Token Security

| Token | Storage | Transmission |
|-------|---------|--------------|
| `accessToken` | Memory + Secure Storage | HTTPS header |
| `serverToken` | Secure Storage only | HTTPS (once) |
| `apiKey` | Secure Storage | HTTPS header |

### HTTPS Requirements

- All API calls must use HTTPS
- Discovery (UDP) is signed, not encrypted
- Token exchange must use HTTPS

### Token Rotation

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Token Rotation                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  accessToken:                                                            │
│  - Short-lived (configurable, default 7 days)                           │
│  - Refresh via /api/auth/refresh-token                                   │
│  - Invalidated on logout                                                 │
│                                                                          │
│  serverToken:                                                            │
│  - Long-lived (no expiration)                                           │
│  - Only exchanged once per server                                        │
│  - Can be regenerated by server admin                                   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Implementation Checklist

### Server Implementation

- [ ] Generate serverId on first startup
- [ ] Generate serverToken on first startup
- [ ] Store in encrypted database
- [ ] Implement v3.0 discovery response
- [ ] Implement /api/auth/token-exchange
- [ ] Implement HMAC-SHA256 signing

### Client Implementation

- [ ] Implement v3.0 discovery request
- [ ] Implement signature verification
- [ ] Store serverToken in secure storage
- [ ] Implement auto-reconnect flow
- [ ] Handle IP change gracefully
- [ ] Show discovery list when multiple servers found

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-06-14 | Initial documentation |