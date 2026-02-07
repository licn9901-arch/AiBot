import service from '@/utils/request'

// 通用分页响应（所有日志端点统一返回格式）
export interface LogPageResponse {
  content: Record<string, any>[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  message?: string
}

// 操作日志查询参数
export interface OperationLogParams {
  userId?: number
  deviceId?: string
  action?: string
  startTime?: string
  endTime?: string
  page?: number
  size?: number
}

// 应用日志查询参数
export interface AppLogParams {
  level?: string
  logger?: string
  search?: string
  hours?: number
  page?: number
  size?: number
}

// 设备事件查询参数
export interface DeviceEventParams {
  deviceId?: string
  eventType?: string
  hours?: number
  page?: number
  size?: number
}

// 操作日志
export function getOperationLogs(params: OperationLogParams = {}): Promise<LogPageResponse> {
  return service.get('api/admin/logs', { params })
}

// 应用日志
export function getAppLogs(params: AppLogParams = {}): Promise<LogPageResponse> {
  return service.get('api/admin/logs/app', { params })
}

// 应用日志统计
export function getAppLogStats(hours: number = 24): Promise<Record<string, any>> {
  return service.get('api/admin/logs/app/stats', { params: { hours } })
}

// 设备事件
export function getDeviceEventLogs(params: DeviceEventParams = {}): Promise<LogPageResponse> {
  return service.get('api/admin/logs/events', { params })
}
