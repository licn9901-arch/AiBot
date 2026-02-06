<template>
  <div class="page">
    <header class="page-header">
      <div>
        <div class="eyebrow">授权码管理</div>
        <h1 class="page-title">授权码清单</h1>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="showGenerateDialog = true">批量生成</el-button>
        <el-button @click="handleExport">导出CSV</el-button>
      </div>
    </header>

    <div class="card">
      <div class="toolbar">
        <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width: 150px">
          <el-option label="未使用" value="UNUSED" />
          <el-option label="已激活" value="ACTIVATED" />
          <el-option label="已撤销" value="REVOKED" />
        </el-select>
        <el-input
          v-model="filterBatchNo"
          placeholder="搜索批次号"
          clearable
          style="width: 200px"
          @keyup.enter="loadData"
        />
        <el-button @click="loadData">刷新</el-button>
      </div>

      <el-table :data="licenses" style="width: 100%" v-loading="loading">
        <el-table-column prop="code" label="授权码" width="280" show-overflow-tooltip />
        <el-table-column prop="batchNo" label="批次号" width="150" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.batchNo || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="productKey" label="产品" width="140" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.productKey || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deviceId" label="绑定设备" width="200">
          <template #default="{ row }">
            {{ row.deviceId || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="激活时间" width="170">
          <template #default="{ row }">
            {{ row.activatedAt ? formatTime(row.activatedAt) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="过期时间" width="170">
          <template #default="{ row }">
            {{ row.expiresAt ? formatTime(row.expiresAt) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.remark || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status !== 'REVOKED'"
              link
              type="danger"
              size="small"
              @click="handleRevoke(row)"
            >
              撤销
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="totalElements"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </div>

    <!-- 批量生成弹窗 -->
    <el-dialog v-model="showGenerateDialog" title="批量生成授权码" width="480px">
      <el-form :model="generateForm" label-width="80px">
        <el-form-item label="产品" required>
          <el-select v-model="generateForm.productKey" placeholder="请选择产品" style="width: 100%">
            <el-option
              v-for="p in products"
              :key="p.productKey"
              :label="`${p.name} (${p.productKey})`"
              :value="p.productKey"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数量" required>
          <el-input-number v-model="generateForm.count" :min="1" :max="1000" />
        </el-form-item>
        <el-form-item label="批次号">
          <el-input v-model="generateForm.batchNo" placeholder="可选，用于分组管理" />
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="generateForm.expiresAt"
            type="datetime"
            placeholder="可选，不填则永不过期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="generateForm.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showGenerateDialog = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="handleGenerate">生成</el-button>
      </template>
    </el-dialog>

    <!-- 生成结果弹窗 -->
    <el-dialog
      v-model="showResultDialog"
      title="生成结果"
      width="750px"
      :close-on-click-modal="false"
      :before-close="handleResultDialogClose"
    >
      <el-alert type="warning" :closable="false" style="margin-bottom: 16px">
        设备密钥已暂存，请尽快下载Excel文件保存。未下载的密钥将在24小时后自动清除。
      </el-alert>
      <el-table :data="generateResults" style="width: 100%" max-height="400">
        <el-table-column prop="code" label="授权码" width="220" show-overflow-tooltip />
        <el-table-column prop="deviceId" label="设备SN" width="220" show-overflow-tooltip />
        <el-table-column prop="deviceSecret" label="设备密钥" show-overflow-tooltip />
      </el-table>
      <template #footer>
        <el-button @click="handleCopyAll">复制全部</el-button>
        <el-button type="primary" :loading="downloading" @click="handleDownloadExcel">下载Excel</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getLicenses, generateLicenses, revokeLicense, exportLicenses, downloadLicenseBatch, confirmLicenseBatch } from '@/api/license'
import { getProducts } from '@/api/product'
import type { LicenseCodeResponse, LicenseStatus, GenerateLicenseResponse } from '@/types/license'
import type { ProductResponse } from '@/types/product'

const licenses = ref<LicenseCodeResponse[]>([])
const loading = ref(false)
const totalElements = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const filterStatus = ref<LicenseStatus | ''>('')
const filterBatchNo = ref('')

const products = ref<ProductResponse[]>([])

const showGenerateDialog = ref(false)
const generating = ref(false)
const generateForm = reactive({
  count: 10,
  productKey: '',
  batchNo: '',
  expiresAt: null as Date | null,
  remark: ''
})

const showResultDialog = ref(false)
const generateResults = ref<GenerateLicenseResponse[]>([])
const currentBatchNo = ref('')
const batchConfirmed = ref(false)
const downloading = ref(false)

onMounted(async () => {
  try {
    products.value = await getProducts()
  } catch (e) {
    console.error('Failed to load products', e)
  }
})

function statusTagType(status: LicenseStatus) {
  switch (status) {
    case 'UNUSED': return 'info'
    case 'ACTIVATED': return 'success'
    case 'REVOKED': return 'danger'
    default: return 'info'
  }
}

function statusLabel(status: LicenseStatus) {
  switch (status) {
    case 'UNUSED': return '未使用'
    case 'ACTIVATED': return '已激活'
    case 'REVOKED': return '已撤销'
    default: return status
  }
}

function formatTime(ts: string) {
  return new Date(ts).toLocaleString()
}

async function loadData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: currentPage.value - 1,
      size: pageSize.value
    }
    if (filterStatus.value) params.status = filterStatus.value
    if (filterBatchNo.value) params.batchNo = filterBatchNo.value

    const res = await getLicenses(params)
    licenses.value = res.content
    totalElements.value = res.totalElements
  } catch (e) {
    console.error('Failed to load licenses', e)
  } finally {
    loading.value = false
  }
}

async function handleGenerate() {
  if (!generateForm.productKey) {
    ElMessage.warning('请选择产品')
    return
  }
  generating.value = true
  try {
    const data: Record<string, any> = {
      count: generateForm.count,
      productKey: generateForm.productKey
    }
    if (generateForm.batchNo) data.batchNo = generateForm.batchNo
    if (generateForm.expiresAt) data.expiresAt = generateForm.expiresAt.toISOString()
    if (generateForm.remark) data.remark = generateForm.remark

    const result = await generateLicenses(data as any)
    ElMessage.success(`成功生成 ${result.items.length} 个授权码`)
    showGenerateDialog.value = false

    // 保存批次号和结果
    currentBatchNo.value = result.batchNo
    generateResults.value = result.items
    batchConfirmed.value = false
    showResultDialog.value = true

    // 重置表单
    generateForm.count = 10
    generateForm.productKey = ''
    generateForm.batchNo = ''
    generateForm.expiresAt = null
    generateForm.remark = ''
    loadData()
  } catch (e) {
    console.error('Failed to generate licenses', e)
  } finally {
    generating.value = false
  }
}

async function handleDownloadExcel() {
  if (!currentBatchNo.value) return
  downloading.value = true
  try {
    const blob = await downloadLicenseBatch(currentBatchNo.value)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `licenses-${currentBatchNo.value}.xlsx`
    a.click()
    URL.revokeObjectURL(url)

    // 下载成功后自动确认
    await confirmLicenseBatch(currentBatchNo.value)
    batchConfirmed.value = true
    ElMessage.success('下载成功，密钥已安全清除')
  } catch (e) {
    console.error('Failed to download batch', e)
    ElMessage.error('下载失败，请重试')
  } finally {
    downloading.value = false
  }
}

async function handleResultDialogClose(done: () => void) {
  if (!batchConfirmed.value) {
    try {
      await ElMessageBox.confirm(
        `密钥数据尚未下载，关闭后仍可通过批次号「${currentBatchNo.value}」重新下载（24小时内有效）。确定关闭？`,
        '提示',
        { type: 'warning', confirmButtonText: '确定关闭', cancelButtonText: '取消' }
      )
      done()
    } catch {
      // 用户取消关闭
    }
  } else {
    done()
  }
}

function handleCopyAll() {
  const text = generateResults.value
    .map(r => `授权码: ${r.code}\t设备SN: ${r.deviceId}\t设备密钥: ${r.deviceSecret}`)
    .join('\n')
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败，请手动复制')
  })
}

async function handleRevoke(row: LicenseCodeResponse) {
  try {
    await ElMessageBox.confirm(
      `确定要撤销授权码 "${row.code.substring(0, 12)}..." 吗？撤销后设备将解绑。`,
      '确认撤销',
      { type: 'warning' }
    )
    await revokeLicense(row.id)
    ElMessage.success('撤销成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') console.error('Failed to revoke', e)
  }
}

async function handleExport() {
  try {
    const params: Record<string, any> = {}
    if (filterStatus.value) params.status = filterStatus.value
    if (filterBatchNo.value) params.batchNo = filterBatchNo.value

    const blob = await exportLicenses(params)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'licenses.csv'
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    console.error('Failed to export', e)
  }
}

watch([filterStatus], () => {
  currentPage.value = 1
  loadData()
})

loadData()
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
