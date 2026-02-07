import service from '@/utils/request'

export interface AdminLoginRequest {
  username: string
  password: string
}

export interface AdminLoginResponse {
  token: string
  userId: string
  username: string
  roles: string[]
}

export function login(data: AdminLoginRequest): Promise<AdminLoginResponse> {
  return service.post('api/auth/login', data)
}

export function logout(): Promise<void> {
  return service.post('api/auth/logout')
}
