<template>
  <div class="page">
    <header class="page-header">
      <div>
        <div class="eyebrow">遥测面板</div>
        <h1 class="page-title">实时遥测与趋势</h1>
        <p class="page-subtitle">查看设备历史数据的变化趋势。</p>
      </div>
      <div class="header-actions">
        <el-select v-model="selectedDeviceId" placeholder="选择设备" style="width: 200px" @change="loadHistory">
          <el-option
            v-for="d in devices"
            :key="d.device.deviceId"
            :label="d.device.deviceId + (d.device.remark ? ` (${d.device.remark})` : '')"
            :value="d.device.deviceId"
          />
        </el-select>
        <el-button @click="loadHistory" :disabled="!selectedDeviceId">刷新</el-button>
      </div>
    </header>

    <div class="card" v-loading="loading">
      <div ref="chartRef" style="width: 100%; height: 400px"></div>
      <el-empty v-if="!selectedDeviceId" description="请选择一个设备查看趋势" />
      <el-empty v-else-if="!history.length" description="暂无历史数据" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getDevices, getDeviceTelemetryHistory } from '@/api/device'
import type { DeviceResponse } from '@/types/device'
import type { TelemetryHistory } from '@/api/device'

import { usePolling } from '@/composables/usePolling'

const devices = ref<DeviceResponse[]>([])
const selectedDeviceId = ref('')
const history = ref<TelemetryHistory[]>([])
const loading = ref(false)
const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

const { startPolling } = usePolling(loadHistory, 10000) // Poll every 10s for telemetry

async function loadDevices() {
  try {
    devices.value = await getDevices()
    if (devices.value.length > 0) {
      selectedDeviceId.value = devices.value[0]?.device.deviceId ?? ''
      await loadHistory()
      startPolling()
    }
  } catch (e) {
    console.error(e)
  }
}

async function loadHistory() {
  if (!selectedDeviceId.value) return
  // Only show loading on initial load or manual refresh
  if (history.value.length === 0) {
    loading.value = true
  }
  try {
    history.value = await getDeviceTelemetryHistory(selectedDeviceId.value)
    renderChart()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}
function renderChart() {
  if (!chartRef.value || history.value.length === 0) {
    chart?.clear()
    return
  }
  
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }

  // Extract all keys from telemetry payload to form series
  const keys = new Set<string>()
  history.value.forEach(h => {
    Object.keys(h.telemetry).forEach(k => {
      if (typeof h.telemetry[k] === 'number') {
        keys.add(k)
      }
    })
  })

  const series = Array.from(keys).map(key => ({
    name: key,
    type: 'line',
    data: history.value.map(h => ({
      name: new Date(h.createdAt).toLocaleString(),
      value: [new Date(h.createdAt), h.telemetry[key]]
    })),
    smooth: true
  }))

  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: Array.from(keys) },
    xAxis: { type: 'time' },
    yAxis: { type: 'value' },
    series
  }

  chart.setOption(option)
}

onMounted(() => {
  loadDevices()
  window.addEventListener('resize', () => chart?.resize())
})

onUnmounted(() => {
  window.removeEventListener('resize', () => chart?.resize())
  chart?.dispose()
})
</script>
