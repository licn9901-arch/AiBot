# 对外 API 文档（OpenAPI）

## 1. 访问方式

- Swagger UI：`http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

说明：仅包含对外 REST API，`/internal` 接口已从 OpenAPI 中隐藏。

## 2. 基础信息

- Base URL：`http://localhost:8080`
- Content-Type：`application/json`

## 3. 示例请求

### 3.1 设备注册

```bash
curl -X POST "http://localhost:8080/api/devices" \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "pet001",
    "secret": "secret123",
    "model": "deskpet-v0.1",
    "remark": "桌面测试设备"
  }'
```

### 3.2 设备列表

```bash
curl "http://localhost:8080/api/devices"
```

### 3.3 设备详情

```bash
curl "http://localhost:8080/api/devices/pet001"
```

### 3.4 下发指令

```bash
curl -X POST "http://localhost:8080/api/devices/pet001/commands" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "move",
    "payload": {
      "direction": "forward",
      "speed": 0.6,
      "durationMs": 800
    }
  }'
```

### 3.5 查询指令

```bash
curl "http://localhost:8080/api/devices/pet001/commands/{reqId}"
```

### 3.6 重试指令（仅 TIMEOUT/FAILED 可重试）

```bash
curl -X POST "http://localhost:8080/api/devices/pet001/commands/{reqId}/retry"
```

## 4. 错误响应示例

```json
{
  "code": "A0400",
  "message": "Invalid request",
  "details": {
    "field": "deviceId"
  }
}
```
