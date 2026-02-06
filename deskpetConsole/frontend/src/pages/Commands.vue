<template>
  <div class="page">
    <header class="page-header">
      <div>
        <div class="eyebrow">指令管理</div>
        <h1 class="page-title">指令下发</h1>
        <p class="page-subtitle">向设备发送控制指令并跟踪执行状态。</p>
      </div>
      <div class="header-actions">
        <el-select v-model="selectedDeviceId" placeholder="选择设备" style="width: 200px">
          <el-option
            v-for="d in devices"
            :key="d.device.deviceId"
            :label="d.device.deviceId + (d.device.remark ? ` (${d.device.remark})` : '')"
            :value="d.device.deviceId"
          />
        </el-select>
      </div>
    </header>

    <div class="cards-grid">
      <!-- 指令表单 -->
      <el-card>
        <template #header>
          <div class="card-header">
            <span>发送指令</span>
          </div>
        </template>
        <el-form label-position="top">
          <el-form-item label="指令类型">
            <el-autocomplete
              v-model="commandType"
              :fetch-suggestions="querySearch"
              placeholder="例如: setEmotion"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="参数负载 (JSON)">
            <el-input
              v-model="commandPayload"
              type="textarea"
              :rows="6"
              placeholder='{ "emotion": "happy" }'
            />
          </el-form-item>
          <el-button type="primary" @click="send" :loading="sending" :disabled="!selectedDeviceId">
            发送指令
          </el-button>
        </el-form>
      </el-card>

      <!-- 最近指令 -->
      <el-card>
        <template #header>
          <div class="card-header">
            <span>最近发送</span>
            <el-button link size="small" @click="clearLog">清空</el-button>
          </div>
        </template>
        <div v-if="commandLog.length === 0" class="empty-text">暂无发送记录</div>
        <el-timeline v-else>
          <el-timeline-item
            v-for="cmd in commandLog"
            :key="cmd.reqId"
            :timestamp="cmd.time"
            placement="top"
            :type="getStatusType(cmd.status)"
          >
            <el-card class="log-card">
              <div class="log-header">
                <span class="log-type">{{ cmd.type }}</span>
                <el-tag size="small" :type="getStatusType(cmd.status)">{{ cmd.status }}</el-tag>
              </div>
              <div class="log-id">ReqID: {{ cmd.reqId }}</div>
              <div v-if="cmd.result" class="log-result">
                结果: {{ JSON.stringify(cmd.result) }}
              </div>
              <el-button
                v-if="cmd.status === 'PENDING' || cmd.status === 'SENT'"
                size="small"
                link
                @click="checkStatus(cmd)"
              >
                刷新状态
              </el-button>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDevices, sendCommand, getCommand } from '@/api/device'
import type { DeviceResponse } from '@/types/device'

interface CommandLogItem {
  reqId: string
  type: string
  time: string
  status: string
  result?: any
}

const devices = ref<DeviceResponse[]>([])
const selectedDeviceId = ref('')
const commandType = ref('setEmotion')
const commandPayload = ref('{\n  "emotion": "happy"\n}')
const sending = ref(false)
const commandLog = ref<CommandLogItem[]>([])

const suggestedTypes = [
  { value: 'setEmotion' },
  { value: 'move' },
  { value: 'speak' },
  { value: 'ping' },
  { value: 'ota' }
]

function querySearch(queryString: string, cb: any) {
  const results = queryString
    ? suggestedTypes.filter(createFilter(queryString))
    : suggestedTypes
  cb(results)
}

function createFilter(queryString: string) {
  return (item: { value: string }) => {
    return item.value.toLowerCase().indexOf(queryString.toLowerCase()) === 0
  }
}

async function loadDevices() {
  try {
    devices.value = await getDevices()
    if (devices.value.length > 0) {
      selectedDeviceId.value = devices.value[0]?.device.deviceId ?? ''
    }
  } catch (e) {
    console.error(e)
  }
}

async function send() {
  if (!selectedDeviceId.value) return
  
  let payloadObj = {}
  try {
    payloadObj = JSON.parse(commandPayload.value)
  } catch (e) {
    // Show error message (using alert for simplicity or Element message if available)
    console.error('Invalid JSON', e)
    alert('Payload 必须是有效的 JSON 格式')
    return
  }

  sending.value = true
  try {
    const res = await sendCommand(selectedDeviceId.value, commandType.value, payloadObj)
    commandLog.value.unshift({
      reqId: res.reqId,
      type: res.type,
      time: new Date().toLocaleTimeString(),
      status: res.status
    })
  } catch (e) {
    console.error(e)
    alert('发送失败')
  } finally {
    sending.value = false
  }
}

async function checkStatus(cmd: CommandLogItem) {
  try {
    const res = await getCommand(selectedDeviceId.value, cmd.reqId)
    cmd.status = res.status
    if (res.ackPayload) {
      cmd.result = res.ackPayload
    }
    if (res.error) {
       cmd.result = { error: res.error }
    }
  } catch (e) {
    console.error(e)
  }
}

function getStatusType(status: string) {
  switch (status) {
    case 'SUCCESS': return 'success'
    case 'FAILED': return 'danger'
    case 'TIMEOUT': return 'warning'
    default: return 'primary'
  }
}

function clearLog() {
  commandLog.value = []
}

onMounted(() => {
  loadDevices()
})
</script>

<style scoped>
.cards-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  align-items: start;
}
.empty-text {
  color: #909399;
  text-align: center;
  padding: 20px;
}
.log-card {
  margin-bottom: 5px;
}
.log-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
}
.log-type {
  font-weight: bold;
}
.log-id {
  font-size: 12px;
  color: #909399;
}
.log-result {
  margin-top: 5px;
  font-size: 12px;
  background: #f4f4f5;
  padding: 4px;
}
</style>
