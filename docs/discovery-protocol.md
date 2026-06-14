# Immich Android Server Discovery Protocol

## Overview

This document defines the UDP-based discovery protocol used by the Immich Android Server to advertise its presence on the local network. The protocol supports both basic discovery and advanced security modes for secure server identification.

## Protocol Specification

### Transport

- **Protocol**: UDP
- **Port**: 2284
- **Broadcast Address**: 255.255.255.255

### Protocol Versions

| Version | Features | Security Level |
|---------|----------|----------------|
| **v1.0** | Basic discovery | None - accepts any server |
| **v2.0** | Server ID matching | Basic - prevents wrong server connection |
| **v3.0** | Token-based signing | Advanced - cryptographic verification |

---

## Version 1.0 (Basic Discovery)

### Message Flow

```
Client                                          Server
  |                                               |
  |---- UDP Broadcast: DISCOVER_IMMICH_SERVER --->|
  |                  to 255.255.255.255:2284      |
  |                                               |
  |<--- UDP Response: IMMICH_SERVER_RESPONSE:<JSON>|
  |       from server-ip:2284                     |
```

### Request Format

```
DISCOVER_IMMICH_SERVER
```

### Response Format (v1.0)

```
IMMICH_SERVER_RESPONSE:<JSON>
```

Example:
```
IMMICH_SERVER_RESPONSE:{"serverUrl":"http://192.168.1.21:2283/api","version":"3.0.0","serverName":"Immich Android Server","timestamp":1781052924}
```

#### Response Fields (v1.0)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `serverUrl` | string | yes | Full HTTP URL of the server API endpoint (must end with `/api`) |
| `version` | string | no | Server version string |
| `serverName` | string | no | Human-readable server name |
| `timestamp` | number | no | Unix epoch seconds when response was generated |

---

## Version 2.0 (Server ID Matching)

### Overview

Version 2.0 adds a unique `serverId` to identify the server. This allows clients to:
- Recognize previously connected servers
- Automatically reconnect when IP changes
- Avoid connecting to wrong servers

### Server ID Generation

The server generates a unique ID on first startup:

```
Server ID = UUID v4 (e.g., "550e8400-e29b-41d4-a716-446655440000")
```

- Stored in SQLite database (`server_config` table)
- Persists across server restarts
- Never changes unless database is cleared

### Message Flow (v2.0)

Same as v1.0, but response includes additional fields.

### Request Format (v2.0)

Same as v1.0:
```
DISCOVER_IMMICH_SERVER
```

### Response Format (v2.0)

```
IMMICH_SERVER_RESPONSE:<JSON>
```

Example:
```
IMMICH_SERVER_RESPONSE:{"serverId":"550e8400-e29b-41d4-a716-446655440000","serverName":"My Home Server","serverUrl":"http://192.168.1.21:2283/api","version":"3.0.0","timestamp":1781052924}
```

#### Response Fields (v2.0)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `serverId` | string | **yes** | Unique server identifier (UUID v4) |
| `serverName` | string | yes | Human-readable server name (user configurable) |
| `serverUrl` | string | yes | Full HTTP URL of the server API endpoint |
| `version` | string | no | Server version string |
| `timestamp` | number | no | Unix epoch seconds when response was generated |

### Client Behavior (v2.0)

#### First Connection

1. Client discovers server, receives `serverId`
2. Client saves `serverId`, `serverName`, `serverUrl` to local storage
3. Client connects to server

#### Subsequent Connections (IP Changed)

1. Client tries to connect to saved `serverUrl`
2. Connection fails (IP changed)
3. Client sends discovery broadcast
4. Client receives responses, matches `serverId`
5. If match found, client updates `serverUrl` and connects
6. If no match, client shows discovery list for user selection

---

## Version 3.0 (Token-Based Signing)

### Overview

Version 3.0 adds cryptographic signing to prevent:
- Man-in-the-middle attacks
- Server spoofing
- Unauthorized server impersonation

### Security Model

```
┌─────────────────────────────────────────────────────────────┐
│                    Security Model                            │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. First Connection (Secure Bootstrap)                     │
│     ┌─────────────┐     HTTPS      ┌─────────────┐         │
│     │   Client    │ ──────────────>│   Server    │         │
│     │             │                │             │         │
│     │             │<───────────────│             │         │
│     │             │  serverToken   │             │         │
│     │             │  (encrypted)   │             │         │
│     └─────────────┘                └─────────────┘         │
│                                                              │
│  2. Discovery (Signed Response)                             │
│     ┌─────────────┐     UDP        ┌─────────────┐         │
│     │   Client    │ ──────────────>│   Server    │         │
│     │             │  DISCOVER      │             │         │
│     │             │                │             │         │
│     │             │<───────────────│             │         │
│     │             │  signed resp   │             │         │
│     │             │  (HMAC-SHA256) │             │         │
│     └─────────────┘                └─────────────┘         │
│                                                              │
│  3. Verification                                             │
│     Client verifies signature using saved serverToken       │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Token Generation

#### Server Token

```
serverToken = HMAC-SHA256(serverId, randomSeed)
```

- Generated on first startup
- Stored in SQLite database (encrypted)
- 256-bit (32 bytes) random value
- Never transmitted over UDP

#### Token Exchange (First Connection)

The serverToken is exchanged via HTTPS during first connection:

```
POST /api/auth/token-exchange
Authorization: Basic <user credentials>

Response:
{
  "serverToken": "base64-encoded-token",
  "serverId": "550e8400-e29b-41d4-a716-446655440000",
  "expiresAt": null  // Token does not expire
}
```

### Request Format (v3.0)

Extended request with client challenge:

```
DISCOVER_IMMICH_SERVER:<clientId>:<challengeNonce>
```

Example:
```
DISCOVER_IMMICH_SERVER:client-abc123:nonce-xyz789
```

#### Request Fields (v3.0)

| Field | Description |
|-------|-------------|
| `DISCOVER_IMMICH_SERVER` | Protocol identifier |
| `clientId` | Client unique identifier |
| `challengeNonce` | Random nonce for this request (prevents replay) |

### Response Format (v3.0)

```
IMMICH_SERVER_RESPONSE:<JSON>
```

Example:
```
IMMICH_SERVER_RESPONSE:{"serverId":"550e8400-e29b-41d4-a716-446655440000","serverName":"My Home Server","serverUrl":"http://192.168.1.21:2283/api","version":"3.0.0","timestamp":1781052924,"challengeNonce":"nonce-xyz789","signature":"a1b2c3d4..."}
```

#### Response Fields (v3.0)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `serverId` | string | yes | Unique server identifier |
| `serverName` | string | yes | Human-readable server name |
| `serverUrl` | string | yes | Full HTTP URL of the server API endpoint |
| `version` | string | no | Server version string |
| `timestamp` | number | yes | Unix epoch seconds (used in signature) |
| `challengeNonce` | string | yes | Must match the request nonce |
| `signature` | string | yes | HMAC-SHA256 signature (hex encoded) |

### Signature Generation

```
signature = HMAC-SHA256(
  serverToken,
  serverId + "|" + serverUrl + "|" + timestamp + "|" + challengeNonce
)
```

#### Signature Components

| Component | Description |
|-----------|-------------|
| `serverToken` | Secret token (only known to server and legitimate client) |
| `serverId` | Server unique identifier |
| `serverUrl` | Server URL being advertised |
| `timestamp` | Response generation time |
| `challengeNonce` | Client-provided nonce |

### Client Verification (v3.0)

1. Check `challengeNonce` matches request
2. Check `timestamp` is within acceptable range (±30 seconds)
3. Compute expected signature using saved `serverToken`
4. Compare computed signature with received `signature`
5. If all checks pass, accept the response

### Security Considerations

#### Prevented Attacks

| Attack | Prevention |
|--------|------------|
| Server spoofing | Signature verification with serverToken |
| Replay attacks | Challenge nonce must match request |
| Man-in-the-middle | serverToken never sent over UDP |
| Unauthorized access | serverToken only exchanged after authentication |

#### Limitations

- Requires HTTPS for initial token exchange
- Token stored on client device (must be protected)
- If token is compromised, attacker can impersonate server

---

## Implementation Notes

### Server Implementation

#### Database Schema

```sql
-- Server configuration table
CREATE TABLE server_config (
    id INTEGER PRIMARY KEY,
    server_id TEXT NOT NULL UNIQUE,
    server_name TEXT NOT NULL DEFAULT 'Immich Android Server',
    server_token TEXT NOT NULL,  -- Encrypted
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);

-- Insert on first startup if not exists
INSERT OR IGNORE INTO server_config (server_id, server_name, server_token, created_at)
VALUES (?, 'Immich Android Server', ?, strftime('%s', 'now'));
```

#### Response Generation (v3.0)

```kotlin
fun createSignedResponse(
    serverId: String,
    serverName: String,
    serverUrl: String,
    serverToken: ByteArray,
    challengeNonce: String
): String {
    val timestamp = Clock.System.now().epochSeconds
    val dataToSign = "$serverId|$serverUrl|$timestamp|$challengeNonce"
    val signature = hmacSha256(serverToken, dataToSign.toByteArray())
    
    val response = DiscoveryResponseV3(
        serverId = serverId,
        serverName = serverName,
        serverUrl = serverUrl,
        version = "3.0.0",
        timestamp = timestamp,
        challengeNonce = challengeNonce,
        signature = signature.toHexString()
    )
    
    return "IMMICH_SERVER_RESPONSE:" + Json.encodeToString(response)
}
```

### Client Implementation

#### Token Storage

```dart
class SecureServerStorage {
  Future<void> saveServerToken(String serverId, String serverToken) async {
    // Use platform secure storage (Keychain on iOS, Keystore on Android)
    await _secureStorage.write(key: 'server_token_$serverId', value: serverToken);
  }
  
  Future<String?> getServerToken(String serverId) async {
    return await _secureStorage.read(key: 'server_token_$serverId');
  }
}
```

#### Signature Verification

```dart
bool verifySignature(
  String serverToken,
  String serverId,
  String serverUrl,
  int timestamp,
  String challengeNonce,
  String receivedSignature
) {
  // Check timestamp is within ±30 seconds
  final now = DateTime.now().millisecondsSinceEpoch / 1000;
  if (abs(now - timestamp) > 30) return false;
  
  // Compute expected signature
  final dataToSign = '$serverId|$serverUrl|$timestamp|$challengeNonce';
  final expectedSignature = hmacSha256(serverToken, utf8.encode(dataToSign));
  
  // Compare signatures
  return expectedSignature.toHexString() == receivedSignature;
}
```

---

## Version Compatibility

### Client-Server Compatibility Matrix

| Client Version | Server Version | Behavior |
|----------------|----------------|----------|
| v1.0 | v1.0 | Basic discovery, no security |
| v1.0 | v2.0 | Works (ignores serverId) |
| v1.0 | v3.0 | Works (ignores signature) |
| v2.0 | v1.0 | Works (no serverId in response) |
| v2.0 | v2.0 | Server ID matching |
| v2.0 | v3.0 | Server ID matching (ignores signature) |
| v3.0 | v1.0 | Fails (no signature in response) |
| v3.0 | v2.0 | Fails (no signature in response) |
| v3.0 | v3.0 | Full security verification |

### Graceful Degradation

Clients should implement graceful degradation:

1. Try v3.0 first (with challenge nonce)
2. If no valid signed response, fall back to v2.0 (serverId matching)
3. If no serverId, fall back to v1.0 (basic discovery)
4. Show appropriate warnings to user based on security level

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-06-10 | Initial protocol definition |
| 2.0 | 2026-06-14 | Added serverId for server identification |
| 3.0 | 2026-06-14 | Added token-based signing for secure verification |