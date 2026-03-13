import service from '@/utils/request'

export interface ImageUploadResponse {
  bizType: string
  objectKey: string
  uploadUrl: string
  url: string
  method: 'PUT'
  headers: Record<string, string>
}

export async function uploadImage(file: File, bizType: 'avatar' | 'product-icon'): Promise<ImageUploadResponse> {
  const response = await service.post('/files/images/presign', {
    bizType,
    fileName: file.name,
    contentType: file.type,
    size: file.size,
  }) as ImageUploadResponse

  const uploadResponse = await fetch(response.uploadUrl, {
    method: response.method,
    headers: response.headers,
    body: file,
  })

  if (!uploadResponse.ok) {
    throw new Error('图片直传失败')
  }

  return response
}
