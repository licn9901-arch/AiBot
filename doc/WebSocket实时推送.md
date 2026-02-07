# WebSocket 实时推送

适用范围：DeskPet V0.5

---

## 1. 概述

V0.5 引入 WebSocket 双向通信，替代前端 HTTP 轮询方案。后端在指令状态变更和设备上下线时，通过 WebSocket 主动推送给前端，实现毫秒级实时更新。

### 1.1 解决的问题

| 问题 | 旧方案（轮询） | 新方案（WebSocket） |
|------|---------------|-------------------|
| 指令状态更新 | `setInterval` 每 2 秒轮询，最多 10 次 | 状态变更时主动推送 |
| 设备上下线感知 | 依赖 `usePolling` 每 10 秒刷新设备信息 | 上下线事件实时推送 |
| 带宽消耗 | 大量无效 HTTP 请求 | 仅在有变更时传输数据 |
| 延迟 | 最高 2 秒延迟 | 近实时（<100ms） |

### 1.2 不变的部分

- mqtt-gateway 仍通过 HTTP 回调 pet-core（`/internal/ack`、`/internal/gateway/deviceOnline` 等）
- 指令创建仍通过 REST API（`POST /api/devices/{deviceId}/commands`）
- Command 实体仍存 PostgreSQL（JPA），TimescaleDB 仅做历史归档
- DeviceService.markOnline/markOffline 保持 PG 最新状态 + TimescaleDB 历史记录

---

## 2. 技术方案

### 2.1 技术选型

**Spring WebSocket + STOMP over SockJS**

| 特性 | 说明 |
|------|------|
| 协议 | STOMP（Simple Text Oriented Messaging Protocol） |
| 传输 | WebSocket，SockJS fallback |
| 认证 | STOMP CONNECT 阶段 Bearer Token（Sa-Token 验证） |
| 订阅模型 | Topic 订阅，按 deviceId 隔离 |
| 重连 | 客户端自动重连（3 秒间隔） |

选择理由：
1. Spring 原生支持 STOMP，配置简单
2. 内置 topic 订阅机制，天然支持按设备 ID 推送
3. Sa-Token 可在 STOMP CONNECT 阶段通过 header 传递 token 认证
4. SockJS fallback 保证浏览器兼容性

### 2.2 架构图

```
┌──────────────┐   STOMP/WebSocket   ┌──────────────────────────────┐
│  前端         │ ◄──────────────────► │         pet-core             │
│  (Vue 3)     │   /ws (SockJS)      │  ┌────────────────────────┐  │
│              │                      │  │  WebSocketPushService  │  │
│  subscribe:  │                      │  │  - pushCommandStatus() │  │
│  /topic/     │                      │  │  - pushPresence()      │  │
│  device/     │                      │  └──────────┬─────────────┘  │
│  {id}/...    │                      │             │                │
└──────────────┘                      │  ┌──────────▼─────────────┐  │
                                      │  │  CommandService        │  │
                                      │  │  DeviceService         │  │
                                      │  └────────────────────────┘  │
                                      └──────────────────────────────┘
```

---

## 3. 后端实现

### 3.1 文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `pet-core/pom.xml` | 修改 | 添加 `spring-boot-starter-websocket` |
| `config/WebSocketConfig.java` | 新建 | STOMP broker + SockJS 端点配置 |
| `config/WebSocketAuthInterceptor.java` | 新建 | STOMP CONNECT 认证拦截器 |
| `config/SaTokenConfig.java` | 修改 | 排除 `/ws/**` 路径 |
| `service/WebSocketPushService.java` | 新建 | 推送服务封装 |
| `service/CommandService.java` | 修改 | 状态变更时调用推送 |
| `service/DeviceService.java` | 修改 | 上下线时调用推送 |

### 3.2 WebSocket 配置

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

### 3.3 认证机制

在 STOMP CONNECT 阶段拦截，从 `Authorization` header 提取 Bearer Token，使用 Sa-Token 验证：

```java
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            // 提取 Bearer token 并用 Sa-Token 验证
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) throw new MessagingException("未登录");
        }
        return message;
    }
}
```

### 3.4 推送服务

```java
@Service
@RequiredArgsConstructor
public class WebSocketPushService {
    private final SimpMessagingTemplate messagingTemplate;

    public void pushCommandStatus(String deviceId, CommandResponse response) {
        messagingTemplate.convertAndSend(
            "/topic/device/" + deviceId + "/command-status", response);
    }

    public void pushPresence(String deviceId, boolean online) {
        messagingTemplate.convertAndSend(
            "/topic/device/" + deviceId + "/presence",
            Map.of("deviceId", deviceId, "online", online, "ts", Instant.now().toString()));
    }
}
```

### 3.5 集成点

**CommandService** — 以下方法中调用 `pushCommandStatus()`：
- `handleAck()` — 收到设备回执后
- `timeoutScan()` — 指令超时后
- `dispatchCommand()` — 指令状态变为 SENT 或 FAILED 后

**DeviceService** — 以下方法中调用 `pushPresence()`：
- `markOnline()` — 设备上线后
- `markOffline()` — 设备下线后

所有推送均使用 `@Autowired(required = false)` 注入，推送失败用 try-catch 包裹，不影响主业务流程。

---

## 4. 前端实现

### 4.1 文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `user-frontend/package.json` | 修改 | 添加 @stomp/stompjs, sockjs-client |
| `user-frontend/vite.config.ts` | 修改 | 添加 /ws 代理 |
| `user-frontend/src/composables/useWebSocket.ts` | 新建 | WebSocket composable（固定 deviceId） |
| `user-frontend/src/pages/DeviceControl.vue` | 修改 | 移除轮询，接入 WebSocket |
| `manager/package.json` | 修改 | 添加 @stomp/stompjs, sockjs-client |
| `manager/vite.config.ts` | 修改 | 添加 /ws 代理 |
| `manager/src/composables/useWebSocket.ts` | 新建 | WebSocket composable（动态 deviceId） |
| `manager/src/pages/Commands.vue` | 修改 | 接入 WebSocket 自动刷新 |

### 4.2 依赖

```bash
npm install @stomp/stompjs sockjs-client
npm install -D @types/sockjs-client
```

### 4.3 Vite 代理配置

```typescript
// vite.config.ts
server: {
  proxy: {
    '/ws': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      ws: true
    }
  }
}
```

### 4.4 user-frontend composable

固定 deviceId，适用于单设备控制页面：

```typescript
export function useDeviceWebSocket(deviceId: string) {
  const commandStatus = ref<CommandResponse | null>(null)
  const presence = ref<PresenceEvent | null>(null)

  function connect() {
    const token = localStorage.getItem('token')
    client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      onConnect: () => {
        client.subscribe(`/topic/device/${deviceId}/command-status`, ...)
        client.subscribe(`/topic/device/${deviceId}/presence`, ...)
      },
      reconnectDelay: 3000,
    })
    client.activate()
  }
  // ...
}
```

### 4.5 manager composable

支持动态切换 deviceId，适用于管理后台可选择不同设备的场景：

```typescript
export function useDeviceWebSocket() {
  function connect() { /* 建立连接 */ }
  function subscribeDevice(deviceId: string) {
    // 取消之前的订阅，订阅新设备
  }
  // ...
}
```

### 4.6 DeviceControl.vue 改造

- 移除 `pollCommandStatus()` 函数及 `setInterval` 逻辑
- 通过 `watch(commandStatus)` 自动更新 `lastCommand`
- 通过 `watch(presence)` 实时更新设备在线状态
- `onMounted` 时调用 `wsConnect()` 建立连接

### 4.7 Commands.vue 改造

- 通过 `watch(commandStatus)` 自动更新 commandLog 中的状态
- 通过 `watch(selectedDeviceId)` 切换设备时重新订阅
- 保留"刷新状态"按钮作为 fallback

---

## 5. 消息格式

### 5.1 指令状态推送

**Topic**: `/topic/device/{deviceId}/command-status`

**触发时机**: 指令状态变为 SENT、ACKED、FAILED、TIMEOUT 时

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

### 5.2 设备上下线推送

**Topic**: `/topic/device/{deviceId}/presence`

**触发时机**: 设备连接/断开 MQTT 时

```json
{
  "deviceId": "pet001",
  "online": true,
  "ts": "2026-02-07T10:00:00Z"
}
```

---

## 6. 数据流

### 6.1 指令状态推送流程

```
用户发送指令 (REST API)
  → CommandService.createCommand()
    → dispatchCommand()
      → 保存状态 SENT/FAILED
      → WebSocketPushService.pushCommandStatus() → 前端收到状态更新

设备回执 (MQTT → Gateway → Internal HTTP)
  → CommandService.handleAck()
    → 保存状态 ACKED/FAILED
    → WebSocketPushService.pushCommandStatus() → 前端收到状态更新

超时扫描 (定时任务)
  → CommandService.timeoutScan()
    → 保存状态 TIMEOUT
    → WebSocketPushService.pushCommandStatus() → 前端收到状态更新
```

### 6.2 设备上下线推送流程

```
设备连接 MQTT
  → Gateway 回调 /internal/gateway/deviceOnline
    → DeviceService.markOnline()
      → 保存 DeviceSession
      → WebSocketPushService.pushPresence(deviceId, true) → 前端收到上线通知

设备断开 MQTT
  → Gateway 回调 /internal/gateway/deviceOffline
    → DeviceService.markOffline()
      → 保存 DeviceSession
      → WebSocketPushService.pushPresence(deviceId, false) → 前端收到下线通知
```

---

## 7. 验证方式

### 7.1 后端验证

```bash
# 1. 启动 pet-core，确认 /ws 端点可访问
curl http://localhost:8080/ws/info
# 应返回 SockJS info JSON

# 2. 检查日志中无 WebSocket 相关错误
```

### 7.2 前端验证

1. 打开浏览器开发者工具 → Network 面板
2. 筛选 WS 类型，确认 WebSocket 连接建立成功
3. 在 DeviceControl 页面发送指令，确认：
   - 无 `/commands/{reqId}` 轮询请求
   - 状态通过 WebSocket 帧实时更新
4. 在 Commands 页面发送指令，确认时间线自动更新状态
5. 通过 mqtt-gateway 模拟设备上下线，确认前端实时收到在线状态变更

### 7.3 异常场景

| 场景 | 预期行为 |
|------|----------|
| WebSocket 连接断开 | STOMP 客户端 3 秒后自动重连 |
| Token 过期 | STOMP CONNECT 被拒绝，前端可引导重新登录 |
| WebSocketPushService 推送失败 | try-catch 捕获，不影响主业务流程 |
| 浏览器不支持 WebSocket | SockJS 自动降级为 HTTP 长轮询 |

---

## 8. 生产环境注意事项

### 8.1 Nginx 反向代理

```nginx
location /ws {
    proxy_pass http://pet-core:8080;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_read_timeout 3600s;
}
```

### 8.2 扩展性

当前使用 Spring 内置 SimpleBroker，适用于单实例部署。如需多实例水平扩展，可替换为外部消息代理：

```java
// 替换 SimpleBroker 为 RabbitMQ/ActiveMQ
config.enableStompBrokerRelay("/topic")
    .setRelayHost("rabbitmq-host")
    .setRelayPort(61613);
```

### 8.3 监控

- 通过 Spring Actuator 监控 WebSocket 会话数
- 关注 WebSocket 连接数是否异常增长
- 建议设置连接数上限，防止资源耗尽
