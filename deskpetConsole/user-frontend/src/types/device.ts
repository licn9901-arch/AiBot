export interface DeviceResponse {
  deviceId: string
  productKey: string
  productName: string | null
  remark: string | null
  connected: boolean
  connectedAt: string | null
  disconnectedAt: string | null
  telemetry: Record<string, any> | null
  createdAt: string
}

export interface TelemetryHistory {
  id: number
  deviceId: string
  telemetry: Record<string, any>
  createdAt: string
}

export interface DeviceEventResponse {
  id: number
  deviceId: string
  eventType: string
  params: Record<string, any> | null
  createdAt: string
}
