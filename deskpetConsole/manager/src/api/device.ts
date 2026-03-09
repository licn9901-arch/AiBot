import service from '@/utils/request'
import type { DeviceResponse } from '@/types/device'

export function getDevices(): Promise<DeviceResponse[]> {
    return service.get('/devices')
}

export function getDevice(id: string): Promise<DeviceResponse> {
    return service.get(`/devices/${id}`)
}
export interface TelemetryHistory {
    id: string
    deviceId: string
    telemetry: Record<string, any>
    createdAt: string
}

export function getDeviceTelemetryHistory(id: string, hours = 24): Promise<TelemetryHistory[]> {
    return service.get(`/devices/${id}/telemetry/history`, { params: { hours } })
}

export interface CommandResponse {
    reqId: string
    deviceId: string
    type: string
    status: string
    createdAt: string
    ackAt?: string
    ackPayload?: Record<string, any>
    error?: string
}

export function sendCommand(deviceId: string, type: string, payload: Record<string, any>): Promise<CommandResponse> {
    return service.post(`/devices/${deviceId}/commands`, { type, payload })
}

export function getCommand(deviceId: string, reqId: string): Promise<CommandResponse> {
    return service.get(`/devices/${deviceId}/commands/${reqId}`)
}
