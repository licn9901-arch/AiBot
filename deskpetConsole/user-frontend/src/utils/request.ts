import axios from 'axios'
import type { AxiosInstance, AxiosError, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ErrorResponse } from '../types/api'
import router from '../router'

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 10000
})

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error: AxiosError) => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data
  },
  (error: AxiosError<ErrorResponse>) => {
    let message = '网络错误'

    if (error.response && error.response.data) {
      const errData = error.response.data

      if (errData.message) {
        message = errData.message
      }

      if (errData.code === 'A0301' || error.response.status === 401) {
        localStorage.removeItem('token')
        router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
      }
    } else if (error.message) {
      message = error.message
    }

    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default service
