import service from '@/utils/request'
import type { LicenseCodeResponse, ActivateLicenseRequest } from '@/types/license'

export function activateLicense(data: ActivateLicenseRequest): Promise<LicenseCodeResponse> {
  return service.post('api/user/licenses/activate', data)
}

export function getMyLicenses(): Promise<LicenseCodeResponse[]> {
  return service.get('api/user/licenses')
}
