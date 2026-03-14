<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import CachedImage from '@/components/ui/CachedImage.vue'
import { useResponsive } from '@/composables/useResponsive'
import { useDeviceStore } from '@/stores/device'
import { useUserStore } from '@/stores/user'
import { formatRelativeTime } from '@/utils/format'

const router = useRouter()
const { isMobile } = useResponsive()
const userStore = useUserStore()
const deviceStore = useDeviceStore()
const refreshing = ref(false)

const username = computed(() => userStore.userInfo?.username || 'admin')
const onlineCount = computed(() => deviceStore.devices.filter((item) => item.online).length)
const offlineCount = computed(() => deviceStore.devices.filter((item) => !item.online).length)
const recentAddedCount = computed(() => {
  const thirtyDaysAgo = Date.now() - 30 * 24 * 60 * 60 * 1000

  return deviceStore.devices.filter((item) => {
    const createdAt = new Date(item.createdAt).getTime()
    return !Number.isNaN(createdAt) && createdAt >= thirtyDaysAgo
  }).length
})

const sortedDevices = computed(() =>
  [...deviceStore.devices].sort((left, right) => new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime()),
)

const recentDevices = computed(() =>
  sortedDevices.value.slice(0, 2).map((device) => ({
    ...device,
    title: device.remark || device.model || `Cubee ${device.productKey}`,
    subtitle: `ID: ${device.deviceId}`,
    statusText: device.online
      ? `在线 · ${device.lastSeen ? `${formatRelativeTime(device.lastSeen)}同步` : '刚刚同步'}`
      : `离线 · ${formatRelativeTime(device.lastSeen)}`,
  })),
)

const stats = computed(() => [
  { label: '设备总数', value: String(deviceStore.devices.length), tone: 'default', status: 'all' },
  { label: '在线设备', value: String(onlineCount.value), tone: 'success', status: 'online' },
  { label: '离线设备', value: String(offlineCount.value), tone: 'danger', status: 'offline' },
  { label: '最近新增', value: `+${recentAddedCount.value}`, tone: 'accent', status: 'recent' },
])

const mobileStats = computed(() => stats.value.slice(0, 3))

async function refreshData() {
  if (refreshing.value) {
    return
  }
  refreshing.value = true
  try {
    await Promise.all([userStore.fetchUser(), deviceStore.fetchDevices()])
  } finally {
    refreshing.value = false
  }
}

onMounted(async () => {
  await refreshData()
})
</script>

<template>
  <div v-if="isMobile" class="mobile-home-page">
    <section class="mobile-hero-card">
      <div class="mobile-hero-kicker">今日概览</div>
      <h1 class="mobile-hero-title">你好，{{ username }} 👋</h1>
      <p class="mobile-hero-description">智慧生活，尽在掌控。查看状态与最近动态。</p>
      <button type="button" class="mobile-hero-button" @click="router.push('/devices')">管理设备 →</button>
    </section>

    <section class="mobile-home-stats">
      <article
        v-for="stat in mobileStats"
        :key="stat.label"
        class="mobile-stat-card"
        @click="router.push(stat.status === 'all' ? '/devices' : { path: '/devices', query: { status: stat.status } })"
      >
        <div class="mobile-stat-label" :class="`is-${stat.tone}`">{{ stat.label }}</div>
        <div class="mobile-stat-value">{{ stat.value }}</div>
        <div class="mobile-stat-foot">{{ stat.status === 'all' ? '全部设备' : stat.status === 'online' ? '运行稳定' : '等待恢复' }}</div>
      </article>
    </section>

    <section class="mobile-section-block">
      <div class="mobile-section-head">
        <h2 class="mobile-section-title">最近设备</h2>
        <button type="button" class="mobile-section-link" @click="router.push('/devices')">查看全部</button>
      </div>

      <div v-if="recentDevices.length === 0" class="mobile-empty-card">
        <div class="ui-empty-emoji">□</div>
        <div>当前还没有已激活设备，先去完成授权激活吧。</div>
        <button type="button" class="ui-button primary" @click="router.push('/activate')">前往激活</button>
      </div>

      <div v-else class="mobile-recent-list">
        <article
          v-for="device in recentDevices"
          :key="device.deviceId"
          class="mobile-recent-card"
          @click="router.push(`/devices/${device.deviceId}`)"
        >
          <div class="mobile-recent-icon" :class="device.online ? 'is-online' : 'is-offline'">
            <CachedImage
              v-if="device.productIcon"
              :src="device.productIcon"
              :cache-key="`product-icon:${device.productKey}`"
              :alt="device.title"
              class="home-device-image"
            >
              <template #fallback>
                <span>🤖</span>
              </template>
            </CachedImage>
            <span v-else>🤖</span>
          </div>
          <div class="mobile-recent-copy">
            <div class="mobile-recent-name">{{ device.title }}</div>
            <div class="mobile-recent-subtitle">{{ device.subtitle }}</div>
            <div class="mobile-recent-status" :class="device.online ? 'is-online' : 'is-offline'">{{ device.statusText }}</div>
          </div>
          <div class="mobile-status-pill" :class="device.online ? 'is-online' : 'is-offline'">{{ device.online ? '在线' : '离线' }}</div>
        </article>
      </div>
    </section>
  </div>

  <div v-else class="home-page">
    <section class="home-hero">
      <div class="home-hero-kicker">今日概览</div>
      <h1 class="home-hero-title">你好，{{ username }} 👋</h1>
      <p class="home-hero-description">智慧生活，尽在掌控。</p>
      <div class="home-hero-actions">
        <button type="button" class="home-action-chip home-action-chip-light" @click="router.push('/devices')">▦ 设备列表</button>
        <button type="button" class="home-action-chip home-action-chip-ghost" @click="router.push('/activate')">⚡ 激活授权</button>
      </div>
    </section>

    <section class="home-overview">
      <div class="home-overview-head">
        <div class="home-overview-copy">
          <h2 class="home-overview-title">首页总览</h2>
          <p class="home-overview-subtitle">快速查看设备状态与最近活跃设备。</p>
        </div>
        <div class="home-overview-actions">
          <button
            type="button"
            class="home-toolbar-button home-toolbar-button-light"
            :disabled="refreshing"
            @click.stop="refreshData"
          >
            {{ refreshing ? '刷新中...' : '↻ 刷新数据' }}
          </button>
          <button type="button" class="home-toolbar-button home-toolbar-button-primary" @click.stop="router.push('/devices')">→ 管理设备</button>
        </div>
      </div>

      <div class="home-stats-grid">
        <article
          v-for="stat in stats"
          :key="stat.label"
          class="home-stat-card"
          @click="router.push(stat.status === 'recent' ? '/devices' : { path: '/devices', query: { status: stat.status } })"
        >
          <div class="home-stat-label" :class="`is-${stat.tone}`">{{ stat.label }}</div>
          <div class="home-stat-value">{{ stat.value }}</div>
        </article>
      </div>
    </section>

    <section class="home-recent">
      <div class="home-recent-head">
        <h3 class="home-recent-title">最近设备</h3>
        <button type="button" class="home-recent-link" @click="router.push('/devices')">查看全部</button>
      </div>

      <div v-if="recentDevices.length === 0" class="ui-empty">
        <div class="ui-empty-emoji">□</div>
        <div>当前还没有已激活设备，先去完成授权激活吧。</div>
        <button type="button" class="ui-button primary" @click="router.push('/activate')">前往激活</button>
      </div>

      <div v-else class="home-recent-grid">
        <article
          v-for="device in recentDevices"
          :key="device.deviceId"
          class="home-device-card"
          @click="router.push(`/devices/${device.deviceId}`)"
        >
          <div class="home-device-icon" :class="device.online ? 'is-online' : 'is-offline'">
            <CachedImage
              v-if="device.productIcon"
              :src="device.productIcon"
              :cache-key="`product-icon:${device.productKey}`"
              :alt="device.title"
              class="home-device-image"
            >
              <template #fallback>
                <span>🤖</span>
              </template>
            </CachedImage>
            <span v-else>🤖</span>
          </div>
          <div class="home-device-copy">
            <div class="home-device-name">{{ device.title }}</div>
            <div class="home-device-subtitle">{{ device.subtitle }}</div>
            <div class="home-device-status" :class="device.online ? 'is-online' : 'is-offline'">{{ device.statusText }}</div>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>
