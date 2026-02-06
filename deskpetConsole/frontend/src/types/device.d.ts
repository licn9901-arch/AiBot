export interface Device {
    deviceId: string
    model: string
    remark?: string
    createdAt?: string
}

export interface DeviceSession {
    deviceId: string
    connected: boolean
    connectedAt?: number
    disconnectedAt?: number
    clientIp?: string
}

export interface TelemetryLatest {
    deviceId: string
    ts: number
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    payload: Record<string, any>
}

export interface DeviceResponse {
    device: Device
    session?: DeviceSession
    telemetry?: TelemetryLatest
}
