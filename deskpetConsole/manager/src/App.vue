<script setup lang="ts">
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()

const navItems = [
  { label: '总览', path: '/' },
  { label: '授权码', path: '/licenses' },
  { label: '产品', path: '/products' },
  { label: '设备', path: '/devices' },
  { label: '指令', path: '/commands' },
  { label: '遥测', path: '/telemetry' },
  { label: '日志', path: '/logs' },
  { label: '设置', path: '/settings' }
]
</script>

<template>
  <!-- 登录页：无侧边栏 -->
  <RouterView v-if="route.name === 'login'" />

  <!-- 主布局：侧边栏 + 内容 -->
  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark"></div>
        <div>
          <div class="brand-title">DeskPet Console</div>
          <div class="brand-subtitle">桌宠控制中心</div>
        </div>
      </div>

      <nav class="nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-link"
          active-class="is-active"
        >
          <span class="nav-indicator"></span>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="sidebar-card">
        <div class="eyebrow">当前用户</div>
        <div class="status-row">
          <span class="status-dot online"></span>
          <span>{{ userStore.username || '管理员' }}</span>
        </div>
        <div class="status-note" v-if="userStore.roles.length">
          {{ userStore.roles.join(', ') }}
        </div>
        <el-button
          type="danger"
          text
          size="small"
          style="margin-top: 8px; padding: 0; font-size: 12px;"
          @click="userStore.logout()"
        >
          退出登录
        </el-button>
      </div>
    </aside>

    <main class="main-content">
      <RouterView />
    </main>
  </div>
</template>
