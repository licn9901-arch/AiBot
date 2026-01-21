# Deskpet

一个由 MQTT 网关与核心业务服务组成的桌宠后端原型，包含设备注册、指令下发、遥测上报与在线状态管理。

## 模块

- `pet-core`：Spring Boot 核心服务，提供 REST API 与内部接口
- `mqtt-gateway`：Vert.x MQTT 网关，负责设备接入与内部转发

## 主要能力

- 设备注册与查询
- 指令下发与状态跟踪（含超时处理）
- MQTT 认证与 Topic 访问控制
- 遥测上报与最新状态维护
- 设备在线/离线通知
- 内部接口 Token 校验（可选）

## 运行环境

- JDK 17（建议）
- Maven 3.x

## 快速开始

1) 启动核心服务：

```bash
mvn -pl pet-core spring-boot:run
```

2) 启动 MQTT 网关：

使用 IDE 运行 `com.deskpet.gateway.GatewayApplication`，或自行配置运行方式。

3) 注册设备：

```bash
curl -X POST http://localhost:8080/api/devices \
  -H "Content-Type: application/json" \
  -d "{\"deviceId\":\"pet-001\",\"secret\":\"secret123\",\"model\":\"deskpet\",\"remark\":\"demo\"}"
```

4) 设备连接 MQTT：

- clientId：`deviceId`
- username：`deviceId`
- password：设备注册时的 `secret`
- 默认 MQTT 端口：`1883`

## 配置项

### pet-core（Spring Boot）

- `server.port`：HTTP 端口，默认 `8080`
- `gateway.baseUrl`：网关内部地址，默认 `http://localhost:8081`
- `internal.token`：内部接口 Token，默认空（不校验）
- `command.timeoutSec`：指令超时秒数，默认 `10`
- `command.timeoutScanMs`：超时扫描间隔，默认 `2000`

### mqtt-gateway（Vert.x）

- `mqtt.port`：MQTT 端口，默认 `1883`
- `internal.port`：网关内部 HTTP 端口，默认 `8081`
- `core.internalBaseUrl`：核心内部地址，默认 `http://localhost:8080`
- `gateway.instanceId`：网关实例 ID，默认 `gateway-1`
- `internal.token`：内部接口 Token，默认空（不校验）

说明：网关通过 JVM System Properties 读取配置，例如 `-Dinternal.token=xxx`。

## 核心 API（pet-core）

- `GET /api/devices`：设备列表
- `GET /api/devices/{deviceId}`：设备详情
- `POST /api/devices`：注册设备
- `POST /api/devices/{deviceId}/commands`：下发指令
- `GET /api/devices/{deviceId}/commands/{reqId}`：指令详情

指令下发请求示例：

```json
{
  "type": "setEmotion",
  "payload": {
    "emotion": "happy"
  }
}
```

## 内部接口

当 `internal.token` 非空时，调用需携带 `X-Internal-Token`。

### pet-core `/internal`

- `GET /internal/auth?deviceId=...&secret=...`
- `POST /internal/telemetry/{deviceId}`
- `POST /internal/ack/{deviceId}`
- `POST /internal/gateway/deviceOnline`
- `POST /internal/gateway/deviceOffline`

遥测上报示例：

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

指令回执示例：

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

## MQTT Topic 约定

- 订阅：`pet/{deviceId}/cmd`
- 上报遥测：`pet/{deviceId}/telemetry`
- 上报指令回执：`pet/{deviceId}/cmd/ack`

## 数据存储

当前版本使用内存存储（ConcurrentHashMap），服务重启后数据会丢失。
