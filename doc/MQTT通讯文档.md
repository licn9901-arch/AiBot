# MQTT 通讯文档（设备固件对接）

适用范围：DeskPet V0.3
协议风格：MQTT + UTF-8 JSON

---

## 1. 连接参数

| 项目 | 值 |
| --- | --- |
| Broker 地址 | `mqtt-gateway` 所在主机 |
| Broker 端口 | 默认 `1883`（配置项：`mqtt.port`） |
| ClientId | `deviceId` |
| Username | `deviceId` |
| Password | 设备注册时的 `secret` |
| TLS | 可选（如需后续升级可扩展） |

> 鉴权失败时，网关返回连接拒绝（Bad user name or password）。

---

## 2. Topic 与 QoS

| 方向 | Topic | QoS | 说明 |
| --- | --- | --- | --- |
| 订阅 | `pet/{deviceId}/cmd` | 1 | 下行指令 |
| 发布 | `pet/{deviceId}/cmd/ack` | 1 | 指令回执 |
| 发布 | `pet/{deviceId}/telemetry` | 0 | 遥测上报 |
| 发布 | `pet/{deviceId}/event` | 1 | 事件上报（V0.3 新增） |

注意：
- 网关强校验 Topic 白名单与 `deviceId` 一致性。
- 在线状态由连接事件决定。

---

## 3. 订阅/发布规则（ACL）

设备只能：
- 订阅 `pet/{deviceId}/cmd`
- 发布 `pet/{deviceId}/telemetry`、`pet/{deviceId}/cmd/ack`、`pet/{deviceId}/event`

其他 Topic 会被网关忽略或拒绝。

---

## 4. 消息格式

### 4.1 指令（Core -> Device）

Topic：`pet/{deviceId}/cmd`

```json
{
  "schemaVersion": 1,
  "type": "move",
  "reqId": "uuid",
  "ts": 1730000000,
  "payload": {
    "direction": "forward",
    "speed": 0.6,
    "durationMs": 800
  }
}
```

type 取值（V0.3）：
- `move` - 移动控制
- `stop` - 停止
- `setEmotion` - 设置表情
- `speak` - 语音播报
- `playAnimation` - 播放动画
- `setBrightness` - 设置亮度
- `setVolume` - 设置音量
- `reboot` - 重启设备

### 4.2 回执（Device -> Core）

Topic：`pet/{deviceId}/cmd/ack`

```json
{
  "schemaVersion": 1,
  "reqId": "uuid",
  "ok": true,
  "code": "DONE",
  "message": "moved forward",
  "ts": 1730000002
}
```

建议 code：
`DONE` / `BAD_PAYLOAD` / `SAFETY_LIMIT` / `BUSY` / `DUPLICATE` / `INTERNAL_ERROR`

### 4.3 遥测（Device -> Core）

Topic：`pet/{deviceId}/telemetry`

```json
{
  "schemaVersion": 1,
  "ts": 1730000000,
  "battery": 87,
  "rssi": -55,
  "brightness": 80,
  "volume": 50,
  "version": "0.1.0"
}
```

### 4.4 事件（Device -> Core）（V0.3 新增）

Topic：`pet/{deviceId}/event`

```json
{
  "eventId": "collision",
  "eventType": "alert",
  "timestamp": 1730000000000,
  "params": {
    "direction": "front",
    "intensity": 75
  }
}
```

#### 事件类型（eventType）

| 类型 | 说明 |
| --- | --- |
| info | 普通信息事件 |
| alert | 告警事件 |
| error | 错误事件 |

#### 预定义事件（eventId）

| eventId | eventType | 说明 | 参数 |
| --- | --- | --- | --- |
| collision | alert | 碰撞事件 | direction, intensity |
| touch | info | 触摸事件 | position, duration |
| lowBattery | alert | 低电量告警 | level |
| fall | error | 跌落事件 | height |
| voiceWakeup | info | 语音唤醒 | keyword, confidence |
| buttonPress | info | 按键事件 | button, pressType |

---

## 5. 指令 payload 说明

### move
- direction：`forward` / `backward` / `left` / `right` / `stop`
- speed：0.0 ~ 1.0（设备需二次限幅）
- durationMs：建议 50 ~ 5000

### stop
- payload 允许为空或省略

### setEmotion
- emotion：`idle` / `happy` / `sad` / `angry` / `sleepy` / `excited` / `confused`

### speak
- text：要播报的文本（最大 200 字符）
- voice：音色 `default` / `cute` / `robot`

### playAnimation
- animation：`wave` / `dance` / `nod` / `shake` / `sleep` / `wakeup`

### setBrightness
- brightness：0 ~ 100

### setVolume
- volume：0 ~ 100

### reboot
- payload 允许为空或省略

---

## 6. 设备侧处理要求

1. **幂等**：缓存最近 N 个 `reqId`，重复 `reqId` 不重复执行，仅回执。
2. **安全限制**：对 speed/duration 做限幅，触发限制时回执 `SAFETY_LIMIT`。
3. **频率限制**：高频指令可拒绝并回 `BUSY`。
4. **时间戳**：`ts` 使用 Unix 秒级时间戳，事件 `timestamp` 使用毫秒级。

---

## 7. 建议的上报频率

- **遥测上报**：2~5 秒一次（建议 5 秒）
- **事件上报**：事件发生时立即上报

---

## 8. 交互流程（简版）

1. 设备连接 MQTT，完成鉴权。
2. 设备订阅 `pet/{deviceId}/cmd`。
3. Core 下发指令 -> 设备执行 -> 设备回 `cmd/ack`。
4. 设备定期上报 `telemetry`。
5. 设备检测到事件时上报 `event`。

---

## 9. 版本兼容

- `schemaVersion` 当前为 1。
- 未识别字段应忽略并保持向后兼容。
- 如收到不支持的版本或缺失字段，回执 `BAD_PAYLOAD`。
