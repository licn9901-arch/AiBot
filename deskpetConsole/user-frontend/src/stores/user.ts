import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserResponse } from '@/types/user'
import { login as apiLogin, logout as apiLogout } from '@/api/auth'
import { getCurrentUser } from '@/api/user'
import type { UserLoginRequest } from '@/types/user'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const userInfo = ref<UserResponse | null>(null)

  async function login(data: UserLoginRequest) {
    const res = await apiLogin(data)
    token.value = res.token
    userInfo.value = res.user
    localStorage.setItem('token', res.token)
  }

  async function fetchUser() {
    if (!token.value) return
    try {
      userInfo.value = await getCurrentUser()
    } catch {
      logout()
    }
  }

  async function logout() {
    try {
      if (token.value) await apiLogout()
    } catch {
      // 忽略登出接口错误
    } finally {
      token.value = null
      userInfo.value = null
      localStorage.removeItem('token')
      router.push({ name: 'login' })
    }
  }

  return { token, userInfo, login, fetchUser, logout }
})
