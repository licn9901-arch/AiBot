<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Cell as VanCell, CellGroup as VanCellGroup, Button as VanButton, Loading as VanLoading } from 'vant'
import { useResponsive } from '@/composables/useResponsive'
import { useDeviceStore } from '@/stores/device'
import { usePolling } from '@/composables/usePolling'
import { formatRelativeTime } from '@/utils/format'

const { isMobile } = useResponsive()
const router = useRouter()
const deviceStore = useDeviceStore()

usePolling(() => deviceStore.fetchDevices(), 10000)

onMounted(() => {
  if (deviceStore.devices.length === 0) {
    deviceStore.fetchDevices()
  }
})
</script>

<template>
  <div>
    <h1 class="page-title">我的设备</h1>

    <!-- 加载中 -->
    <div v-if="deviceStore.loading && deviceStore.devices.length === 0" style="text-align: center; padding: 60px 0;">
      <template v-if="isMobile">
        <VanLoading type="spinner" color="var(--primary)">加载中...</VanLoading>
      </template>
      <template v-else>
        <el-icon class="is-loading" :size="24" style="color: var(--primary);"><i class="el-icon-loading" /></el-icon>
        <div style="color: var(--muted); margin-top: 8px;">加载中...</div>
      </template>
    </div>

    <!-- 空状态 -->
    <div v-else-if="deviceStore.devices.length === 0" class="empty-state">
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
    <template v-else-if="isMobile">
      <VanCellGroup inset>
        <VanCell
          v-for="device in deviceStore.devices"
          :key="device.deviceId"
          :title="device.remark || device.deviceId"
          :label="`${device.model || device.productKey} · ${device.online ? '在线' : '离线 ' + formatRelativeTime(device.lastSeen)}`"
          is-link
          @click="router.push(`/devices/${device.deviceId}`)"
        >
          <template #right-icon>
            <span class="status-dot" :class="device.online ? 'online' : 'offline'" style="margin-top: 12px;"></span>
          </template>
        </VanCell>
      </VanCellGroup>
    </template>

    <!-- PC 端设备卡片网格 -->
    <template v-else>
      <div class="grid-3">
        <div
          v-for="device in deviceStore.devices"
          :key="device.deviceId"
          class="card"
          style="cursor: pointer; transition: transform 0.2s;"
          @click="router.push(`/devices/${device.deviceId}`)"
          @mouseenter="($event.currentTarget as HTMLElement).style.transform = 'translateY(-2px)'"
          @mouseleave="($event.currentTarget as HTMLElement).style.transform = ''"
        >
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px;">
            <span style="font-weight: 600; font-size: 15px;">{{ device.remark || device.deviceId }}</span>
            <span class="tag" :class="device.online ? 'success' : 'danger'">
              {{ device.online ? '在线' : '离线' }}
            </span>
          </div>
          <div style="font-size: 13px; color: var(--muted);">
            {{ device.model || device.productKey }}
          </div>
          <div style="font-size: 12px; color: var(--muted); margin-top: 8px;">
            SN: {{ device.deviceId }}
          </div>
          <div style="font-size: 12px; color: var(--muted); margin-top: 4px;">
            {{ device.online ? '当前在线' : `最后在线 ${formatRelativeTime(device.lastSeen)}` }}
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
