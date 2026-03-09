<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { showToast } from 'vant'
import CachedImage from '@/components/ui/CachedImage.vue'
import { useResponsive } from '@/composables/useResponsive'
import { usePolling } from '@/composables/usePolling'
import { useDeviceWebSocket } from '@/composables/useWebSocket'
import { sendCommand } from '@/api/command'
import { getDevice } from '@/api/device'
import type { DeviceResponse } from '@/types/device'

const route = useRoute()
const { isMobile } = useResponsive()

const deviceId = route.params.deviceId as string
type MoveDirection = 'forward' | 'left' | 'right' | 'backward'
type MoveCommandDirection = MoveDirection | 'stop'

const device = ref<DeviceResponse | null>(null)
const loading = ref(true)
const commandLoading = ref(false)
const workModeLoading = ref(false)
const selectedWorkMode = ref<'sleep' | 'curious' | null>(null)
const activeDirection = ref<MoveDirection | null>(null)
const joystickPointerId = ref<number | null>(null)

const { presence, connect, disconnect } = useDeviceWebSocket(deviceId)

const isOffline = computed(() => !device.value?.online)
const deviceName = computed(() => device.value?.remark || device.value?.model || device.value?.deviceId || '设备控制台')
const deviceSubtitle = computed(() => (device.value ? `ID: ${device.value.deviceId}` : '-'))
const pageTitle = computed(() => (isOffline.value ? '设备控制台（离线）' : '设备控制台'))
const pageSubtitle = computed(() =>
  isOffline.value
    ? '桌宠机器人 V1 · 当前设备离线，控制功能暂不可用'
    : '桌宠机器人 V1 · 实时控制台 · 云台联动',
)
const deviceStatusText = computed(() => (device.value?.online ? '在线 · 控制链路正常' : '离线 · 等待设备重新上线'))
const productIconCacheKey = computed(() => `product-icon:${device.value?.productKey || deviceId}`)

function notify(message: string, type: 'success' | 'warning' | 'error' = 'success') {
  if (isMobile.value) {
    showToast(message)
    return
  }

  if (type === 'success') ElMessage.success(message)
  if (type === 'warning') ElMessage.warning(message)
  if (type === 'error') ElMessage.error(message)
}

async function fetchDeviceInfo() {
  try {
    device.value = await getDevice(deviceId)
  } finally {
    loading.value = false
  }
}

let moveQueue: Promise<void> = Promise.resolve()

async function performMove(direction: MoveCommandDirection) {
  if (!device.value?.online) {
    notify('设备离线，当前不可控制', 'warning')
    return
  }

  commandLoading.value = true
  try {
    await sendCommand(deviceId, { type: 'move', payload: { direction } })
  } catch {
    notify('控制指令发送失败，请稍后重试', 'error')
  } finally {
    commandLoading.value = false
  }
}

function enqueueMove(direction: MoveCommandDirection) {
  moveQueue = moveQueue.then(() => performMove(direction))
  return moveQueue
}

async function handleWorkModeToggle(mode: 'sleep' | 'curious') {
  if (!device.value?.online) {
    notify('设备离线，当前不可切换工作模式', 'warning')
    return
  }

  const nextMode = selectedWorkMode.value === mode ? null : mode
  const modeValue = nextMode === 'sleep' ? 0 : nextMode === 'curious' ? 1 : 2

  workModeLoading.value = true
  try {
    await sendCommand(deviceId, {
      type: 'setWorkMode',
      payload: { mode: modeValue },
    })
    selectedWorkMode.value = nextMode
    notify(nextMode ? `已切换为${nextMode === 'sleep' ? '睡眠模式' : '好奇模式'}` : '已切换为正常模式')
  } catch {
    notify('工作模式切换失败，请稍后重试', 'error')
  } finally {
    workModeLoading.value = false
  }
}

function handleDirectionPress(event: PointerEvent, direction: MoveDirection) {
  if (activeDirection.value === direction) {
    return
  }

  const target = event.currentTarget as HTMLElement | null
  target?.setPointerCapture?.(event.pointerId)
  joystickPointerId.value = event.pointerId
  activeDirection.value = direction
  void enqueueMove(direction)
}

function handleDirectionRelease(event?: PointerEvent) {
  if (event && joystickPointerId.value !== null && event.pointerId !== joystickPointerId.value) {
    return
  }

  joystickPointerId.value = null
  if (!activeDirection.value) {
    return
  }

  activeDirection.value = null
  void enqueueMove('stop')
}

async function handleCenterStop() {
  joystickPointerId.value = null
  activeDirection.value = null
  await enqueueMove('stop')
}

watch(presence, (value) => {
  if (value && device.value) {
    device.value = { ...device.value, online: value.online }
  }
})

usePolling(async () => {
  await fetchDeviceInfo()
}, 12000)

onMounted(async () => {
  await fetchDeviceInfo()
  connect()
})

onBeforeUnmount(() => {
  disconnect()
})
</script>

<template>
  <div class="control-page">
    <section class="control-header">
      <div class="control-header-copy">
        <h1 class="control-title">{{ pageTitle }}</h1>
        <p class="control-subtitle">{{ pageSubtitle }}</p>
      </div>
    </section>

    <div v-if="loading" class="control-empty">
      <div class="ui-empty-emoji">⏳</div>
      <div>正在载入设备信息...</div>
    </div>

    <template v-else-if="device">
      <section class="control-hero" :class="isOffline ? 'is-offline' : ''">
        <div class="control-hero-icon">
          <CachedImage
            v-if="device.productIcon"
            :src="device.productIcon"
            :cache-key="productIconCacheKey"
            :alt="deviceName"
            class="control-hero-image"
          >
            <template #fallback>
              <span>🤖</span>
            </template>
          </CachedImage>
          <span v-else>🤖</span>
        </div>
        <div class="control-hero-copy">
          <h2 class="control-hero-title">{{ deviceName }}</h2>
          <div class="control-hero-meta">{{ deviceSubtitle }}</div>
          <div class="control-hero-status" :class="device.online ? 'is-online' : 'is-offline'">{{ deviceStatusText }}</div>
        </div>
      </section>

      <section class="control-mode-card">
        <div class="control-section-title">工作模式</div>
        <div class="control-mode-pills">
          <button
            type="button"
            class="control-mode-pill"
            :class="selectedWorkMode === 'sleep' ? 'is-active' : ''"
            :disabled="workModeLoading || isOffline"
            @click="handleWorkModeToggle('sleep')"
          >
            睡眠模式
          </button>
          <button
            type="button"
            class="control-mode-pill"
            :class="selectedWorkMode === 'curious' ? 'is-active' : ''"
            :disabled="workModeLoading || isOffline"
            @click="handleWorkModeToggle('curious')"
          >
            好奇模式
          </button>
        </div>
      </section>

      <section class="control-joystick-card">
        <div class="control-joystick-head">
          <div class="control-section-title">摇杆控制</div>
          <div class="control-joystick-tip">{{ isOffline ? '设备离线，暂不可控制' : '长按方向移动，松开自动停止' }}</div>
        </div>

        <div class="control-joystick-stage">
          <div
            class="control-joystick"
            :class="[activeDirection ? `is-${activeDirection}` : '', isOffline ? 'is-offline' : '']"
            @contextmenu.prevent
          >
            <div class="control-joystick-segment top" />
            <div class="control-joystick-segment right" />
            <div class="control-joystick-segment bottom" />
            <div class="control-joystick-segment left" />
            <div class="control-joystick-line control-joystick-line-vertical" />
            <div class="control-joystick-line control-joystick-line-horizontal" />

            <button
              type="button"
              class="control-joystick-button up"
              :class="activeDirection === 'forward' ? 'is-active' : ''"
              :disabled="isOffline"
              @pointerdown.prevent="handleDirectionPress($event, 'forward')"
              @pointerup.prevent="handleDirectionRelease"
              @pointercancel="handleDirectionRelease"
              @pointerleave="handleDirectionRelease"
              @lostpointercapture="handleDirectionRelease"
              @contextmenu.prevent
            >
              ↑
            </button>
            <button
              type="button"
              class="control-joystick-button left"
              :class="activeDirection === 'left' ? 'is-active' : ''"
              :disabled="isOffline"
              @pointerdown.prevent="handleDirectionPress($event, 'left')"
              @pointerup.prevent="handleDirectionRelease"
              @pointercancel="handleDirectionRelease"
              @pointerleave="handleDirectionRelease"
              @lostpointercapture="handleDirectionRelease"
              @contextmenu.prevent
            >
              ←
            </button>
            <button
              type="button"
              class="control-joystick-button center"
              :disabled="isOffline"
              @click="handleCenterStop"
              @contextmenu.prevent
            >
              <span class="sr-only">停止</span>
            </button>
            <button
              type="button"
              class="control-joystick-button right"
              :class="activeDirection === 'right' ? 'is-active' : ''"
              :disabled="isOffline"
              @pointerdown.prevent="handleDirectionPress($event, 'right')"
              @pointerup.prevent="handleDirectionRelease"
              @pointercancel="handleDirectionRelease"
              @pointerleave="handleDirectionRelease"
              @lostpointercapture="handleDirectionRelease"
              @contextmenu.prevent
            >
              →
            </button>
            <button
              type="button"
              class="control-joystick-button down"
              :class="activeDirection === 'backward' ? 'is-active' : ''"
              :disabled="isOffline"
              @pointerdown.prevent="handleDirectionPress($event, 'backward')"
              @pointerup.prevent="handleDirectionRelease"
              @pointercancel="handleDirectionRelease"
              @pointerleave="handleDirectionRelease"
              @lostpointercapture="handleDirectionRelease"
              @contextmenu.prevent
            >
              ↓
            </button>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>
