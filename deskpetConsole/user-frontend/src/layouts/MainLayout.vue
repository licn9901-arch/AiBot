<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Tabbar as VanTabbar, TabbarItem as VanTabbarItem } from 'vant'
import { useResponsive } from '@/composables/useResponsive'
import { useUserStore } from '@/stores/user'

const { isMobile } = useResponsive()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const navItems = [
  { label: '首页', name: 'home', path: '/home', icon: 'wap-home-o' },
  { label: '设备', name: 'devices', path: '/devices', icon: 'desktop-o' },
  { label: '激活', name: 'activate', path: '/activate', icon: 'certificate' },
  { label: '我的', name: 'profile', path: '/profile', icon: 'user-o' }
]

const activeTab = computed(() => {
  const matched = navItems.find(item => route.path.startsWith(item.path))
  return matched ? matched.name : 'home'
})

function onTabChange(name: string | number) {
  const item = navItems.find(n => n.name === name)
  if (item) router.push(item.path)
}

function handleLogout() {
  userStore.logout()
}
</script>

<template>
  <!-- PC 端布局 -->
  <div v-if="!isMobile" class="app-layout">
    <aside class="app-sidebar">
      <div class="brand">
        <div class="brand-mark"></div>
        <div>
          <div class="brand-title">DeskPet</div>
          <div class="brand-subtitle">桌宠控制中心</div>
        </div>
      </div>

      <nav class="sidebar-nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.name"
          :to="item.path"
          class="nav-link"
          active-class="is-active"
        >
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="sidebar-user">
        <div class="user-name">{{ userStore.userInfo?.username || '用户' }}</div>
        <div class="user-email">{{ userStore.userInfo?.email || '' }}</div>
        <el-button
          type="danger"
          text
          size="small"
          style="margin-top: 8px; padding: 0;"
          @click="handleLogout"
        >
          退出登录
        </el-button>
      </div>
    </aside>

    <main class="app-main">
      <RouterView />
    </main>
  </div>

  <!-- 移动端布局 -->
  <div v-else>
    <main class="mobile-content">
      <RouterView />
    </main>

    <VanTabbar :model-value="activeTab" @update:model-value="onTabChange" fixed placeholder>
      <VanTabbarItem
        v-for="item in navItems"
        :key="item.name"
        :name="item.name"
        :icon="item.icon"
      >
        {{ item.label }}
      </VanTabbarItem>
    </VanTabbar>
  </div>
</template>
