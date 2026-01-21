# Deskpet

An early-stage backend prototype for a desktop pet system. It consists of an MQTT gateway and a core service that handles device registration, command delivery, telemetry, and online status.

## Modules

- `pet-core`: Spring Boot core service with REST APIs and internal endpoints
- `mqtt-gateway`: Vert.x MQTT gateway for device access and internal forwarding

## Key Features

- Device registration and querying
- Command delivery with status tracking (timeout handling included)
- MQTT authentication and topic ACL
- Telemetry ingestion with latest state caching
- Online/offline presence updates
- Optional token validation for internal endpoints

## Requirements

- JDK 17 (recommended)
- Maven 3.x

## Quick Start

1) Start the core service:

```bash
mvn -pl pet-core spring-boot:run
```

2) Start the MQTT gateway:

Run `com.deskpet.gateway.GatewayApplication` from your IDE or preferred runner.

3) Register a device:

```bash
curl -X POST http://localhost:8080/api/devices \
  -H "Content-Type: application/json" \
  -d "{\"deviceId\":\"pet-001\",\"secret\":\"secret123\",\"model\":\"deskpet\",\"remark\":\"demo\"}"
```

4) Connect via MQTT:

- clientId: `deviceId`
- username: `deviceId`
- password: the device `secret`
- default MQTT port: `1883`

## Configuration

### pet-core (Spring Boot)

- `server.port`: HTTP port, default `8080`
- `gateway.baseUrl`: gateway internal base URL, default `http://localhost:8081`
- `internal.token`: internal token, default empty (no validation)
- `command.timeoutSec`: command timeout seconds, default `10`
- `command.timeoutScanMs`: timeout scan interval, default `2000`

### mqtt-gateway (Vert.x)

- `mqtt.port`: MQTT port, default `1883`
- `internal.port`: gateway internal HTTP port, default `8081`
- `core.internalBaseUrl`: core internal base URL, default `http://localhost:8080`
- `gateway.instanceId`: gateway instance ID, default `gateway-1`
- `internal.token`: internal token, default empty (no validation)

Note: the gateway reads config from JVM System Properties, e.g. `-Dinternal.token=xxx`.

## Core API (pet-core)

- `GET /api/devices`
- `GET /api/devices/{deviceId}`
- `POST /api/devices`
- `POST /api/devices/{deviceId}/commands`
- `GET /api/devices/{deviceId}/commands/{reqId}`

Command request example:

```json
{
  "type": "setEmotion",
  "payload": {
    "emotion": "happy"
  }
}
```

## Internal Endpoints

When `internal.token` is set, calls must include `X-Internal-Token`.

### pet-core `/internal`

- `GET /internal/auth?deviceId=...&secret=...`
- `POST /internal/telemetry/{deviceId}`
- `POST /internal/ack/{deviceId}`
- `POST /internal/gateway/deviceOnline`
- `POST /internal/gateway/deviceOffline`

Telemetry example:

```json
{
  "schemaVersion": 1,
  "ts": 1710000000,
  "firmwareVersion": "1.0.0",
  "rssi": -55,
  "battery": 0.82,
  "lastAction": "idle",
  "extra": {
    "temp": 36.5
  }
}
```

Command ack example:

```json
{
  "schemaVersion": 1,
  "reqId": "uuid",
  "ok": true,
  "code": null,
  "message": null,
  "ts": 1710000000
}
```

### mqtt-gateway `/internal`

- `POST /internal/command/send`

## MQTT Topics

- Subscribe: `pet/{deviceId}/cmd`
- Publish telemetry: `pet/{deviceId}/telemetry`
- Publish command ack: `pet/{deviceId}/cmd/ack`

## Storage

Current version uses in-memory stores (ConcurrentHashMap). Data is lost on restart.
