import service from '@/utils/request'
import type { CommandCreateRequest, CommandResponse } from '@/types/command'

export function sendCommand(deviceId: string, data: CommandCreateRequest): Promise<CommandResponse> {
  return service.post(`api/devices/${deviceId}/commands`, data)
}

export function getCommand(deviceId: string, reqId: string): Promise<CommandResponse> {
  return service.get(`api/devices/${deviceId}/commands/${reqId}`)
}

export function retryCommand(deviceId: string, reqId: string): Promise<CommandResponse> {
  return service.post(`api/devices/${deviceId}/commands/${reqId}/retry`)
}
