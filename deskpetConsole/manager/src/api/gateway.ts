import service from '@/utils/request'

export interface GatewayMetrics {
  deskpet_gateway_online?: number
  deskpet_gateway_connect_total?: number
  deskpet_gateway_disconnect_total?: number
  deskpet_gateway_telemetry_total?: number
  deskpet_gateway_ack_total?: number
  deskpet_gateway_event_total?: number
  deskpet_gateway_auth_fail_total?: number
  deskpet_gateway_auth_retry_total?: number
  deskpet_gateway_callback_fail_total?: number
  deskpet_gateway_command_send_total?: number
  deskpet_gateway_command_send_ok_total?: number
  deskpet_gateway_command_send_fail_total?: number
  deskpet_gateway_uptime_seconds?: number
  [key: string]: any
}

export function getGatewayMetrics(): Promise<GatewayMetrics> {
  return service.get('api/admin/gateway/metrics')
}
