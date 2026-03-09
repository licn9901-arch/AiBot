import service from '@/utils/request'
import type { DeviceResponse, TelemetryHistory, DeviceEventResponse } from '@/types/device'

export function getMyDevices(): Promise<DeviceResponse[]> {
  return service.get('/user/me/devices')
}

export function getDevice(deviceId: string): Promise<DeviceResponse> {
  return service.get(`/devices/${deviceId}`)
}

export function getDeviceTelemetryHistory(deviceId: string, hours = 24): Promise<TelemetryHistory[]> {
  return service.get(`/devices/${deviceId}/telemetry/history`, { params: { hours } })
}

export function getDeviceEvents(deviceId: string, page = 0, size = 20): Promise<DeviceEventResponse[]> {
  return service.get(`/devices/${deviceId}/events`, { params: { page, size } })
}
