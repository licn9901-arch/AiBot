import service from '@/utils/request'
import type { UserLoginRequest, UserLoginResponse, UserRegisterRequest } from '@/types/user'

export function login(data: UserLoginRequest): Promise<UserLoginResponse> {
  return service.post('api/auth/login', data)
}

export function register(data: UserRegisterRequest): Promise<void> {
  return service.post('api/auth/register', data)
}

export function logout(): Promise<void> {
  return service.post('api/auth/logout')
}
