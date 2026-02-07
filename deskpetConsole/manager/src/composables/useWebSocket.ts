import { ref, onUnmounted } from 'vue'
import SockJS from 'sockjs-client/dist/sockjs.min.js'
import { Client, type StompSubscription } from '@stomp/stompjs'
import type { CommandResponse } from '@/api/device'

export interface PresenceEvent {
  deviceId: string
  online: boolean
  ts: string
}

/**
 * 管理后台 WebSocket composable
 * 支持动态切换 deviceId 订阅
 */
export function useDeviceWebSocket() {
  const commandStatus = ref<CommandResponse | null>(null)
  const presence = ref<PresenceEvent | null>(null)
  const connected = ref(false)
  let client: Client | null = null
  let commandSub: StompSubscription | null = null
  let presenceSub: StompSubscription | null = null

  function connect() {
    const token = localStorage.getItem('token')
    client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      onConnect: () => {
        connected.value = true
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

  /**
   * 订阅指定设备的消息，切换设备时会自动取消之前的订阅
   */
  function subscribeDevice(deviceId: string) {
    // 取消之前的订阅
    commandSub?.unsubscribe()
    presenceSub?.unsubscribe()
    commandSub = null
    presenceSub = null
    commandStatus.value = null
    presence.value = null

    if (!client || !connected.value || !deviceId) return

    commandSub = client.subscribe(`/topic/device/${deviceId}/command-status`, (msg) => {
      commandStatus.value = JSON.parse(msg.body)
    })
    presenceSub = client.subscribe(`/topic/device/${deviceId}/presence`, (msg) => {
      presence.value = JSON.parse(msg.body)
    })
  }

  function disconnect() {
    commandSub?.unsubscribe()
    presenceSub?.unsubscribe()
    client?.deactivate()
    client = null
    connected.value = false
  }

  onUnmounted(disconnect)

  return { commandStatus, presence, connected, connect, subscribeDevice, disconnect }
}
