import { ref, onUnmounted } from 'vue'
import SockJS from 'sockjs-client/dist/sockjs.min.js'
import { Client } from '@stomp/stompjs'
import type { CommandResponse } from '@/types/command'

export interface PresenceEvent {
  deviceId: string
  online: boolean
  ts: string
}

/**
 * 设备 WebSocket composable
 * 订阅指定设备的指令状态变更和上下线事件
 */
export function useDeviceWebSocket(deviceId: string) {
  const commandStatus = ref<CommandResponse | null>(null)
  const presence = ref<PresenceEvent | null>(null)
  const connected = ref(false)
  let client: Client | null = null

  function connect() {
    const token = localStorage.getItem('token')
    client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      onConnect: () => {
        connected.value = true
        // 订阅指令状态
        client!.subscribe(`/topic/device/${deviceId}/command-status`, (msg) => {
          commandStatus.value = JSON.parse(msg.body)
        })
        // 订阅上下线
        client!.subscribe(`/topic/device/${deviceId}/presence`, (msg) => {
          presence.value = JSON.parse(msg.body)
        })
      },
      onDisconnect: () => {
        connected.value = false
      },
      onStompError: () => {
        connected.value = false
      },
      reconnectDelay: 3000,
    })
    client.activate()
  }

  function disconnect() {
    client?.deactivate()
    client = null
    connected.value = false
  }

  onUnmounted(disconnect)

  return { commandStatus, presence, connected, connect, disconnect }
}
