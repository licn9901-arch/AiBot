export interface ProductResponse {
  id: string
  productKey: string
  name: string
  description: string | null
  status: string
  createdAt: string
  updatedAt: string | null
}

export interface ProductCreateRequest {
  productKey: string
  name: string
  description?: string
}

export interface ProductUpdateRequest {
  name?: string
  description?: string
  status?: string
}

export interface PropertyDTO {
  id: string
  identifier: string
  name: string
  dataType: string
  specs: Record<string, any> | null
  accessMode: string
  required: boolean
  description: string | null
  sortOrder: number
}

export interface PropertyCreateRequest {
  identifier: string
  name: string
  dataType: string
  specs?: Record<string, any>
  accessMode?: string
  required?: boolean
  description?: string
  sortOrder?: number
}

export interface ServiceDTO {
  id: string
  identifier: string
  name: string
  callType: string
  inputParams: Record<string, any>[] | null
  outputParams: Record<string, any>[] | null
  description: string | null
  sortOrder: number
}

export interface ServiceCreateRequest {
  identifier: string
  name: string
  callType?: string
  inputParams?: Record<string, any>[]
  outputParams?: Record<string, any>[]
  description?: string
  sortOrder?: number
}

export interface EventDTO {
  id: string
  identifier: string
  name: string
  eventType: string
  outputParams: Record<string, any>[] | null
  description: string | null
  sortOrder: number
}

export interface EventCreateRequest {
  identifier: string
  name: string
  eventType: string
  outputParams?: Record<string, any>[]
  description?: string
  sortOrder?: number
}

export interface ThingModelDTO {
  id: string
  productKey: string
  productName: string
  description: string | null
  status: string
  properties: PropertyDTO[]
  services: ServiceDTO[]
  events: EventDTO[]
  createdAt: string
  updatedAt: string | null
}

// 参数项接口，用于服务/事件的输入输出参数
export interface ParamItem {
  identifier: string
  name: string
  dataType: string
  specs?: Record<string, any>
}

// 数据类型选项
export const DATA_TYPE_OPTIONS = [
  { label: 'int', value: 'int' },
  { label: 'float', value: 'float' },
  { label: 'double', value: 'double' },
  { label: 'bool', value: 'bool' },
  { label: 'string', value: 'string' },
  { label: 'enum', value: 'enum' },
  { label: 'struct', value: 'struct' },
  { label: 'array', value: 'array' },
] as const
