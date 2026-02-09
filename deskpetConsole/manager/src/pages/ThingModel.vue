<template>
  <div class="page">
    <header class="page-header">
      <div>
        <div class="eyebrow">物模型编辑</div>
        <h1 class="page-title">{{ model?.productName || productKey }} 的物模型</h1>
      </div>
      <div class="header-actions">
        <el-button @click="handleImport">导入JSON</el-button>
        <el-button @click="handleExport">导出JSON</el-button>
        <el-button link @click="$router.push('/products')">返回产品列表</el-button>
      </div>
    </header>

    <div class="card" v-loading="loading">
      <el-tabs v-model="activeTab">
        <!-- 属性 Tab -->
        <el-tab-pane label="属性" name="properties">
          <div class="tab-toolbar">
            <el-button type="primary" size="small" @click="openPropertyDialog()">添加属性</el-button>
          </div>
          <el-table :data="model?.properties || []" style="width: 100%">
            <el-table-column prop="identifier" label="标识" width="140" />
            <el-table-column prop="name" label="名称" width="120" />
            <el-table-column prop="dataType" label="数据类型" width="90" />
            <el-table-column label="规格" min-width="180">
              <template #default="{ row }">
                <SpecsDisplay :specs="row.specs" :data-type="row.dataType" />
              </template>
            </el-table-column>
            <el-table-column label="访问模式" width="90">
              <template #default="{ row }">
                <el-tag :type="(row.accessMode || 'rw') === 'rw' ? '' : 'info'" size="small">
                  {{ (row.accessMode || 'rw') === 'rw' ? '读写' : '只读' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="必需" width="70">
              <template #default="{ row }">
                <el-tag :type="row.required ? 'danger' : 'info'" size="small">
                  {{ row.required ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.description || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="sortOrder" label="排序" width="70" />
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openPropertyDialog(row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="handleDeleteProperty(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 服务 Tab -->
        <el-tab-pane label="服务" name="services">
          <div class="tab-toolbar">
            <el-button type="primary" size="small" @click="openServiceDialog()">添加服务</el-button>
          </div>
          <el-table :data="model?.services || []" style="width: 100%" row-key="id">
            <el-table-column type="expand">
              <template #default="{ row }">
                <div class="expand-content">
                  <div class="expand-section">
                    <div class="expand-label">输入参数</div>
                    <ParamTableReadonly :params="row.inputParams" />
                  </div>
                  <div class="expand-section">
                    <div class="expand-label">输出参数</div>
                    <ParamTableReadonly :params="row.outputParams" />
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="identifier" label="标识" width="140" />
            <el-table-column prop="name" label="名称" width="140" />
            <el-table-column prop="callType" label="调用类型" width="100">
              <template #default="{ row }">
                {{ row.callType || 'async' }}
              </template>
            </el-table-column>
            <el-table-column label="输入参数" min-width="160">
              <template #default="{ row }">
                {{ formatParamsSummary(row.inputParams) }}
              </template>
            </el-table-column>
            <el-table-column label="输出参数" min-width="160">
              <template #default="{ row }">
                {{ formatParamsSummary(row.outputParams) }}
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.description || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openServiceDialog(row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="handleDeleteService(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 事件 Tab -->
        <el-tab-pane label="事件" name="events">
          <div class="tab-toolbar">
            <el-button type="primary" size="small" @click="openEventDialog()">添加事件</el-button>
          </div>
          <el-table :data="model?.events || []" style="width: 100%" row-key="id">
            <el-table-column type="expand">
              <template #default="{ row }">
                <div class="expand-content">
                  <div class="expand-section">
                    <div class="expand-label">输出参数</div>
                    <ParamTableReadonly :params="row.outputParams" />
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="identifier" label="标识" width="140" />
            <el-table-column prop="name" label="名称" width="140" />
            <el-table-column prop="eventType" label="事件类型" width="100" />
            <el-table-column label="输出参数" min-width="180">
              <template #default="{ row }">
                {{ formatParamsSummary(row.outputParams) }}
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.description || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openEventDialog(row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="handleDeleteEvent(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 属性编辑弹窗 -->
    <el-dialog v-model="showPropertyDialog" :title="editingPropertyId ? '编辑属性' : '添加属性'" class="adaptive-dialog">
      <el-form :model="propertyForm" label-width="80px">
        <el-form-item label="标识" required>
          <el-input v-model="propertyForm.identifier" placeholder="如 temperature" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="propertyForm.name" placeholder="如 温度" />
        </el-form-item>
        <el-form-item label="数据类型" required>
          <el-select v-model="propertyForm.dataType" style="width: 100%">
            <el-option
              v-for="opt in DATA_TYPE_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="规格">
          <SpecsEditor v-model="propertyForm.specs" :data-type="propertyForm.dataType" />
        </el-form-item>
        <el-form-item label="访问模式">
          <el-select v-model="propertyForm.accessMode" style="width: 100%">
            <el-option label="读写 (rw)" value="rw" />
            <el-option label="只读 (r)" value="r" />
          </el-select>
        </el-form-item>
        <el-form-item label="必需">
          <el-switch v-model="propertyForm.required" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="propertyForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="propertyForm.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPropertyDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitProperty">确定</el-button>
      </template>
    </el-dialog>

    <!-- 服务编辑弹窗 -->
    <el-dialog v-model="showServiceDialog" :title="editingServiceId ? '编辑服务' : '添加服务'" class="adaptive-dialog">
      <el-form :model="serviceForm" label-width="80px">
        <el-form-item label="标识" required>
          <el-input v-model="serviceForm.identifier" placeholder="如 setTemperature" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="serviceForm.name" placeholder="如 设置温度" />
        </el-form-item>
        <el-form-item label="调用类型">
          <el-select v-model="serviceForm.callType" style="width: 100%">
            <el-option label="异步 (async)" value="async" />
            <el-option label="同步 (sync)" value="sync" />
          </el-select>
        </el-form-item>
        <el-form-item label="输入参数">
          <ParamTable v-model="serviceForm.inputParams" />
        </el-form-item>
        <el-form-item label="输出参数">
          <ParamTable v-model="serviceForm.outputParams" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="serviceForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="serviceForm.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showServiceDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitService">确定</el-button>
      </template>
    </el-dialog>

    <!-- 事件编辑弹窗 -->
    <el-dialog v-model="showEventDialog" :title="editingEventId ? '编辑事件' : '添加事件'" class="adaptive-dialog">
      <el-form :model="eventForm" label-width="80px">
        <el-form-item label="标识" required>
          <el-input v-model="eventForm.identifier" placeholder="如 temperatureAlarm" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="eventForm.name" placeholder="如 温度告警" />
        </el-form-item>
        <el-form-item label="事件类型" required>
          <el-select v-model="eventForm.eventType" style="width: 100%">
            <el-option label="信息 (info)" value="info" />
            <el-option label="告警 (alert)" value="alert" />
            <el-option label="故障 (error)" value="error" />
          </el-select>
        </el-form-item>
        <el-form-item label="输出参数">
          <ParamTable v-model="eventForm.outputParams" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="eventForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="eventForm.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEventDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitEvent">确定</el-button>
      </template>
    </el-dialog>

    <!-- 隐藏的文件输入 -->
    <input ref="fileInput" type="file" accept=".json" style="display: none" @change="onFileSelected" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getProductDetail,
  addProperty, updateProperty, deleteProperty,
  addService, updateService, deleteService,
  addEvent, updateEvent, deleteEvent,
  exportThingModel, importThingModel
} from '@/api/product'
import type { ThingModelDTO, PropertyDTO, ServiceDTO, EventDTO } from '@/types/product'
import { DATA_TYPE_OPTIONS } from '@/types/product'
import SpecsDisplay from '@/components/thing-model/SpecsDisplay.vue'
import SpecsEditor from '@/components/thing-model/SpecsEditor.vue'
import ParamTableReadonly from '@/components/thing-model/ParamTableReadonly.vue'
import ParamTable from '@/components/thing-model/ParamTable.vue'

const route = useRoute()
const productKey = route.params.productKey as string

const model = ref<ThingModelDTO | null>(null)
const loading = ref(false)
const submitting = ref(false)
const activeTab = ref('properties')
const fileInput = ref<HTMLInputElement>()

// ==================== 属性 ====================

const showPropertyDialog = ref(false)
const editingPropertyId = ref<number | null>(null)
const propertyForm = reactive({
  identifier: '',
  name: '',
  dataType: 'int',
  specs: null as Record<string, any> | null,
  accessMode: 'rw',
  required: false,
  description: '',
  sortOrder: 0
})

function openPropertyDialog(row?: PropertyDTO) {
  if (row) {
    editingPropertyId.value = row.id
    propertyForm.identifier = row.identifier
    propertyForm.name = row.name
    propertyForm.dataType = row.dataType
    propertyForm.specs = row.specs ? { ...row.specs } : null
    propertyForm.accessMode = row.accessMode || 'rw'
    propertyForm.required = row.required
    propertyForm.description = row.description || ''
    propertyForm.sortOrder = row.sortOrder
  } else {
    editingPropertyId.value = null
    propertyForm.identifier = ''
    propertyForm.name = ''
    propertyForm.dataType = 'int'
    propertyForm.specs = null
    propertyForm.accessMode = 'rw'
    propertyForm.required = false
    propertyForm.description = ''
    propertyForm.sortOrder = 0
  }
  showPropertyDialog.value = true
}

async function handleSubmitProperty() {
  if (!propertyForm.identifier.trim() || !propertyForm.name.trim()) {
    ElMessage.warning('请填写标识和名称')
    return
  }
  submitting.value = true
  try {
    const data = {
      identifier: propertyForm.identifier,
      name: propertyForm.name,
      dataType: propertyForm.dataType,
      specs: propertyForm.specs || undefined,
      accessMode: propertyForm.accessMode,
      required: propertyForm.required,
      description: propertyForm.description || undefined,
      sortOrder: propertyForm.sortOrder
    }
    if (editingPropertyId.value) {
      await updateProperty(productKey, editingPropertyId.value, data)
      ElMessage.success('更新成功')
    } else {
      await addProperty(productKey, data)
      ElMessage.success('添加成功')
    }
    showPropertyDialog.value = false
    loadData()
  } catch (e) {
    console.error('Failed to submit property', e)
  } finally {
    submitting.value = false
  }
}

async function handleDeleteProperty(row: PropertyDTO) {
  try {
    await ElMessageBox.confirm(`确定要删除属性「${row.name}」吗？`, '确认删除', { type: 'warning' })
    await deleteProperty(productKey, row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') console.error('Failed to delete property', e)
  }
}

// ==================== 服务 ====================

const showServiceDialog = ref(false)
const editingServiceId = ref<number | null>(null)
const serviceForm = reactive({
  identifier: '',
  name: '',
  callType: 'async',
  inputParams: null as Record<string, any>[] | null,
  outputParams: null as Record<string, any>[] | null,
  description: '',
  sortOrder: 0
})

function openServiceDialog(row?: ServiceDTO) {
  if (row) {
    editingServiceId.value = row.id
    serviceForm.identifier = row.identifier
    serviceForm.name = row.name
    serviceForm.callType = row.callType || 'async'
    serviceForm.inputParams = row.inputParams ? [...row.inputParams] : null
    serviceForm.outputParams = row.outputParams ? [...row.outputParams] : null
    serviceForm.description = row.description || ''
    serviceForm.sortOrder = row.sortOrder
  } else {
    editingServiceId.value = null
    serviceForm.identifier = ''
    serviceForm.name = ''
    serviceForm.callType = 'async'
    serviceForm.inputParams = null
    serviceForm.outputParams = null
    serviceForm.description = ''
    serviceForm.sortOrder = 0
  }
  showServiceDialog.value = true
}

async function handleSubmitService() {
  if (!serviceForm.identifier.trim() || !serviceForm.name.trim()) {
    ElMessage.warning('请填写标识和名称')
    return
  }
  submitting.value = true
  try {
    const data = {
      identifier: serviceForm.identifier,
      name: serviceForm.name,
      callType: serviceForm.callType,
      inputParams: serviceForm.inputParams || undefined,
      outputParams: serviceForm.outputParams || undefined,
      description: serviceForm.description || undefined,
      sortOrder: serviceForm.sortOrder
    }
    if (editingServiceId.value) {
      await updateService(productKey, editingServiceId.value, data)
      ElMessage.success('更新成功')
    } else {
      await addService(productKey, data)
      ElMessage.success('添加成功')
    }
    showServiceDialog.value = false
    loadData()
  } catch (e) {
    console.error('Failed to submit service', e)
  } finally {
    submitting.value = false
  }
}

async function handleDeleteService(row: ServiceDTO) {
  try {
    await ElMessageBox.confirm(`确定要删除服务「${row.name}」吗？`, '确认删除', { type: 'warning' })
    await deleteService(productKey, row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') console.error('Failed to delete service', e)
  }
}

// ==================== 事件 ====================

const showEventDialog = ref(false)
const editingEventId = ref<number | null>(null)
const eventForm = reactive({
  identifier: '',
  name: '',
  eventType: 'info',
  outputParams: null as Record<string, any>[] | null,
  description: '',
  sortOrder: 0
})

function openEventDialog(row?: EventDTO) {
  if (row) {
    editingEventId.value = row.id
    eventForm.identifier = row.identifier
    eventForm.name = row.name
    eventForm.eventType = row.eventType
    eventForm.outputParams = row.outputParams ? [...row.outputParams] : null
    eventForm.description = row.description || ''
    eventForm.sortOrder = row.sortOrder
  } else {
    editingEventId.value = null
    eventForm.identifier = ''
    eventForm.name = ''
    eventForm.eventType = 'info'
    eventForm.outputParams = null
    eventForm.description = ''
    eventForm.sortOrder = 0
  }
  showEventDialog.value = true
}

async function handleSubmitEvent() {
  if (!eventForm.identifier.trim() || !eventForm.name.trim()) {
    ElMessage.warning('请填写标识和名称')
    return
  }
  submitting.value = true
  try {
    const data = {
      identifier: eventForm.identifier,
      name: eventForm.name,
      eventType: eventForm.eventType,
      outputParams: eventForm.outputParams || undefined,
      description: eventForm.description || undefined,
      sortOrder: eventForm.sortOrder
    }
    if (editingEventId.value) {
      await updateEvent(productKey, editingEventId.value, data)
      ElMessage.success('更新成功')
    } else {
      await addEvent(productKey, data)
      ElMessage.success('添加成功')
    }
    showEventDialog.value = false
    loadData()
  } catch (e) {
    console.error('Failed to submit event', e)
  } finally {
    submitting.value = false
  }
}

async function handleDeleteEvent(row: EventDTO) {
  try {
    await ElMessageBox.confirm(`确定要删除事件「${row.name}」吗？`, '确认删除', { type: 'warning' })
    await deleteEvent(productKey, row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') console.error('Failed to delete event', e)
  }
}

// ==================== 导入导出 ====================

async function handleExport() {
  try {
    const blob = await exportThingModel(productKey)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${productKey}-thing-model.json`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    console.error('Failed to export', e)
  }
}

function handleImport() {
  fileInput.value?.click()
}

async function onFileSelected(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  try {
    const text = await file.text()
    // 验证 JSON 格式
    JSON.parse(text)
    await ElMessageBox.confirm(
      '导入将覆盖当前物模型定义，确定继续吗？',
      '确认导入',
      { type: 'warning' }
    )
    await importThingModel(productKey, text)
    ElMessage.success('导入成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('导入失败，请检查 JSON 格式')
      console.error('Failed to import', e)
    }
  } finally {
    // 重置 file input 以便再次选择同一文件
    input.value = ''
  }
}

// ==================== 辅助函数 ====================

function formatParamsSummary(params: Record<string, any>[] | null): string {
  if (!params || params.length === 0) return '-'
  const names = params.map(p => p.identifier || p.name).filter(Boolean)
  return `${params.length} 个参数 (${names.join(', ')})`
}

// ==================== 加载数据 ====================

async function loadData() {
  loading.value = true
  try {
    model.value = await getProductDetail(productKey)
  } catch (e) {
    console.error('Failed to load thing model', e)
  } finally {
    loading.value = false
  }
}

loadData()
</script>

<style scoped>
.tab-toolbar {
  margin-bottom: 16px;
}
.expand-content {
  padding: 12px 20px;
}
.expand-section {
  margin-bottom: 12px;
}
.expand-section:last-child {
  margin-bottom: 0;
}
.expand-label {
  font-weight: 500;
  font-size: 13px;
  color: var(--el-text-color-regular);
  margin-bottom: 6px;
}
</style>

<style>
/* 自适应弹窗：非 scoped，因为 el-dialog 挂载到 body */
.adaptive-dialog {
  width: auto !important;
  min-width: 560px;
  max-width: 90vw;
}
.adaptive-dialog .el-dialog__body {
  overflow-x: auto;
}
</style>
