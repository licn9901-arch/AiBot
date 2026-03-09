import service from '@/utils/request'
import type { UserResponse, UserUpdateRequest } from '@/types/user'

export function getCurrentUser(): Promise<UserResponse> {
  return service.get('/user/me')
}

export function updateCurrentUser(data: UserUpdateRequest): Promise<UserResponse> {
  return service.put('/user/me', data)
}
