import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as apiLogin, logout as apiLogout } from '@/api/auth'
import type { AdminLoginRequest } from '@/api/auth'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const username = ref<string | null>(localStorage.getItem('username'))
  const roles = ref<string[]>(JSON.parse(localStorage.getItem('roles') || '[]'))

  async function login(data: AdminLoginRequest) {
    const res = await apiLogin(data)
    token.value = res.token
    username.value = res.username
    roles.value = res.roles
    localStorage.setItem('token', res.token)
    localStorage.setItem('username', res.username)
    localStorage.setItem('roles', JSON.stringify(res.roles))
  }

  async function logout() {
    try {
      if (token.value) await apiLogout()
    } catch {
      // 忽略登出接口错误
    } finally {
      token.value = null
      username.value = null
      roles.value = []
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('roles')
      router.push({ name: 'login' })
    }
  }

  function clearAuth() {
    token.value = null
    username.value = null
    roles.value = []
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('roles')
  }

  return { token, username, roles, login, logout, clearAuth }
})
