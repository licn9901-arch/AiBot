export interface CommandCreateRequest {
  type: string
  payload: Record<string, any>
}

export interface CommandResponse {
  reqId: string
  deviceId: string
  type: string
  status: string
  createdAt: string
  ackAt: string | null
  ackPayload: Record<string, any> | null
  error: string | null
}
