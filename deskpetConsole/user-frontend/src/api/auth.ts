import service from '@/utils/request'
import type {
  ForgotPasswordRequest,
  ResetPasswordRequest,
  TokenValidationResponse,
  UserLoginRequest,
  UserLoginResponse,
  UserRegisterRequest,
} from '@/types/user'

export function login(data: UserLoginRequest): Promise<UserLoginResponse> {
  return service.post('/auth/login', data)
}

export function register(data: UserRegisterRequest): Promise<void> {
  return service.post('/auth/register', data)
}

export function activateAccount(token: string): Promise<void> {
  return service.get('/auth/activate', { params: { token } })
}

export function forgotPassword(data: ForgotPasswordRequest): Promise<void> {
  return service.post('/auth/forgot-password', data)
}

export function validateResetPasswordToken(token: string): Promise<TokenValidationResponse> {
  return service.get('/auth/reset-password/validate', { params: { token } })
}

export function resetPassword(data: ResetPasswordRequest): Promise<void> {
  return service.post('/auth/reset-password', data)
}

export function logout(): Promise<void> {
  return service.post('/auth/logout')
}
