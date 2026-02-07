<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Tab as VanTab, Tabs as VanTabs, Button as VanButton, Field as VanField,
  CellGroup as VanCellGroup, Cell as VanCell, Loading as VanLoading, showToast
} from 'vant'
import * as echarts from 'echarts'
import { useResponsive } from '@/composables/useResponsive'
import { usePolling } from '@/composables/usePolling'
import { useDeviceWebSocket } from '@/composables/useWebSocket'
import { getDevice, getDeviceTelemetryHistory, getDeviceEvents } from '@/api/device'
import { sendCommand } from '@/api/command'
import type { DeviceResponse, TelemetryHistory, DeviceEventResponse } from '@/types/device'
import type { CommandResponse } from '@/types/command'
import { formatTime } from '@/utils/format'

const { isMobile } = useResponsive()
const route = useRoute()
const deviceId = route.params.deviceId as string

const device = ref<DeviceResponse | null>(null)
const activeTab = ref('control')
const loading = ref(true)

// WebSocket 实时通信
const { commandStatus, presence, connect: wsConnect } = useDeviceWebSocket(deviceId)

// 控制相关
const presetCommands = [
  { label: '前进', type: 'move', payload: { direction: 'forward' } },
  { label: '后退', type: 'move', payload: { direction: 'backward' } },
  { label: '左转', type: 'move', payload: { direction: 'left' } },
  { label: '右转', type: 'move', payload: { direction: 'right' } },
  { label: '说话', type: 'speak', payload: { text: 'Hello!' } },
  { label: 'LED 开', type: 'led', payload: { state: 'on' } },
  { label: 'LED 关', type: 'led', payload: { state: 'off' } },
  { label: '停止', type: 'move', payload: { direction: 'stop' } }
]
const customType = ref('')
const customPayload = ref('{}')
const commandLoading = ref(false)
const lastCommand = ref<CommandResponse | null>(null)

// 遥测相关
const telemetryHistory = ref<TelemetryHistory[]>([])
const chartRef = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null

// 事件相关
const events = ref<DeviceEventResponse[]>([])

// 监听 WebSocket 指令状态推送，自动更新 lastCommand
watch(commandStatus, (newStatus) => {
  if (newStatus && lastCommand.value && newStatus.reqId === lastCommand.value.reqId) {
    lastCommand.value = newStatus
  }
})

// 监听 WebSocket 设备上下线推送，实时更新在线状态
watch(presence, (newPresence) => {
  if (newPresence && device.value) {
    device.value = { ...device.value, online: newPresence.online }
  }
})

async function fetchDevice() {
  try {
    device.value = await getDevice(deviceId)
  } catch {
    // 错误已处理
  } finally {
    loading.value = false
  }
}

async function handleSendCommand(type: string, payload: Record<string, any>) {
  commandLoading.value = true
  lastCommand.value = null
  try {
    const cmd = await sendCommand(deviceId, { type, payload })
    lastCommand.value = cmd
    const msg = '指令已下发'
    isMobile.value ? showToast(msg) : ElMessage.success(msg)
    // 指令状态通过 WebSocket 实时推送，无需轮询
  } catch {
    // 错误已处理
  } finally {
    commandLoading.value = false
  }
}

async function handleCustomCommand() {
  if (!customType.value) {
    const msg = '请输入指令类型'
    isMobile.value ? showToast(msg) : ElMessage.warning(msg)
    return
  }
  try {
    const payload = JSON.parse(customPayload.value)
    await handleSendCommand(customType.value, payload)
  } catch {
    const msg = 'Payload 必须是合法的 JSON'
    isMobile.value ? showToast(msg) : ElMessage.error(msg)
  }
}

async function fetchTelemetry() {
  try {
    telemetryHistory.value = await getDeviceTelemetryHistory(deviceId, 24)
    renderChart()
  } catch {
    // 错误已处理
  }
}

function renderChart() {
  if (!chartRef.value || telemetryHistory.value.length === 0) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  const times = telemetryHistory.value.map(t => formatTime(t.createdAt))
  // 提取所有遥测 key
  const allKeys = new Set<string>()
  telemetryHistory.value.forEach(t => {
    Object.keys(t.telemetry).forEach(k => allKeys.add(k))
  })

  const series = Array.from(allKeys).map(key => ({
    name: key,
    type: 'line' as const,
    smooth: true,
    data: telemetryHistory.value.map(t => t.telemetry[key] ?? null)
  }))

  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: Array.from(allKeys) },
    xAxis: { type: 'category', data: times },
    yAxis: { type: 'value' },
    series,
    grid: { left: 40, right: 20, top: 40, bottom: 30 }
  })
}

async function fetchEvents() {
  try {
    events.value = await getDeviceEvents(deviceId)
  } catch {
    // 错误已处理
  }
}

function onTabChange(tab: string) {
  activeTab.value = tab
  if (tab === 'telemetry') fetchTelemetry()
  if (tab === 'events') fetchEvents()
}

usePolling(fetchDevice, 10000)

onMounted(() => {
  fetchDevice()
  wsConnect()
})
</script>

<template>
  <div>
    <!-- 加载中 -->
    <div v-if="loading" style="text-align: center; padding: 60px 0;">
      <template v-if="isMobile">
        <VanLoading type="spinner" color="var(--primary)" />
      </template>
      <template v-else>
        <div style="color: var(--muted);">加载中...</div>
      </template>
    </div>

    <template v-else-if="device">
      <!-- 设备基本信息 -->
      <div class="card" style="margin-bottom: 20px;">
        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px;">
          <h1 class="page-title" style="margin-bottom: 0;">{{ device.remark || device.deviceId }}</h1>
          <span class="tag" :class="device.online ? 'success' : 'danger'">
            {{ device.online ? '在线' : '离线' }}
          </span>
        </div>
        <div style="font-size: 13px; color: var(--muted);">
          SN: {{ device.deviceId }} · 产品: {{ device.model || device.productKey }}
        </div>
      </div>

      <!-- 移动端 Tab -->
      <template v-if="isMobile">
        <VanTabs :active="activeTab" @update:active="onTabChange" shrink sticky>
          <VanTab title="控制" name="control">
            <div style="padding: 16px 0;">
              <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; margin-bottom: 24px;">
                <VanButton
                  v-for="cmd in presetCommands"
                  :key="cmd.type + JSON.stringify(cmd.payload)"
                  type="primary"
                  plain
                  round
                  size="small"
                  :loading="commandLoading"
                  @click="handleSendCommand(cmd.type, cmd.payload)"
                >
                  {{ cmd.label }}
                </VanButton>
              </div>

              <h3 style="font-size: 14px; font-weight: 600; margin-bottom: 12px;">自定义指令</h3>
              <VanCellGroup inset>
                <VanField v-model="customType" label="类型" placeholder="指令类型" />
                <VanField v-model="customPayload" label="Payload" placeholder='{"key":"value"}' type="textarea" rows="2" />
              </VanCellGroup>
              <div style="margin: 16px;">
                <VanButton round block type="primary" :loading="commandLoading" color="var(--primary)" @click="handleCustomCommand">
                  发送指令
                </VanButton>
              </div>

              <div v-if="lastCommand" class="card" style="margin-top: 16px;">
                <div style="font-size: 13px; color: var(--muted); margin-bottom: 4px;">指令结果</div>
                <div>状态: <span class="tag" :class="lastCommand.status === 'ACKED' ? 'success' : lastCommand.status === 'FAILED' ? 'danger' : 'warning'">{{ lastCommand.status }}</span></div>
                <div v-if="lastCommand.error" style="color: #dc2626; font-size: 13px; margin-top: 4px;">{{ lastCommand.error }}</div>
                <div v-if="lastCommand.ackPayload" style="font-size: 12px; color: var(--muted); margin-top: 4px;">
                  <pre style="white-space: pre-wrap;">{{ JSON.stringify(lastCommand.ackPayload, null, 2) }}</pre>
                </div>
              </div>
            </div>
          </VanTab>

          <VanTab title="遥测" name="telemetry">
            <div style="padding: 16px 0;">
              <div v-if="device.telemetry" class="card" style="margin-bottom: 16px;">
                <div style="font-size: 13px; color: var(--muted); margin-bottom: 8px;">最新遥测</div>
                <div v-for="(val, key) in device.telemetry" :key="key" style="display: flex; justify-content: space-between; padding: 4px 0; font-size: 14px;">
                  <span>{{ key }}</span>
                  <span style="font-weight: 600;">{{ val }}</span>
                </div>
              </div>
              <div ref="chartRef" style="width: 100%; height: 250px;"></div>
              <div v-if="telemetryHistory.length === 0" class="empty-state">
                <div class="empty-text">暂无遥测数据</div>
              </div>
            </div>
          </VanTab>

          <VanTab title="事件" name="events">
            <div style="padding: 16px 0;">
              <div v-if="events.length === 0" class="empty-state">
                <div class="empty-text">暂无事件记录</div>
              </div>
              <VanCellGroup v-else inset>
                <VanCell
                  v-for="evt in events"
                  :key="evt.id"
                  :title="evt.eventType"
                  :label="formatTime(evt.createdAt)"
                >
                  <template #value>
                    <span style="font-size: 12px; color: var(--muted);">{{ evt.params ? JSON.stringify(evt.params) : '-' }}</span>
                  </template>
                </VanCell>
              </VanCellGroup>
            </div>
          </VanTab>
        </VanTabs>
      </template>

      <!-- PC 端 Tab -->
      <template v-else>
        <el-tabs v-model="activeTab" @tab-change="onTabChange">
          <el-tab-pane label="控制" name="control">
            <div style="display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 24px;">
              <el-button
                v-for="cmd in presetCommands"
                :key="cmd.type + JSON.stringify(cmd.payload)"
                round
                :loading="commandLoading"
                @click="handleSendCommand(cmd.type, cmd.payload)"
              >
                {{ cmd.label }}
              </el-button>
            </div>

            <div class="card" style="max-width: 500px;">
              <h3 style="font-size: 15px; font-weight: 600; margin-bottom: 16px;">自定义指令</h3>
              <el-form label-position="top" @submit.prevent="handleCustomCommand">
                <el-form-item label="指令类型">
                  <el-input v-model="customType" placeholder="例如: move, speak, led" />
                </el-form-item>
                <el-form-item label="Payload (JSON)">
                  <el-input v-model="customPayload" type="textarea" :rows="3" placeholder='{"key":"value"}' />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" round native-type="submit" :loading="commandLoading" style="background: var(--primary); border-color: var(--primary);">
                    发送指令
                  </el-button>
                </el-form-item>
              </el-form>
            </div>

            <div v-if="lastCommand" class="card" style="margin-top: 20px; max-width: 500px;">
              <div style="font-size: 13px; color: var(--muted); margin-bottom: 8px;">指令执行结果</div>
              <div style="margin-bottom: 4px;">
                请求 ID: <code>{{ lastCommand.reqId }}</code>
              </div>
              <div style="margin-bottom: 4px;">
                状态: <span class="tag" :class="lastCommand.status === 'ACKED' ? 'success' : lastCommand.status === 'FAILED' ? 'danger' : 'warning'">{{ lastCommand.status }}</span>
              </div>
              <div v-if="lastCommand.error" style="color: #dc2626; font-size: 13px;">错误: {{ lastCommand.error }}</div>
              <div v-if="lastCommand.ackPayload" style="margin-top: 8px;">
                <div style="font-size: 12px; color: var(--muted);">响应:</div>
                <pre style="background: var(--surface-2); padding: 8px; border-radius: 8px; font-size: 12px; white-space: pre-wrap;">{{ JSON.stringify(lastCommand.ackPayload, null, 2) }}</pre>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="遥测" name="telemetry">
            <div v-if="device.telemetry" class="card" style="margin-bottom: 20px;">
              <h3 style="font-size: 15px; font-weight: 600; margin-bottom: 12px;">最新遥测数据</h3>
              <div class="grid-3">
                <div v-for="(val, key) in device.telemetry" :key="key" style="text-align: center;">
                  <div style="font-size: 24px; font-weight: 700; color: var(--primary);">{{ val }}</div>
                  <div style="font-size: 13px; color: var(--muted); margin-top: 4px;">{{ key }}</div>
                </div>
              </div>
            </div>
            <div class="card">
              <h3 style="font-size: 15px; font-weight: 600; margin-bottom: 12px;">历史趋势</h3>
              <div ref="chartRef" style="width: 100%; height: 350px;"></div>
              <div v-if="telemetryHistory.length === 0" class="empty-state">
                <div class="empty-text">暂无遥测历史数据</div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="事件" name="events">
            <div v-if="events.length === 0" class="empty-state">
              <div class="empty-text">暂无事件记录</div>
            </div>
            <el-table v-else :data="events" stripe style="width: 100%;">
              <el-table-column prop="eventType" label="事件类型" width="180" />
              <el-table-column prop="createdAt" label="时间" width="200">
                <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
              </el-table-column>
              <el-table-column label="参数">
                <template #default="{ row }">
                  <code v-if="row.params" style="font-size: 12px;">{{ JSON.stringify(row.params) }}</code>
                  <span v-else style="color: var(--muted);">-</span>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </template>
    </template>
  </div>
</template>
