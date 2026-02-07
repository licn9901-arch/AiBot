import service from '@/utils/request'
import type { LicenseCodeResponse, GenerateLicenseRequest, GenerateBatchResult, LicenseQueryParams, Page } from '@/types/license'

export function getLicenses(params: LicenseQueryParams = {}): Promise<Page<LicenseCodeResponse>> {
    return service.get('api/admin/licenses', { params })
}

export function generateLicenses(data: GenerateLicenseRequest): Promise<GenerateBatchResult> {
    return service.post('api/admin/licenses/generate', data)
}

export function revokeLicense(id: string): Promise<void> {
    return service.put(`api/admin/licenses/${id}/revoke`)
}

export function exportLicenses(params: LicenseQueryParams = {}): Promise<Blob> {
    return service.get('api/admin/licenses/export', {
        params,
        responseType: 'blob',
        // 跳过默认的 response.data 拦截，直接返回 blob
        transformResponse: undefined
    })
}

export function getBatchStats(batchNo: string): Promise<Record<string, any>> {
    return service.get(`api/admin/licenses/batch/${batchNo}/stats`)
}

export function downloadLicenseBatch(batchNo: string): Promise<Blob> {
    return service.get(`api/admin/licenses/batch/${batchNo}/download`, {
        responseType: 'blob',
        transformResponse: undefined
    })
}

export function confirmLicenseBatch(batchNo: string): Promise<void> {
    return service.post(`api/admin/licenses/batch/${batchNo}/confirm`)
}
