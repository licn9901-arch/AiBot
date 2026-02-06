<template>
  <div class="page">
    <header class="page-header">
      <div>
        <el-button link @click="$router.back()">← 返回列表</el-button>
        <h1 class="page-title">{{ deviceId }}</h1>
      </div>
      <div class="header-actions">
        <el-button type="primary">发送指令</el-button>
        <el-button>刷新</el-button>
      </div>
    </header>

    <div v-loading="loading" v-if="deviceData">
      <div class="cards-grid">
        <!-- 基础信息 -->
        <el-card>
          <template #header>
            <div class="card-header">
              <span>基础信息</span>
            </div>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="设备ID">{{ deviceData.device.deviceId }}</el-descriptions-item>
            <el-descriptions-item label="型号">{{ deviceData.device.model }}</el-descriptions-item>
            <el-descriptions-item label="备注">{{ deviceData.device.remark || '-' }}</el-descriptions-item>
            <el-descriptions-item label="密钥">
              <span class="blur-text">******</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 在线状态 -->
        <el-card>
          <template #header>
            <div class="card-header">
              <span>在线状态</span>
              <el-tag :type="deviceData.session?.connected ? 'success' : 'info'">
                {{ deviceData.session?.connected ? '在线' : '离线' }}
              </el-tag>
            </div>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="连接时间" v-if="deviceData.session?.connected">
              {{ formatTime(deviceData.session?.connectedAt) }}
            </el-descriptions-item>
            <el-descriptions-item label="断开时间" v-else>
              {{ formatTime(deviceData.session?.disconnectedAt) }}
            </el-descriptions-item>
            <el-descriptions-item label="客户端IP">
              {{ deviceData.session?.clientIp || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 最新遥测 -->
        <el-card class="full-width">
          <template #header>
            <div class="card-header">
              <span>最新遥测数据</span>
              <span class="text-sm text-gray">
                {{ formatTime(deviceData.telemetry?.ts) }}
              </span>
            </div>
          </template>
          <pre class="json-block" v-if="deviceData.telemetry">{{ JSON.stringify(deviceData.telemetry.payload, null, 2) }}</pre>
          <el-empty v-else description="暂无遥测数据" />
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { getDevice } from '@/api/device'
import type { DeviceResponse } from '@/types/device'

import { usePolling } from '@/composables/usePolling'

const route = useRoute()
const deviceId = route.params.id as string
const loading = ref(false)
const deviceData = ref<DeviceResponse | null>(null)
usePolling(loadData, 3000)

function formatTime(ts?: number) {
  if (!ts) return '-'
  return new Date(ts).toLocaleString()
}

async function loadData() {
  if (!deviceData.value) {
    loading.value = true
  }
  try {
    deviceData.value = await getDevice(deviceId)
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}
// Polling starts automatically

</script>

<style scoped>
.cards-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}
.full-width {
  grid-column: span 2;
}
.json-block {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  font-family: monospace;
}
.blur-text {
  filter: blur(4px);
  cursor: pointer;
  transition: filter 0.3s;
}
.blur-text:hover {
  filter: none;
}
</style>
