<template>
  <div class="page">
    <header class="page-header">
      <div>
        <div class="eyebrow">可观测性</div>
        <h1 class="page-title">日志管理</h1>
      </div>
      <el-button @click="loadData">刷新</el-button>
    </header>

    <div class="card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 操作日志 Tab -->
        <el-tab-pane label="操作日志" name="operation">
          <div class="toolbar">
            <el-input
              v-model="opFilter.userId"
              placeholder="用户ID"
              clearable
              style="width: 120px"
              @keyup.enter="loadData"
            />
            <el-input
              v-model="opFilter.deviceId"
              placeholder="设备ID"
              clearable
              style="width: 180px"
              @keyup.enter="loadData"
            />
            <el-input
              v-model="opFilter.action"
              placeholder="操作类型"
              clearable
              style="width: 150px"
              @keyup.enter="loadData"
            />
            <el-button @click="loadData">查询</el-button>
          </div>

          <el-table :data="opLogs" v-loading="loading" style="width: 100%">
            <el-table-column label="用户ID" width="100">
              <template #default="{ row }">{{ row.user_id ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="设备ID" width="180" show-overflow-tooltip>
              <template #default="{ row }">{{ row.device_id || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-tag size="small">{{ row.action }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="详情" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.payload ? (typeof row.payload === 'string' ? row.payload : JSON.stringify(row.payload)) : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="IP" width="140">
              <template #default="{ row }">{{ row.ip || '-' }}</template>
            </el-table-column>
            <el-table-column label="时间" width="170">
              <template #default="{ row }">{{ formatTime(row.created_at) }}</template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="opPage"
              v-model:page-size="opSize"
              :total="opTotal"
              :page-sizes="[20, 50, 100]"
              layout="total, sizes, prev, pager, next"
              @size-change="loadData"
              @current-change="loadData"
            />
          </div>
        </el-tab-pane>

        <!-- 应用日志 Tab -->
        <el-tab-pane label="应用日志" name="app">
          <div class="toolbar">
            <el-select v-model="appFilter.level" placeholder="日志级别" clearable style="width: 120px">
              <el-option label="DEBUG" value="DEBUG" />
              <el-option label="INFO" value="INFO" />
              <el-option label="WARN" value="WARN" />
              <el-option label="ERROR" value="ERROR" />
            </el-select>
            <el-input
              v-model="appFilter.logger"
              placeholder="Logger 名称"
              clearable
              style="width: 200px"
              @keyup.enter="loadData"
            />
            <el-input
              v-model="appFilter.search"
              placeholder="关键词搜索"
              clearable
              style="width: 200px"
              @keyup.enter="loadData"
            />
            <el-select v-model="appFilter.hours" style="width: 130px">
              <el-option label="最近1小时" :value="1" />
              <el-option label="最近6小时" :value="6" />
              <el-option label="最近24小时" :value="24" />
              <el-option label="最近7天" :value="168" />
            </el-select>
            <el-button @click="loadData">查询</el-button>
          </div>

          <el-alert
            v-if="appMessage"
            :title="appMessage"
            type="warning"
            :closable="false"
            style="margin-bottom: 12px"
          />

          <el-table :data="appLogs" v-loading="loading" style="width: 100%">
            <el-table-column label="时间" width="170">
              <template #default="{ row }">{{ formatTime(row.logTime || row.log_time) }}</template>
            </el-table-column>
            <el-table-column label="级别" width="90">
              <template #default="{ row }">
                <el-tag :type="levelTagType(row.level)" size="small" effect="dark">
                  {{ row.level }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Logger" width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ row.logger || '-' }}</template>
            </el-table-column>
            <el-table-column label="线程" width="140" show-overflow-tooltip>
              <template #default="{ row }">{{ row.thread || '-' }}</template>
            </el-table-column>
            <el-table-column label="消息" min-width="300" show-overflow-tooltip>
              <template #default="{ row }">{{ row.message || '-' }}</template>
            </el-table-column>
            <el-table-column label="异常" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.exception" type="danger" size="small" @click="showException(row.exception)" style="cursor: pointer">
                  查看
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="appPage"
              v-model:page-size="appSize"
              :total="appTotal"
              :page-sizes="[20, 50, 100]"
              layout="total, sizes, prev, pager, next"
              @size-change="loadData"
              @current-change="loadData"
            />
          </div>
        </el-tab-pane>

        <!-- 设备事件 Tab -->
        <el-tab-pane label="设备事件" name="event">
          <div class="toolbar">
            <el-input
              v-model="eventFilter.deviceId"
              placeholder="设备ID"
              clearable
              style="width: 180px"
              @keyup.enter="loadData"
            />
            <el-select v-model="eventFilter.eventType" placeholder="事件类型" clearable style="width: 120px">
              <el-option label="info" value="info" />
              <el-option label="alert" value="alert" />
              <el-option label="error" value="error" />
            </el-select>
            <el-select v-model="eventFilter.hours" style="width: 130px">
              <el-option label="最近1小时" :value="1" />
              <el-option label="最近6小时" :value="6" />
              <el-option label="最近24小时" :value="24" />
              <el-option label="最近7天" :value="168" />
            </el-select>
            <el-button @click="loadData">查询</el-button>
          </div>

          <el-table :data="eventLogs" v-loading="loading" style="width: 100%">
            <el-table-column label="设备ID" width="180" show-overflow-tooltip>
              <template #default="{ row }">{{ row.device_id }}</template>
            </el-table-column>
            <el-table-column label="事件标识" width="140">
              <template #default="{ row }">{{ row.event_id }}</template>
            </el-table-column>
            <el-table-column label="事件类型" width="100">
              <template #default="{ row }">
                <el-tag :type="eventTypeTag(row.event_type)" size="small">
                  {{ row.event_type }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="参数" min-width="250" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.params ? (typeof row.params === 'string' ? row.params : JSON.stringify(row.params)) : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="时间" width="170">
              <template #default="{ row }">{{ formatTime(row.created_at) }}</template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="eventPage"
              v-model:page-size="eventSize"
              :total="eventTotal"
              :page-sizes="[20, 50, 100]"
              layout="total, sizes, prev, pager, next"
              @size-change="loadData"
              @current-change="loadData"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 异常详情弹窗 -->
    <el-dialog v-model="exceptionDialogVisible" title="异常详情" width="700px">
      <pre class="exception-content">{{ exceptionContent }}</pre>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getOperationLogs, getAppLogs, getDeviceEventLogs } from '@/api/logs'

const activeTab = ref('operation')
const loading = ref(false)

// 操作日志
const opLogs = ref<Record<string, any>[]>([])
const opTotal = ref(0)
const opPage = ref(1)
const opSize = ref(20)
const opFilter = reactive({ userId: '', deviceId: '', action: '' })

// 应用日志
const appLogs = ref<Record<string, any>[]>([])
const appTotal = ref(0)
const appPage = ref(1)
const appSize = ref(20)
const appMessage = ref('')
const appFilter = reactive({ level: '', logger: '', search: '', hours: 24 })

// 设备事件
const eventLogs = ref<Record<string, any>[]>([])
const eventTotal = ref(0)
const eventPage = ref(1)
const eventSize = ref(20)
const eventFilter = reactive({ deviceId: '', eventType: '', hours: 24 })

// 异常弹窗
const exceptionDialogVisible = ref(false)
const exceptionContent = ref('')

function formatTime(ts: string) {
  if (!ts) return '-'
  return new Date(ts).toLocaleString()
}

function levelTagType(level: string) {
  switch (level?.toUpperCase()) {
    case 'ERROR': return 'danger'
    case 'WARN': return 'warning'
    case 'DEBUG': return 'info'
    default: return 'success'
  }
}

function eventTypeTag(type: string) {
  switch (type) {
    case 'error': return 'danger'
    case 'alert': return 'warning'
    default: return 'info'
  }
}

function showException(exception: string) {
  exceptionContent.value = exception
  exceptionDialogVisible.value = true
}

function handleTabChange() {
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    switch (activeTab.value) {
      case 'operation': {
        const params: Record<string, any> = {
          page: opPage.value - 1,
          size: opSize.value
        }
        if (opFilter.userId) params.userId = Number(opFilter.userId)
        if (opFilter.deviceId) params.deviceId = opFilter.deviceId
        if (opFilter.action) params.action = opFilter.action
        const res = await getOperationLogs(params)
        opLogs.value = res.content
        opTotal.value = res.totalElements
        break
      }
      case 'app': {
        const params: Record<string, any> = {
          page: appPage.value - 1,
          size: appSize.value,
          hours: appFilter.hours
        }
        if (appFilter.level) params.level = appFilter.level
        if (appFilter.logger) params.logger = appFilter.logger
        if (appFilter.search) params.search = appFilter.search
        const res = await getAppLogs(params)
        appLogs.value = res.content
        appTotal.value = res.totalElements
        appMessage.value = res.message || ''
        break
      }
      case 'event': {
        const params: Record<string, any> = {
          page: eventPage.value - 1,
          size: eventSize.value,
          hours: eventFilter.hours
        }
        if (eventFilter.deviceId) params.deviceId = eventFilter.deviceId
        if (eventFilter.eventType) params.eventType = eventFilter.eventType
        const res = await getDeviceEventLogs(params)
        eventLogs.value = res.content
        eventTotal.value = res.totalElements
        break
      }
    }
  } catch (error) {
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.exception-content {
  background: #f5f5f5;
  padding: 16px;
  border-radius: 4px;
  font-size: 13px;
  line-height: 1.6;
  max-height: 400px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
