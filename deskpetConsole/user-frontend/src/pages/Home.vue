<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Button as VanButton, Cell as VanCell, CellGroup as VanCellGroup } from 'vant'
import { useResponsive } from '@/composables/useResponsive'
import { useUserStore } from '@/stores/user'
import { useDeviceStore } from '@/stores/device'
import { formatRelativeTime } from '@/utils/format'

const { isMobile } = useResponsive()
const router = useRouter()
const userStore = useUserStore()
const deviceStore = useDeviceStore()

const onlineCount = computed(() => deviceStore.devices.filter(d => d.online).length)
const recentDevices = computed(() => deviceStore.devices.slice(0, 3))

onMounted(async () => {
  await userStore.fetchUser()
  await deviceStore.fetchDevices()
})
</script>

<template>
  <div>
    <h1 class="page-title">
      你好，{{ userStore.userInfo?.username || '用户' }}
    </h1>

    <!-- 统计卡片 -->
    <div :class="isMobile ? '' : 'grid-3'" style="margin-bottom: 24px;">
      <div class="card" style="text-align: center; cursor: pointer;" @click="router.push('/devices')">
        <div style="font-size: 32px; font-weight: 700; color: var(--primary);">{{ deviceStore.devices.length }}</div>
        <div style="color: var(--muted); font-size: 14px; margin-top: 4px;">设备总数</div>
      </div>
      <div class="card" style="text-align: center; margin-top: 12px;" :style="!isMobile && 'margin-top: 0'">
        <div style="font-size: 32px; font-weight: 700; color: var(--secondary);">{{ onlineCount }}</div>
        <div style="color: var(--muted); font-size: 14px; margin-top: 4px;">在线设备</div>
      </div>
      <div class="card" style="text-align: center; cursor: pointer; margin-top: 12px;" :style="!isMobile && 'margin-top: 0'" @click="router.push('/activate')">
        <div style="font-size: 32px; font-weight: 700; color: #f59e0b;">+</div>
        <div style="color: var(--muted); font-size: 14px; margin-top: 4px;">激活授权码</div>
      </div>
    </div>

    <!-- 最近设备 -->
    <h2 style="font-size: 16px; font-weight: 600; margin-bottom: 12px;">最近设备</h2>

    <div v-if="recentDevices.length === 0" class="empty-state">
      <div class="empty-text">暂无设备，去激活授权码添加设备吧</div>
      <template v-if="isMobile">
        <VanButton type="primary" round size="small" color="var(--primary)" @click="router.push('/activate')">
          激活授权码
        </VanButton>
      </template>
      <template v-else>
        <el-button type="primary" round @click="router.push('/activate')" style="background: var(--primary); border-color: var(--primary);">
          激活授权码
        </el-button>
      </template>
    </div>

    <!-- 移动端设备列表 -->
    <template v-if="isMobile && recentDevices.length > 0">
      <VanCellGroup inset>
        <VanCell
          v-for="device in recentDevices"
          :key="device.deviceId"
          :title="device.remark || device.deviceId"
          :label="device.model || device.productKey"
          is-link
          @click="router.push(`/devices/${device.deviceId}`)"
        >
          <template #right-icon>
            <span class="status-dot" :class="device.online ? 'online' : 'offline'" style="margin-right: 8px;"></span>
          </template>
        </VanCell>
      </VanCellGroup>
      <div style="text-align: center; margin-top: 16px;">
        <VanButton plain round size="small" @click="router.push('/devices')">查看全部设备</VanButton>
      </div>
    </template>

    <!-- PC 端设备卡片 -->
    <template v-if="!isMobile && recentDevices.length > 0">
      <div class="grid-3">
        <div
          v-for="device in recentDevices"
          :key="device.deviceId"
          class="card"
          style="cursor: pointer;"
          @click="router.push(`/devices/${device.deviceId}`)"
        >
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px;">
            <span style="font-weight: 600;">{{ device.remark || device.deviceId }}</span>
            <span class="status-dot" :class="device.online ? 'online' : 'offline'"></span>
          </div>
          <div style="font-size: 13px; color: var(--muted);">{{ device.model || device.productKey }}</div>
          <div style="font-size: 12px; color: var(--muted); margin-top: 8px;">
            {{ device.online ? '在线' : `最后在线 ${formatRelativeTime(device.lastSeen)}` }}
          </div>
        </div>
      </div>
      <div style="text-align: center; margin-top: 16px;">
        <el-button round @click="router.push('/devices')">查看全部设备</el-button>
      </div>
    </template>
  </div>
</template>
