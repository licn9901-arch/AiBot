<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CachedImage from '@/components/ui/CachedImage.vue'
import { usePolling } from '@/composables/usePolling'
import { useResponsive } from '@/composables/useResponsive'
import { useDeviceStore } from '@/stores/device'
import { formatRelativeTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const { isMobile } = useResponsive()
const deviceStore = useDeviceStore()

const filters = [
  { label: '全部', value: 'all' },
  { label: '在线', value: 'online' },
  { label: '离线', value: 'offline' },
] as const

const activeFilter = computed(() => {
  const status = route.query.status
  return status === 'online' || status === 'offline' || status === 'all' ? status : 'all'
})

const filteredDevices = computed(() => {
  if (activeFilter.value === 'online') {
    return deviceStore.devices.filter((item) => item.online)
  }
  if (activeFilter.value === 'offline') {
    return deviceStore.devices.filter((item) => !item.online)
  }
  return deviceStore.devices
})

const deviceCards = computed(() =>
  filteredDevices.value.map((device, index) => ({
    ...device,
    title: device.remark || device.model || device.deviceId,
    statusLine: device.online
      ? `${device.deviceId} · 在线 · ${device.lastSeen ? `${formatRelativeTime(device.lastSeen)}同步` : '刚刚同步'}`
      : `${device.deviceId} · 离线 · ${formatRelativeTime(device.lastSeen)}`,
    description: device.online ? '支持进入控制台进行实时操控。' : '离线状态下控制功能不可用。',
    primaryActionText: device.online && index === 0 ? '置顶中' : device.online ? '在线设备' : '不可控制',
    secondaryActionText: device.online ? '进入控制台' : '查看详情',
  })),
)

const mobileDeviceCards = computed(() =>
  filteredDevices.value.map((device, index) => ({
    ...device,
    title: device.remark || device.model || device.deviceId,
    deviceLine: `设备 ID · ${device.deviceId}`,
    lastSeenText: `最后在线：${formatRelativeTime(device.lastSeen)}`,
    statusLabel: device.online ? '在线' : '离线',
    topLabel: index === 0 ? '置顶设备' : '暂未分组',
  })),
)

function updateFilter(status: 'all' | 'online' | 'offline') {
  router.replace({ path: '/devices', query: status === 'all' ? {} : { status } })
}

async function refreshDevices() {
  await deviceStore.fetchDevices()
}

function openDevice(deviceId: string) {
  router.push(`/devices/${deviceId}`)
}

function goToActivate() {
  router.push('/activate')
}

usePolling(() => deviceStore.fetchDevices(), 10000)

onMounted(async () => {
  await refreshDevices()
})
</script>

<template>
  <div v-if="isMobile" class="mobile-devices-page">
    <section class="mobile-page-heading">
      <div>
        <h1 class="mobile-page-title">我的设备</h1>
      </div>
      <button type="button" class="mobile-inline-button" @click="refreshDevices">
        {{ deviceStore.loading ? '刷新中' : '↻ 刷新' }}
      </button>
    </section>

    <section class="mobile-filter-shell">
      <button
        v-for="filter in filters"
        :key="filter.value"
        type="button"
        class="mobile-filter-pill"
        :class="[activeFilter === filter.value ? 'is-active' : '', `is-${filter.value}`]"
        @click="updateFilter(filter.value)"
      >
        {{ filter.label }}
      </button>
    </section>

    <section v-if="deviceStore.loading && mobileDeviceCards.length === 0" class="mobile-empty-card">
      <div class="ui-empty-emoji">⏳</div>
      <div>正在拉取你的设备列表...</div>
    </section>

    <template v-else>
      <section v-if="mobileDeviceCards.length > 0" class="mobile-group-row">
        <span class="mobile-group-label">未分组</span>
        <span class="mobile-group-count">{{ mobileDeviceCards.length }} 台设备</span>
      </section>

      <section v-if="mobileDeviceCards.length === 0" class="mobile-empty-card">
        <div class="ui-empty-emoji">📭</div>
        <div>当前筛选下没有设备，试试切换筛选或前往添加设备。</div>
        <button type="button" class="ui-button primary" @click="goToActivate">去添加</button>
      </section>

      <section v-else class="mobile-device-list">
        <article
          v-for="card in mobileDeviceCards"
          :key="card.deviceId"
          class="mobile-device-card"
          @click="openDevice(card.deviceId)"
        >
          <div class="mobile-device-card-top">
            <span class="mobile-device-tag">{{ card.topLabel }}</span>
            <button type="button" class="mobile-device-more" @click.stop>⋯</button>
          </div>
          <div class="mobile-device-card-main">
            <div class="mobile-device-icon" :class="card.online ? 'is-online' : 'is-offline'">
              <CachedImage
                v-if="card.productIcon"
                :src="card.productIcon"
                :cache-key="`product-icon:${card.productKey}`"
                :alt="card.title"
                class="devices-card-image"
              >
                <template #fallback>
                  <span>🤖</span>
                </template>
              </CachedImage>
              <span v-else>🤖</span>
            </div>
            <div class="mobile-device-copy">
              <div class="mobile-device-title-row">
                <h2 class="mobile-device-title">{{ card.title }}</h2>
                <span class="mobile-status-pill" :class="card.online ? 'is-online' : 'is-offline'">{{ card.statusLabel }}</span>
              </div>
              <div class="mobile-device-meta">{{ card.deviceLine }}</div>
              <div class="mobile-device-meta">{{ card.lastSeenText }}</div>
            </div>
          </div>
          <div class="mobile-device-actions">
            <button type="button" class="mobile-device-action" @click.stop="openDevice(card.deviceId)">
              {{ card.online ? '置顶中' : '置顶' }}
            </button>
          </div>
        </article>
      </section>
    </template>
  </div>

  <div v-else class="devices-page">
    <section class="devices-header">
      <div class="devices-header-copy">
        <h1 class="devices-title">设备管理</h1>
        <p class="devices-subtitle">统一管理设备状态与控制入口。</p>
      </div>
      <div class="devices-header-actions">
        <button type="button" class="devices-toolbar-button devices-toolbar-button-light" @click="refreshDevices">
          {{ deviceStore.loading ? '刷新中...' : '↻ 刷新状态' }}
        </button>
        <button type="button" class="devices-toolbar-button devices-toolbar-button-primary" @click="goToActivate">+ 添加设备</button>
      </div>
    </section>

    <section class="devices-filter-row">
      <button
        v-for="filter in filters"
        :key="filter.value"
        type="button"
        class="devices-filter-chip"
        :class="[activeFilter === filter.value ? 'is-active' : '', `is-${filter.value}`]"
        @click="updateFilter(filter.value)"
      >
        {{ filter.label }}
      </button>
    </section>

    <section v-if="deviceStore.loading && deviceCards.length === 0" class="devices-empty">
      <div class="ui-empty-emoji">⏳</div>
      <div>正在拉取你的设备列表...</div>
    </section>

    <section v-else-if="deviceCards.length === 0" class="devices-empty">
      <div class="ui-empty-emoji">📭</div>
      <div>当前筛选下没有设备，试试切换筛选或前往添加设备。</div>
      <button type="button" class="ui-button primary" @click="goToActivate">去添加</button>
    </section>

    <section v-else class="devices-grid">
      <article
        v-for="card in deviceCards"
        :key="card.deviceId"
        class="devices-card"
      >
        <div class="devices-card-main">
          <div class="devices-card-icon-wrap">
            <div class="devices-card-icon" :class="card.online ? 'is-online' : 'is-offline'">
              <CachedImage
                v-if="card.productIcon"
                :src="card.productIcon"
                :cache-key="`product-icon:${card.productKey}`"
                :alt="card.title"
                class="devices-card-image"
              >
                <template #fallback>
                  <span>🤖</span>
                </template>
              </CachedImage>
              <span v-else>🤖</span>
            </div>
          </div>
          <div class="devices-card-copy">
            <h2 class="devices-card-title">{{ card.title }}</h2>
            <div class="devices-card-status-line" :class="card.online ? 'is-online' : 'is-offline'">{{ card.statusLine }}</div>
            <p class="devices-card-description">{{ card.description }}</p>
            <div class="devices-card-actions">
              <button
                type="button"
                class="devices-card-pill"
                :class="card.online ? 'is-online' : 'is-offline'"
              >
                {{ card.primaryActionText }}
              </button>
              <button
                type="button"
                class="devices-card-pill is-neutral"
                @click="openDevice(card.deviceId)"
              >
                {{ card.secondaryActionText }}
              </button>
            </div>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>
