export interface ErrorResponse {
    code: string
    message: string
    details?: Record<string, any>
}

// Keep ApiResponse generic if needed, but per docs, success might just be T
// We can define a type for what the axios promise resolves to (T)
