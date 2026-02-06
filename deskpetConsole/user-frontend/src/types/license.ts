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

export interface ActivateLicenseRequest {
  code: string
}
