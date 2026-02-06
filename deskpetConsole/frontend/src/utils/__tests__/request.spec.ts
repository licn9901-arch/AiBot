import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ElMessage } from 'element-plus'

// Use vi.hoisted to ensure mock is available in factory
const mocks = vi.hoisted(() => {
    const mockAxios: any = {
        create: vi.fn(),
        interceptors: {
            request: { use: vi.fn() },
            response: { use: vi.fn() }
        },
        get: vi.fn(),
        post: vi.fn()
    }
    // Make create return the same instance
    mockAxios.create.mockReturnValue(mockAxios)
    return {
        axios: mockAxios
    }
})

vi.mock('axios', () => ({
    default: mocks.axios
}))

vi.mock('element-plus', () => ({
    ElMessage: {
        error: vi.fn()
    }
}))

// Import request after mocking
import request from '../request'

describe('Request Utility', () => {
    let reqInterceptor: { onFulfilled: any, onRejected: any }
    let respInterceptor: { onFulfilled: any, onRejected: any }

    // Capture interceptors
    const reqHandlers = mocks.axios.interceptors.request.use.mock.calls[0]
    const respHandlers = mocks.axios.interceptors.response.use.mock.calls[0]

    if (reqHandlers) {
        reqInterceptor = {
            onFulfilled: reqHandlers[0],
            onRejected: reqHandlers[1]
        }
    }

    if (respHandlers) {
        respInterceptor = {
            onFulfilled: respHandlers[0],
            onRejected: respHandlers[1]
        }
    }

    beforeEach(() => {
        vi.clearAllMocks()
        // Re-setup create return value just in case
        mocks.axios.create.mockReturnValue(mocks.axios)
    })

    describe('Request Interceptor', () => {
        it('should exist', () => {
            expect(reqInterceptor).toBeDefined()
        })

        if (!reqHandlers) return

        it('should add Authorization header when token exists', () => {
            const config = { headers: {} }
            vi.spyOn(Storage.prototype, 'getItem').mockReturnValue('test-token')

            const result = reqInterceptor.onFulfilled(config)

            expect(result.headers.Authorization).toBe('Bearer test-token')
        })

        it('should not add Authorization header when token is missing', () => {
            const config = { headers: {} }
            vi.spyOn(Storage.prototype, 'getItem').mockReturnValue(null)

            const result = reqInterceptor.onFulfilled(config)

            expect(result.headers.Authorization).toBeUndefined()
        })
    })

    describe('Response Interceptor', () => {
        it('should exist', () => {
            expect(respInterceptor).toBeDefined()
        })

        if (!respHandlers) return

        it('should return data directly on success', () => {
            const response = { data: { success: true } }
            const result = respInterceptor.onFulfilled(response)
            expect(result).toEqual({ success: true })
        })

        it('should handle standard ErrorResponse', async () => {
            const error = {
                response: {
                    data: {
                        code: 'A0400',
                        message: 'Custom Error Message'
                    }
                }
            }

            try {
                await respInterceptor.onRejected(error)
            } catch (e) {
                expect(ElMessage.error).toHaveBeenCalledWith('Custom Error Message')
            }
        })

        it('should handle 401 by clearing token', async () => {
            const removeItemSpy = vi.spyOn(Storage.prototype, 'removeItem')
            const error = {
                response: {
                    status: 401,
                    data: {
                        code: 'A0301',
                        message: 'Unauthorized'
                    }
                }
            }

            try {
                await respInterceptor.onRejected(error)
            } catch (e) {
                expect(removeItemSpy).toHaveBeenCalledWith('token')
                expect(ElMessage.error).toHaveBeenCalledWith('Unauthorized')
            }
        })
    })
})
