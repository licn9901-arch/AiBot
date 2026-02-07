export interface DeviceResponse {
  deviceId: string
  model: string | null
  productKey: string
  remark: string | null
  createdAt: string
  online: boolean
  lastSeen: string | null
  telemetry: Record<string, any> | null
}

export interface TelemetryHistory {
  id: string
  deviceId: string
  telemetry: Record<string, any>
  createdAt: string
}

export interface DeviceEventResponse {
  id: string
  deviceId: string
  eventType: string
  params: Record<string, any> | null
  createdAt: string
}
