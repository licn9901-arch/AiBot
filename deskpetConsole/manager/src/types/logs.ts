// 操作日志（对应后端 OperationLog 实体）
export interface OperationLogEntry {
  id: string
  userId: string | null
  deviceId: string | null
  action: string
  payload: Record<string, any> | null
  ip: string | null
  userAgent: string | null
  createdAt: string
}

// 应用日志（对应 TimescaleDB ts_app_log）
export interface AppLogEntry {
  logTime: string
  level: string
  logger: string | null
  thread: string | null
  message: string | null
  exception: string | null
}

// 设备事件日志（对应 DeviceEvent 实体）
export interface DeviceEventEntry {
  id: string
  deviceId: string
  eventId: string
  eventType: string
  params: Record<string, any> | null
  createdAt: string
}

// 应用日志统计
export interface AppLogStats {
  [level: string]: number
}
