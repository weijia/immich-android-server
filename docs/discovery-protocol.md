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
  |<--- UDP Response: JSON payload ---------------|
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

The server responds with a JSON object:

```json
{
  "serverUrl": "http://192.168.1.21:2283",
  "version": "1.108.0",
  "serverName": "Immich Android Server",
  "timestamp": 1781052924
}
```

#### Response Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `serverUrl` | string | yes | Full HTTP URL of the Immich server API endpoint |
| `version` | string | no | Server version string |
| `serverName` | string | no | Human-readable server name |
| `timestamp` | number | no | Unix epoch seconds when response was generated |

### Example

#### Client Request

```
UDP datagram to 255.255.255.255:2284
Payload: DISCOVER_IMMICH_SERVER
```

#### Server Response

```
UDP datagram from 192.168.1.21:2284
Payload: {"serverUrl":"http://192.168.1.21:2283","version":"1.108.0","serverName":"Immich Android Server","timestamp":1781052924}
```

## Implementation Notes

### Client

1. Bind a UDP socket to any available port (`0.0.0.0:0`)
2. Send broadcast request to `255.255.255.255:2284`
3. Wait for responses (recommended timeout: 5 seconds)
4. Parse each response as JSON
5. Extract `serverUrl` for connection

### Server

1. Bind a UDP socket to `0.0.0.0:2284`
2. Listen for incoming datagrams
3. If payload equals `DISCOVER_IMMICH_SERVER`, respond with JSON
4. Send response back to the client's address (from the received datagram)

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-06-10 | Initial protocol definition |
