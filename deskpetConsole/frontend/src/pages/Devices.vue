<template>
  <div class="page">
    <header class="page-header">
      <div>
        <div class="eyebrow">设备管理</div>
        <h1 class="page-title">设备清单</h1>
        <p class="page-subtitle">管理桌宠设备信息、在线状态与分组。</p>
      </div>
      <div class="header-actions">
        <el-button type="primary">添加设备</el-button>
        <el-button>导出清单</el-button>
      </div>
    </header>

    <div class="card">
      <div class="toolbar">
        <el-input
          v-model="searchQuery"
          placeholder="搜索设备ID或备注"
          clearable
          style="width: 300px"
          prefix-icon="Search"
        />
        <el-button @click="refresh">刷新</el-button>
      </div>

      <el-table :data="filteredDevices" style="width: 100%" v-loading="loading">
        <el-table-column prop="device.deviceId" label="设备ID" width="180" sortable />
        <el-table-column prop="device.model" label="型号" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.session?.connected ? 'success' : 'info'">
              {{ row.session?.connected ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后活跃" width="180">
          <template #default="{ row }">
            {{ formatTime(row.session?.connected ? row.session?.connectedAt : row.session?.disconnectedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="device.remark" label="备注" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="$router.push(`/devices/${row.device.deviceId}`)">详情</el-button>
            <el-button link type="primary" size="small">指令</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { getDevices } from '@/api/device'
import type { DeviceResponse } from '@/types/device'


import { usePolling } from '@/composables/usePolling'

const devices = ref<DeviceResponse[]>([])
const loading = ref(false)
const searchQuery = ref('')
usePolling(refresh, 5000)

const filteredDevices = computed(() => {
  if (!searchQuery.value) return devices.value
  const q = searchQuery.value.toLowerCase()
  return devices.value.filter(
    (d: DeviceResponse) =>
      d.device.deviceId.toLowerCase().includes(q) ||
      d.device.remark?.toLowerCase().includes(q)
  )
})

function formatTime(ts?: number) {
  if (!ts) return '-'
  return new Date(ts).toLocaleString()
}

async function refresh() {
  // Silent refresh if already have data, otherwise show loading
  if (devices.value.length === 0) {
    loading.value = true
  }
  try {
    devices.value = await getDevices()
  } catch (e) {
    console.error('Failed to load devices', e)
  } finally {
    loading.value = false
  }
}
// Polling starts automatically on mount via usePolling hook implementation

</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}
</style>
