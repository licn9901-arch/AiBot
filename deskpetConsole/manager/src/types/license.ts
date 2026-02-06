export type LicenseStatus = 'UNUSED' | 'ACTIVATED' | 'REVOKED'

export interface LicenseCodeResponse {
  id: number
  code: string
  batchNo: string | null
  status: LicenseStatus
  deviceId: string | null
  productKey: string | null
  userId: number | null
  activatedAt: string | null
  expiresAt: string | null
  remark: string | null
  createdAt: string
}

export interface GenerateLicenseRequest {
  count: number
  productKey: string
  batchNo?: string
  expiresAt?: string
  remark?: string
}

export interface GenerateLicenseResponse {
  id: number
  code: string
  deviceId: string
  deviceSecret: string
  productKey: string
  batchNo: string | null
  status: string
  expiresAt: string | null
  remark: string | null
  createdAt: string
}

export interface GenerateBatchResult {
  batchNo: string
  items: GenerateLicenseResponse[]
}

export interface LicenseQueryParams {
  status?: LicenseStatus
  batchNo?: string
  page?: number
  size?: number
}

export interface BatchStats {
  batchNo: string
  total: number
  unused: number
  activated: number
  revoked: number
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}
