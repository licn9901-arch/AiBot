export interface UserLoginRequest {
  username: string
  password: string
}

export interface UserLoginResponse {
  token: string
  user: UserResponse
}

export interface UserRegisterRequest {
  username: string
  password: string
  email?: string
  phone?: string
}

export interface UserResponse {
  id: number
  username: string
  email: string | null
  phone: string | null
  avatar: string | null
  createdAt: string
  updatedAt: string | null
}

export interface UserUpdateRequest {
  email?: string
  phone?: string
  avatar?: string
}
