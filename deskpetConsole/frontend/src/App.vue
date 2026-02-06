<script setup lang="ts">
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
  <div class="app-shell">
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
        <div class="eyebrow">运行状态</div>
        <div class="status-row">
          <span class="status-dot online"></span>
          <span>控制台就绪</span>
        </div>
        <div class="status-note">等待实时通道接入</div>
      </div>
    </aside>

    <main class="main-content">
      <RouterView />
    </main>
  </div>
</template>
