export interface DeviceResponse {
    deviceId: string
    model: string
    productKey?: string
    remark?: string
    createdAt?: string
    online: boolean
    lastSeen?: string
    telemetry?: Record<string, any>
}
