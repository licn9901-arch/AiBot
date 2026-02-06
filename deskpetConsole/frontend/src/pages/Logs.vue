<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import type { LogEntry, AlertEntry } from '../types/logs'
import { fetchLogs, fetchAlerts } from '../api/logs'

const activeTab = ref('logs')
const loading = ref(false)
const logs = ref<LogEntry[]>([])
const alerts = ref<AlertEntry[]>([])

// Filter state
const search = ref('')
const logLevel = ref('')

const loadData = async () => {
  loading.value = true
  try {
    if (activeTab.value === 'logs') {
      logs.value = await fetchLogs({ 
        search: search.value, 
        level: logLevel.value || undefined 
      })
    } else {
      alerts.value = await fetchAlerts()
    }
  } catch (error) {
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const getLevelTag = (level: string) => {
  switch (level) {
    case 'error':
    case 'critical':
      return 'danger'
    case 'warning':
      return 'warning'
    default:
      return 'info'
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="logs-page">
    <div class="page-header">
      <h1 class="page-title">日志与告警</h1>
      <el-button :icon="Refresh" circle @click="loadData" :loading="loading" />
    </div>

    <el-tabs v-model="activeTab" class="log-tabs" @tab-change="loadData">
      <el-tab-pane label="系统日志" name="logs">
        <div class="toolbar">
          <el-input
            v-model="search"
            placeholder="搜索日志..."
            :prefix-icon="Search"
            clearable
            @input="loadData"
            style="width: 300px"
          />
          <el-select 
            v-model="logLevel" 
            placeholder="日志级别" 
            clearable 
            @change="loadData"
            style="width: 120px"
          >
            <el-option label="Info" value="info" />
            <el-option label="Warning" value="warning" />
            <el-option label="Error" value="error" />
          </el-select>
        </div>

        <el-table :data="logs" v-loading="loading" style="width: 100%" height="calc(100vh - 240px)">
          <el-table-column prop="timestamp" label="时间" width="180">
            <template #default="{ row }">
              {{ new Date(row.timestamp).toLocaleString() }}
            </template>
          </el-table-column>
          <el-table-column prop="level" label="级别" width="100">
            <template #default="{ row }">
              <el-tag :type="getLevelTag(row.level)" size="small" effect="dark">
                {{ row.level.toUpperCase() }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="module" label="模块" width="120" />
          <el-table-column prop="message" label="内容" min-width="300" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="告警信息" name="alerts">
        <el-table :data="alerts" v-loading="loading" style="width: 100%">
          <el-table-column prop="timestamp" label="告警时间" width="180">
            <template #default="{ row }">
              {{ new Date(row.timestamp).toLocaleString() }}
            </template>
          </el-table-column>
          <el-table-column prop="level" label="严重程度" width="100">
            <template #default="{ row }">
              <el-tag :type="getLevelTag(row.level)" size="small">
                {{ row.level === 'critical' ? '严重' : '警告' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="source" label="来源" width="120" />
          <el-table-column prop="message" label="告警内容" min-width="300" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'active' ? 'danger' : 'success'" size="small" effect="plain">
                {{ row.status === 'active' ? '未解决' : '已解决' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.logs-page {
  padding: 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.log-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

:deep(.el-tabs__content) {
  flex: 1;
  overflow: hidden;
  height: 100%;
}

:deep(.el-tab-pane) {
  height: 100%;
  display: flex;
  flex-direction: column;
}
</style>
