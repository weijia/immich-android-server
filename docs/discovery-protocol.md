# Immich Android Server Discovery Protocol

## Overview

This document defines the UDP-based discovery protocol used by the Immich Android Server to advertise its presence on the local network.

## Protocol Specification

### Transport

- **Protocol**: UDP
- **Port**: 2284
- **Broadcast Address**: 255.255.255.255

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

The client sends a plain text UDP broadcast:

```
DISCOVER_IMMICH_SERVER
```

- No prefix, no suffix, no JSON wrapper
- Must match exactly (case-sensitive)

### Response Format

The server responds with a text payload containing a prefix followed by JSON:

```
IMMICH_SERVER_RESPONSE:<JSON>
```

Example response:

```
IMMICH_SERVER_RESPONSE:{"serverUrl":"http://192.168.1.21:2283/api","version":"1.108.0","serverName":"Immich Android Server","timestamp":1781052924}
```

#### Response Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `serverUrl` | string | yes | Full HTTP URL of the Immich server API endpoint (must end with `/api`) |
| `version` | string | no | Server version string |
| `serverName` | string | no | Human-readable server name |
| `timestamp` | number | no | Unix epoch seconds when response was generated |

#### Important Notes

- The `serverUrl` field **must** end with `/api` (e.g. `http://192.168.1.21:2283/api`)
- The response **must** start with the prefix `IMMICH_SERVER_RESPONSE:`
- The JSON part after the prefix is a standard JSON object

### Example

#### Client Request

```
UDP datagram to 255.255.255.255:2284
Payload: DISCOVER_IMMICH_SERVER
```

#### Server Response

```
UDP datagram from 192.168.1.21:2284
Payload: IMMICH_SERVER_RESPONSE:{"serverUrl":"http://192.168.1.21:2283/api","version":"1.108.0","serverName":"Immich Android Server","timestamp":1781052924}
```

## Implementation Notes

### Client

1. Bind a UDP socket to any available port (`0.0.0.0:0`)
2. Enable broadcast mode on the socket
3. Send broadcast request to `255.255.255.255:2284`
4. Wait for responses (recommended timeout: 5 seconds)
5. For each response, check for `IMMICH_SERVER_RESPONSE:` prefix
6. Parse the JSON part after the prefix
7. Extract `serverUrl` for connection
8. Deduplicate by `serverUrl`

### Server

1. Bind a UDP socket to `0.0.0.0:2284`
2. Listen for incoming datagrams
3. If payload equals `DISCOVER_IMMICH_SERVER`, respond with `IMMICH_SERVER_RESPONSE:` + JSON
4. Ensure `serverUrl` ends with `/api`
5. Send response back to the client's address (from the received datagram)

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-06-10 | Initial protocol definition |
