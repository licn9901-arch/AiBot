import axios from 'axios'
import type { AxiosInstance, AxiosError, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ErrorResponse } from '../types/api'

// Create axios instance
const service: AxiosInstance = axios.create({
    baseURL: import.meta.env.VITE_API_URL || '/api',
    timeout: 10000
})

// Request interceptor
service.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        // Get token from localStorage (or store)
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

// Response interceptor
service.interceptors.response.use(
    (response: AxiosResponse) => {
        // For 2xx responses, return data directly
        return response.data
    },
    (error: AxiosError<ErrorResponse>) => {
        let message = 'Network Error'

        if (error.response && error.response.data) {
            const errData = error.response.data

            // Use message from backend if available
            if (errData.message) {
                message = errData.message
            }

            // Handle specific error codes
            if (errData.code === 'A0301' || error.response.status === 401) {
                // Token invalid or unauthorized
                localStorage.removeItem('token')
                // Only reload if we are not already on login page (if applicable)
                // window.location.reload()
            }
        } else if (error.message) {
            message = error.message
        }

        ElMessage.error(message)
        return Promise.reject(error)
    }
)

export default service
