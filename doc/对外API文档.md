# 对外 API 文档

适用范围：DeskPet V0.5

## 1. 访问方式

- Swagger UI：`http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

说明：仅包含对外 REST API，`/internal` 接口已从 OpenAPI 中隐藏。

## 2. 基础信息

- Base URL：`http://localhost:8080`
- Content-Type：`application/json`

## 3. 认证说明

V0.2 起需要登录认证，使用 Sa-Token 框架。

- 登录后获取 Token，后续请求携带 `satoken` Cookie 或 Header
- 部分接口需要特定权限或角色

---

## 4. 认证接口（无需登录）

### 4.1 用户注册

```bash
curl -X POST "http://localhost:8080/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "nickname": "测试用户",
    "email": "test@example.com"
  }'
```

响应：
```json
{
  "id": 1,
  "username": "testuser",
  "nickname": "测试用户",
  "email": "test@example.com",
  "status": "ACTIVE",
  "roles": ["USER"]
}
```

### 4.2 用户登录

```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

响应：
```json
{
  "token": "xxx-xxx-xxx",
  "user": {
    "id": 1,
    "username": "testuser",
    "nickname": "测试用户"
  }
}
```

### 4.3 退出登录

```bash
curl -X POST "http://localhost:8080/api/auth/logout" \
  -H "satoken: xxx-xxx-xxx"
```

---

## 5. 用户接口（需登录）

### 5.1 获取当前用户信息

```bash
curl "http://localhost:8080/api/users/me" \
  -H "satoken: xxx-xxx-xxx"
```

### 5.2 更新当前用户信息

```bash
curl -X PUT "http://localhost:8080/api/users/me" \
  -H "Content-Type: application/json" \
  -H "satoken: xxx-xxx-xxx" \
  -d '{
    "nickname": "新昵称",
    "email": "new@example.com"
  }'
```

### 5.3 激活授权码

```bash
curl -X POST "http://localhost:8080/api/users/me/licenses/activate" \
  -H "Content-Type: application/json" \
  -H "satoken: xxx-xxx-xxx" \
  -d '{
    "code": "DKPT-XXXX-XXXX-XXXX"
  }'
```

### 5.4 我的授权码列表

```bash
curl "http://localhost:8080/api/users/me/licenses" \
  -H "satoken: xxx-xxx-xxx"
```

### 5.5 我的设备列表

```bash
curl "http://localhost:8080/api/users/me/devices" \
  -H "satoken: xxx-xxx-xxx"
```

---

## 6. 设备接口（需登录 + device:* 权限）

### 6.1 设备注册

```bash
curl -X POST "http://localhost:8080/api/devices" \
  -H "Content-Type: application/json" \
  -H "satoken: xxx-xxx-xxx" \
  -d '{
    "deviceId": "pet001",
    "secret": "secret123",
    "model": "deskpet-v1",
    "remark": "桌面测试设备"
  }'
```

### 6.2 设备列表

```bash
curl "http://localhost:8080/api/devices" \
  -H "satoken: xxx-xxx-xxx"
```

### 6.3 设备详情

```bash
curl "http://localhost:8080/api/devices/pet001" \
  -H "satoken: xxx-xxx-xxx"
```

响应包含：设备信息、在线状态、最新遥测数据。

### 6.4 下发指令

```bash
curl -X POST "http://localhost:8080/api/devices/pet001/commands" \
  -H "Content-Type: application/json" \
  -H "satoken: xxx-xxx-xxx" \
  -d '{
    "type": "move",
    "payload": {
      "direction": "forward",
      "speed": 0.6,
      "durationMs": 800
    }
  }'
```

响应：
```json
{
  "reqId": "uuid",
  "status": "PENDING"
}
```

### 6.5 查询指令

```bash
curl "http://localhost:8080/api/devices/pet001/commands/{reqId}" \
  -H "satoken: xxx-xxx-xxx"
```

### 6.6 重试指令（仅 TIMEOUT/FAILED 可重试）

```bash
curl -X POST "http://localhost:8080/api/devices/pet001/commands/{reqId}/retry" \
  -H "satoken: xxx-xxx-xxx"
```

### 6.7 设备事件历史（V0.3 新增）

```bash
curl "http://localhost:8080/api/devices/pet001/events?eventType=alert&page=0&size=20" \
  -H "satoken: xxx-xxx-xxx"
```

查询参数：
- `eventId`：事件标识符（可选）
- `eventType`：事件类型 info/alert/error（可选）
- `startTime`：开始时间（可选）
- `endTime`：结束时间（可选）
- `page`：页码（默认0）
- `size`：每页大小（默认20）

### 6.8 设备事件统计（V0.3 新增）

```bash
curl "http://localhost:8080/api/devices/pet001/events/stats" \
  -H "satoken: xxx-xxx-xxx"
```

响应：
```json
{
  "total": 156,
  "byEventId": {
    "collision": 45,
    "touch": 89,
    "lowBattery": 12
  },
  "byEventType": {
    "INFO": 99,
    "ALERT": 57,
    "ERROR": 0
  }
}
```

---

## 7. 管理接口（需 ADMIN 角色）

### 7.1 授权码管理

#### 批量生成授权码

```bash
curl -X POST "http://localhost:8080/api/admin/licenses/generate" \
  -H "Content-Type: application/json" \
  -H "satoken: xxx-xxx-xxx" \
  -d '{
    "count": 10,
    "batchNo": "BATCH-2024-001",
    "remark": "测试批次"
  }'
```

#### 授权码列表

```bash
curl "http://localhost:8080/api/admin/licenses?status=UNUSED&page=0&size=20" \
  -H "satoken: xxx-xxx-xxx"
```

#### 撤销授权码

```bash
curl -X PUT "http://localhost:8080/api/admin/licenses/{id}/revoke" \
  -H "satoken: xxx-xxx-xxx"
```

#### 导出授权码（CSV）

```bash
curl "http://localhost:8080/api/admin/licenses/export?batchNo=BATCH-2024-001" \
  -H "satoken: xxx-xxx-xxx" \
  -o licenses.csv
```

#### 批次统计

```bash
curl "http://localhost:8080/api/admin/licenses/batch/BATCH-2024-001/stats" \
  -H "satoken: xxx-xxx-xxx"
```

### 7.2 用户管理

#### 用户列表

```bash
curl "http://localhost:8080/api/admin/users?page=0&size=20" \
  -H "satoken: xxx-xxx-xxx"
```

#### 用户详情

```bash
curl "http://localhost:8080/api/admin/users/{id}" \
  -H "satoken: xxx-xxx-xxx"
```

#### 更新用户状态

```bash
curl -X PUT "http://localhost:8080/api/admin/users/{id}/status" \
  -H "Content-Type: application/json" \
  -H "satoken: xxx-xxx-xxx" \
  -d '{
    "status": "DISABLED"
  }'
```

### 7.3 操作日志

```bash
curl "http://localhost:8080/api/admin/logs?module=device&page=0&size=20" \
  -H "satoken: xxx-xxx-xxx"
```

---

## 8. 产品与物模型管理（V0.3 新增，需 ADMIN 角色）

### 8.1 产品管理

#### 创建产品

```bash
curl -X POST "http://localhost:8080/api/admin/products" \
  -H "Content-Type: application/json" \
  -H "satoken: xxx-xxx-xxx" \
  -d '{
    "productKey": "deskpet-v2",
    "productName": "桌宠V2",
    "description": "第二代桌宠设备"
  }'
```

#### 产品列表

```bash
curl "http://localhost:8080/api/admin/products" \
  -H "satoken: xxx-xxx-xxx"
```

#### 产品详情（含完整物模型）

```bash
curl "http://localhost:8080/api/admin/products/deskpet-v1" \
  -H "satoken: xxx-xxx-xxx"
```

#### 更新产品

```bash
curl -X PUT "http://localhost:8080/api/admin/products/deskpet-v2" \
  -H "Content-Type: application/json" \
  -H "satoken: xxx-xxx-xxx" \
  -d '{
    "productName": "桌宠V2升级版",
    "description": "更新描述"
  }'
```

#### 删除产品

```bash
curl -X DELETE "http://localhost:8080/api/admin/products/deskpet-v2" \
  -H "satoken: xxx-xxx-xxx"
```

### 8.2 物模型-属性

#### 添加属性

```bash
curl -X POST "http://localhost:8080/api/admin/products/deskpet-v1/properties" \
  -H "Content-Type: application/json" \
  -H "satoken: xxx-xxx-xxx" \
  -d '{
    "identifier": "temperature",
    "name": "温度",
    "dataType": "FLOAT",
    "accessMode": "R",
    "required": false,
    "specs": {"min": -20, "max": 60, "unit": "℃"},
    "description": "设备温度"
  }'
```

#### 更新属性

```bash
curl -X PUT "http://localhost:8080/api/admin/products/deskpet-v1/properties/{id}" \
  -H "Content-Type: application/json" \
  -H "satoken: xxx-xxx-xxx" \
  -d '{
    "name": "设备温度",
    "description": "更新描述"
  }'
```

#### 删除属性

```bash
curl -X DELETE "http://localhost:8080/api/admin/products/deskpet-v1/properties/{id}" \
  -H "satoken: xxx-xxx-xxx"
```

### 8.3 物模型-服务

#### 添加服务

```bash
curl -X POST "http://localhost:8080/api/admin/products/deskpet-v1/services" \
  -H "Content-Type: application/json" \
  -H "satoken: xxx-xxx-xxx" \
  -d '{
    "identifier": "customAction",
    "name": "自定义动作",
    "callType": "ASYNC",
    "inputParams": [
      {"identifier": "actionId", "name": "动作ID", "dataType": "STRING"}
    ],
    "outputParams": [],
    "description": "执行自定义动作"
  }'
```

#### 更新服务

```bash
curl -X PUT "http://localhost:8080/api/admin/products/deskpet-v1/services/{id}" \
  -H "Content-Type: application/json" \
  -H "satoken: xxx-xxx-xxx" \
  -d '{
    "name": "自定义动作V2",
    "description": "更新描述"
  }'
```

#### 删除服务

```bash
curl -X DELETE "http://localhost:8080/api/admin/products/deskpet-v1/services/{id}" \
  -H "satoken: xxx-xxx-xxx"
```

### 8.4 物模型-事件

#### 添加事件

```bash
curl -X POST "http://localhost:8080/api/admin/products/deskpet-v1/events" \
  -H "Content-Type: application/json" \
  -H "satoken: xxx-xxx-xxx" \
  -d '{
    "identifier": "overheat",
    "name": "过热告警",
    "eventType": "ALERT",
    "outputParams": [
      {"identifier": "temperature", "name": "温度", "dataType": "FLOAT"}
    ],
    "description": "设备过热时触发"
  }'
```

#### 更新事件

```bash
curl -X PUT "http://localhost:8080/api/admin/products/deskpet-v1/events/{id}" \
  -H "Content-Type: application/json" \
  -H "satoken: xxx-xxx-xxx" \
  -d '{
    "name": "过热告警V2",
    "description": "更新描述"
  }'
```

#### 删除事件

```bash
curl -X DELETE "http://localhost:8080/api/admin/products/deskpet-v1/events/{id}" \
  -H "satoken: xxx-xxx-xxx"
```

### 8.5 物模型导入导出

#### 导出物模型

```bash
curl "http://localhost:8080/api/admin/products/deskpet-v1/export" \
  -H "satoken: xxx-xxx-xxx" \
  -o thing-model.json
```

导出格式：
```json
{
  "properties": [...],
  "services": [...],
  "events": [...]
}
```

#### 导入物模型

```bash
curl -X POST "http://localhost:8080/api/admin/products/deskpet-v1/import" \
  -H "Content-Type: application/json" \
  -H "satoken: xxx-xxx-xxx" \
  -d @thing-model.json
```

---

## 9. 错误响应格式

```json
{
  "code": "A0400",
  "message": "Invalid request",
  "details": {
    "field": "deviceId"
  }
}
```

错误码详情见 `doc/错误码文档.md`。

---

## 10. API 端点汇总

### 认证接口（无需登录）
```
POST /api/auth/register           # 用户注册
POST /api/auth/login              # 用户登录
POST /api/auth/logout             # 退出登录
```

### 用户接口（需登录）
```
GET  /api/users/me                # 获取当前用户信息
PUT  /api/users/me                # 更新当前用户信息
POST /api/users/me/licenses/activate  # 激活授权码
GET  /api/users/me/licenses       # 我的授权码列表
GET  /api/users/me/devices        # 我的设备列表
```

### 设备接口（需登录 + device:* 权限）
```
POST /api/devices                 # 设备注册
GET  /api/devices                 # 设备列表
GET  /api/devices/{deviceId}      # 设备详情
POST /api/devices/{deviceId}/commands           # 下发指令
GET  /api/devices/{deviceId}/commands/{reqId}   # 查询指令
POST /api/devices/{deviceId}/commands/{reqId}/retry  # 重试指令
GET  /api/devices/{deviceId}/events             # 设备事件历史
GET  /api/devices/{deviceId}/events/stats       # 设备事件统计
```

### 管理接口（需 ADMIN 角色）
```
# 授权码管理
POST /api/admin/licenses/generate # 批量生成授权码
GET  /api/admin/licenses          # 授权码列表
PUT  /api/admin/licenses/{id}/revoke  # 撤销授权码
GET  /api/admin/licenses/export   # 导出授权码（CSV）
GET  /api/admin/licenses/batch/{batchNo}/stats  # 批次统计

# 用户管理
GET  /api/admin/users             # 用户列表
GET  /api/admin/users/{id}        # 用户详情
PUT  /api/admin/users/{id}/status # 更新用户状态

# 操作日志
GET  /api/admin/logs              # 操作日志查询

# 产品管理
POST   /api/admin/products                    # 创建产品
GET    /api/admin/products                    # 产品列表
GET    /api/admin/products/{productKey}       # 产品详情
PUT    /api/admin/products/{productKey}       # 更新产品
DELETE /api/admin/products/{productKey}       # 删除产品

# 物模型-属性
POST   /api/admin/products/{productKey}/properties      # 添加属性
PUT    /api/admin/products/{productKey}/properties/{id} # 更新属性
DELETE /api/admin/products/{productKey}/properties/{id} # 删除属性

# 物模型-服务
POST   /api/admin/products/{productKey}/services        # 添加服务
PUT    /api/admin/products/{productKey}/services/{id}   # 更新服务
DELETE /api/admin/products/{productKey}/services/{id}   # 删除服务

# 物模型-事件
POST   /api/admin/products/{productKey}/events          # 添加事件
PUT    /api/admin/products/{productKey}/events/{id}     # 更新事件
DELETE /api/admin/products/{productKey}/events/{id}     # 删除事件

# 导入导出
GET    /api/admin/products/{productKey}/export          # 导出物模型
POST   /api/admin/products/{productKey}/import          # 导入物模型
```

---

## WebSocket 实时推送接口（V0.5）

### 连接方式

- 端点：`/ws`（SockJS）
- 协议：STOMP over WebSocket
- 认证：STOMP CONNECT 帧中携带 `Authorization: Bearer {token}`

### 连接示例

```javascript
import SockJS from 'sockjs-client/dist/sockjs.min.js'
import { Client } from '@stomp/stompjs'

const client = new Client({
  webSocketFactory: () => new SockJS('/ws'),
  connectHeaders: { Authorization: `Bearer ${token}` },
  onConnect: () => {
    // 订阅指令状态
    client.subscribe('/topic/device/pet001/command-status', (msg) => {
      console.log(JSON.parse(msg.body))
    })
    // 订阅设备上下线
    client.subscribe('/topic/device/pet001/presence', (msg) => {
      console.log(JSON.parse(msg.body))
    })
  },
  reconnectDelay: 3000,
})
client.activate()
```

### 订阅 Topic

| Topic | 说明 | 触发时机 |
|-------|------|----------|
| `/topic/device/{deviceId}/command-status` | 指令状态变更 | 指令状态变为 SENT/ACKED/FAILED/TIMEOUT 时 |
| `/topic/device/{deviceId}/presence` | 设备上下线 | 设备连接/断开 MQTT 时 |

### 消息格式

#### 指令状态推送

```json
{
  "reqId": "b1f2a9c6-4a0e-4b0b-9d3b-1a2b3c4d5e6f",
  "deviceId": "pet001",
  "type": "move",
  "payload": {"direction": "forward"},
  "status": "ACKED",
  "ackCode": "DONE",
  "ackMessage": "moved forward",
  "createdAt": "2026-02-07T10:00:00Z",
  "updatedAt": "2026-02-07T10:00:01Z"
}
```

#### 设备上下线推送

```json
{
  "deviceId": "pet001",
  "online": true,
  "ts": "2026-02-07T10:00:00Z"
}
```
