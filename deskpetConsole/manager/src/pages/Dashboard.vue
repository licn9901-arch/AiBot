<template>
  <div class="page">
    <header class="page-header">
      <div>
        <div class="eyebrow">控制台总览</div>
        <h1 class="page-title">桌宠运行态势</h1>
        <p class="page-subtitle">设备状态、事件与产品管理一览。</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadAll">刷新</el-button>
      </div>
    </header>

    <section class="metrics-grid" v-loading="loading">
      <div class="card">
        <div class="metric-value">{{ onlineCount }}</div>
        <div class="metric-label">在线设备</div>
        <div class="metric-note">共 {{ devices.length }} 台设备</div>
      </div>
      <div class="card">
        <div class="metric-value">{{ devices.length - onlineCount }}</div>
        <div class="metric-label">离线设备</div>
        <div class="metric-note">{{ offlineRate }}</div>
      </div>
      <div class="card">
        <div class="metric-value">{{ recentEventCount }}</div>
        <div class="metric-label">近24h事件</div>
        <div class="metric-note">{{ alertEventCount }} 条告警/故障</div>
      </div>
      <div class="card">
        <div class="metric-value">{{ productCount }}</div>
        <div class="metric-label">产品型号</div>
        <div class="metric-note">物模型定义</div>
      </div>
    </section>

    <section class="grid-2">
      <div class="card">
        <div class="eyebrow">设备概览</div>
        <h2>设备列表</h2>
        <ul class="data-list" v-if="devices.length > 0">
          <li v-for="device in displayDevices" :key="device.deviceId">
            <div>
              <div class="item-title">{{ device.deviceId }}<template v-if="device.remark"> · {{ device.remark }}</template></div>
              <div class="item-subtitle">{{ device.lastSeen ? formatTime(device.lastSeen) : '暂无心跳记录' }}</div>
            </div>
            <span :class="['status', { idle: !device.online }]">{{ device.online ? '在线' : '离线' }}</span>
          </li>
        </ul>
        <p v-else class="placeholder">暂无设备</p>
        <div style="margin-top: 16px;">
          <el-button link type="primary" @click="$router.push('/devices')">查看全部设备</el-button>
        </div>
      </div>
      <div class="card">
        <div class="eyebrow">最近事件</div>
        <h2>设备事件</h2>
        <ul class="data-list" v-if="recentEvents.length > 0">
          <li v-for="event in recentEvents" :key="event.id">
            <div>
              <div class="item-title">{{ event.deviceId }} · {{ event.eventId || event.event_id }}</div>
              <div class="item-subtitle">{{ formatTime(event.createdAt || event.created_at) }}</div>
            </div>
            <span :class="['status', eventTypeClass(event.eventType || event.event_type)]">{{ event.eventType || event.event_type }}</span>
          </li>
        </ul>
        <p v-else class="placeholder">近24小时无事件</p>
        <div style="margin-top: 16px;">
          <el-button link type="primary" @click="$router.push('/logs')">查看全部日志</el-button>
        </div>
      </div>
    </section>

    <!-- 网关 Metrics -->
    <section class="card gateway-metrics-card" v-loading="gwLoading">
      <div class="eyebrow">MQTT 网关</div>
      <h2>网关运行指标</h2>
      <div v-if="gwError" class="placeholder">{{ gwError }}</div>
      <template v-else-if="gw">
        <div class="gw-grid">
          <div class="gw-item">
            <div class="gw-value">{{ gw.deskpet_gateway_online ?? '-' }}</div>
            <div class="gw-label">在线连接</div>
          </div>
          <div class="gw-item">
            <div class="gw-value">{{ formatUptime(gw.deskpet_gateway_uptime_seconds) }}</div>
            <div class="gw-label">运行时长</div>
          </div>
          <div class="gw-item">
            <div class="gw-value">{{ gw.deskpet_gateway_connect_total ?? 0 }}</div>
            <div class="gw-label">连接次数</div>
          </div>
          <div class="gw-item">
            <div class="gw-value">{{ gw.deskpet_gateway_disconnect_total ?? 0 }}</div>
            <div class="gw-label">断开次数</div>
          </div>
          <div class="gw-item">
            <div class="gw-value">{{ gw.deskpet_gateway_telemetry_total ?? 0 }}</div>
            <div class="gw-label">遥测上报</div>
          </div>
          <div class="gw-item">
            <div class="gw-value">{{ gw.deskpet_gateway_event_total ?? 0 }}</div>
            <div class="gw-label">事件上报</div>
          </div>
          <div class="gw-item">
            <div class="gw-value">{{ gw.deskpet_gateway_ack_total ?? 0 }}</div>
            <div class="gw-label">指令回执</div>
          </div>
          <div class="gw-item">
            <div class="gw-value">{{ gw.deskpet_gateway_command_send_total ?? 0 }}</div>
            <div class="gw-label">指令下发</div>
          </div>
          <div class="gw-item">
            <div class="gw-value success">{{ gw.deskpet_gateway_command_send_ok_total ?? 0 }}</div>
            <div class="gw-label">下发成功</div>
          </div>
          <div class="gw-item">
            <div class="gw-value" :class="{ danger: (gw.deskpet_gateway_command_send_fail_total ?? 0) > 0 }">{{ gw.deskpet_gateway_command_send_fail_total ?? 0 }}</div>
            <div class="gw-label">下发失败</div>
          </div>
          <div class="gw-item">
            <div class="gw-value" :class="{ danger: (gw.deskpet_gateway_auth_fail_total ?? 0) > 0 }">{{ gw.deskpet_gateway_auth_fail_total ?? 0 }}</div>
            <div class="gw-label">鉴权失败</div>
          </div>
          <div class="gw-item">
            <div class="gw-value" :class="{ danger: (gw.deskpet_gateway_callback_fail_total ?? 0) > 0 }">{{ gw.deskpet_gateway_callback_fail_total ?? 0 }}</div>
            <div class="gw-label">回调失败</div>
          </div>
        </div>
      </template>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getDevices } from '@/api/device'
import { getDeviceEventLogs } from '@/api/logs'
import { getProducts } from '@/api/product'
import { getGatewayMetrics } from '@/api/gateway'
import type { DeviceResponse } from '@/types/device'
import type { GatewayMetrics } from '@/api/gateway'

const loading = ref(false)
const devices = ref<DeviceResponse[]>([])
const recentEvents = ref<Record<string, any>[]>([])
const productCount = ref(0)
const recentEventCount = ref(0)
const alertEventCount = ref(0)

const gwLoading = ref(false)
const gw = ref<GatewayMetrics | null>(null)
const gwError = ref('')

const onlineCount = computed(() => devices.value.filter(d => d.online).length)

const offlineRate = computed(() => {
  if (devices.value.length === 0) return '暂无设备'
  const rate = ((devices.value.length - onlineCount.value) / devices.value.length * 100).toFixed(0)
  return `离线率 ${rate}%`
})

// 最多展示 5 台设备，在线的排前面
const displayDevices = computed(() => {
  return [...devices.value]
    .sort((a, b) => {
      if (a.online === b.online) return 0
      return a.online ? -1 : 1
    })
    .slice(0, 5)
})

function formatTime(time: string) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin} 分钟前`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour} 小时前`
  return date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function eventTypeClass(type: string) {
  if (type === 'error') return 'error'
  if (type === 'alert') return 'warning'
  return ''
}

function formatUptime(seconds?: number) {
  if (!seconds) return '-'
  const s = Math.floor(seconds)
  const d = Math.floor(s / 86400)
  const h = Math.floor((s % 86400) / 3600)
  const m = Math.floor((s % 3600) / 60)
  if (d > 0) return `${d}d ${h}h`
  if (h > 0) return `${h}h ${m}m`
  return `${m}m`
}

async function loadAll() {
  loading.value = true
  gwLoading.value = true
  try {
    const [deviceList, eventPage, productList] = await Promise.all([
      getDevices(),
      getDeviceEventLogs({ hours: 24, page: 0, size: 5 }),
      getProducts()
    ])
    devices.value = deviceList
    recentEvents.value = eventPage.content || []
    recentEventCount.value = eventPage.totalElements || 0
    alertEventCount.value = recentEvents.value.filter(
      e => (e.eventType || e.event_type) === 'alert' || (e.eventType || e.event_type) === 'error'
    ).length
    productCount.value = Array.isArray(productList) ? productList.length : 0
  } catch (e) {
    console.error('Failed to load dashboard data', e)
  } finally {
    loading.value = false
  }
  // 网关 metrics 单独加载，失败不影响其他数据
  try {
    gw.value = await getGatewayMetrics()
    gwError.value = ''
  } catch (e) {
    gwError.value = '无法连接到网关，请检查网关是否运行'
    gw.value = null
  } finally {
    gwLoading.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.grid-2 {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.metric-value {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.2;
}
.metric-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-regular);
  margin-top: 4px;
}
.metric-note {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}
.data-list {
  list-style: none;
  padding: 0;
  margin: 12px 0 0;
}
.data-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.data-list li:last-child {
  border-bottom: none;
}
.item-title {
  font-size: 14px;
  font-weight: 500;
}
.item-subtitle {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}
.status {
  font-size: 12px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 10px;
  background: var(--el-color-success-light-9);
  color: var(--el-color-success);
}
.status.idle {
  background: var(--el-fill-color);
  color: var(--el-text-color-secondary);
}
.status.warning {
  background: var(--el-color-warning-light-9);
  color: var(--el-color-warning);
}
.status.error {
  background: var(--el-color-danger-light-9);
  color: var(--el-color-danger);
}
.placeholder {
  color: var(--el-text-color-placeholder);
  font-size: 13px;
  padding: 16px 0;
}
.gateway-metrics-card {
  margin-bottom: 24px;
}
.gw-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  margin-top: 16px;
}
.gw-item {
  text-align: center;
}
.gw-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.3;
}
.gw-value.success {
  color: var(--el-color-success);
}
.gw-value.danger {
  color: var(--el-color-danger);
}
.gw-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
</style>
