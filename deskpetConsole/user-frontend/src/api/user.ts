import service from '@/utils/request'
import type { UserResponse, UserUpdateRequest } from '@/types/user'

export function getCurrentUser(): Promise<UserResponse> {
  return service.get('api/user/me')
}

export function updateCurrentUser(data: UserUpdateRequest): Promise<UserResponse> {
  return service.put('api/user/me', data)
}
