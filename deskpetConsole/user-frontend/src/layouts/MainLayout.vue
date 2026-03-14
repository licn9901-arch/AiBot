<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import AppBrand from '@/components/ui/AppBrand.vue'
import { useResponsive } from '@/composables/useResponsive'
import { useUserStore } from '@/stores/user'
import { useDeviceStore } from '@/stores/device'

const route = useRoute()
const router = useRouter()
const { isMobile } = useResponsive()
const userStore = useUserStore()
const deviceStore = useDeviceStore()

const navItems = [
  { label: '首页', name: 'home', path: '/home', icon: '◰' },
  { label: '设备', name: 'devices', path: '/devices', icon: '▦' },
  { label: '激活', name: 'activate', path: '/activate', icon: '⚡' },
  { label: '我的', name: 'profile', path: '/profile', icon: '◎' },
] as const

const activeName = computed(() => {
  const matched = navItems.find((item) => route.path.startsWith(item.path))
  return matched?.name ?? 'home'
})

const mobileActiveName = computed(() => {
  if (route.name === 'device-control') {
    return ''
  }

  const matched = navItems.find((item) => item.name === route.name)
  return matched?.name ?? ''
})

const userDisplayName = computed(() => userStore.userInfo?.username || '用户')
const userSubtitle = computed(() => userStore.userInfo?.email || '连接你的 Cubee 设备')
const userInitial = computed(() => userDisplayName.value.trim().charAt(0).toUpperCase() || 'C')
const mobileSectionLabel = computed(() => {
  switch (route.name) {
    case 'devices':
      return '统一设备管理'
    case 'device-control':
      return '设备实时控制'
    case 'activate':
      return '设备授权与激活'
    case 'profile':
      return '个人资料与授权码'
    default:
      return '设备状态总览'
  }
})
const onlineRatio = computed(() => {
  if (deviceStore.devices.length === 0) return '0%'
  return `${Math.round((deviceStore.devices.filter((item) => item.online).length / deviceStore.devices.length) * 100)}%`
})

function goTo(path: string) {
  router.push(path)
}

onMounted(async () => {
  await userStore.fetchUser()
  if (deviceStore.devices.length === 0) {
    await deviceStore.fetchDevices()
  }
})
</script>

<template>
  <div v-if="!isMobile" class="desktop-shell">
    <header class="desktop-topbar console-topbar">
      <AppBrand />
      <div class="console-topbar-actions">
        <div class="console-user-chip">{{ userSubtitle }}</div>
        <button type="button" class="console-avatar-button" @click="goTo('/profile')">{{ userInitial }}</button>
      </div>
    </header>

    <div class="desktop-body">
      <aside class="desktop-sidebar console-sidebar">
        <div class="console-sidebar-label">导航</div>
        <nav class="console-nav">
          <RouterLink
            v-for="item in navItems"
            :key="item.name"
            :to="item.path"
            class="console-nav-item"
            :class="activeName === item.name ? 'is-active' : ''"
          >
            <span class="console-nav-icon">{{ item.icon }}</span>
            <span>{{ item.label }}</span>
          </RouterLink>
        </nav>

        <div class="console-online-card">
          <div class="console-online-label">在线率</div>
          <div class="console-online-value">{{ onlineRatio }}</div>
          <div class="console-online-meta">{{ deviceStore.devices.filter((item) => item.online).length }} / {{ deviceStore.devices.length }} 台设备在线</div>
        </div>
      </aside>

      <main class="desktop-main">
        <div class="desktop-page">
          <RouterView />
        </div>
      </main>
    </div>
  </div>

  <div v-else class="mobile-shell">
    <header class="mobile-topbar mobile-app-header">
      <div class="mobile-brand-row">
        <div class="mobile-brand-copy">
          <div class="mobile-brand-title">Cubee</div>
          <div class="mobile-brand-subtitle">{{ userDisplayName }} · {{ mobileSectionLabel }}</div>
        </div>
        <button type="button" class="mobile-brand-action" @click="goTo('/profile')">↗</button>
      </div>
    </header>

    <main class="mobile-main">
      <RouterView />
    </main>

    <div class="mobile-tabbar-wrap">
      <nav class="mobile-tabbar">
        <button
          v-for="item in navItems"
          :key="item.name"
          type="button"
          class="mobile-tabbar-item"
          :class="mobileActiveName === item.name ? 'active' : ''"
          @click="goTo(item.path)"
        >
          <span style="font-size: 16px; line-height: 1;">{{ item.icon }}</span>
          <span>{{ item.label }}</span>
        </button>
      </nav>
    </div>
  </div>
</template>
