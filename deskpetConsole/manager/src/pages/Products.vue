<template>
  <div class="page">
    <header class="page-header">
      <div>
        <div class="eyebrow">产品管理</div>
        <h1 class="page-title">产品列表</h1>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="openCreateDialog">创建产品</el-button>
      </div>
    </header>

    <div class="card">
      <el-table :data="products" style="width: 100%" v-loading="loading">
        <el-table-column prop="productKey" label="产品标识" width="180" />
        <el-table-column prop="name" label="产品名称" width="180" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.description || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="$router.push(`/products/${row.productKey}`)">
              物模型
            </el-button>
            <el-button link type="primary" size="small" @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 创建/编辑产品弹窗 -->
    <el-dialog v-model="showDialog" :title="isEdit ? '编辑产品' : '创建产品'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="产品标识" required>
          <el-input v-model="form.productKey" :disabled="isEdit" placeholder="唯一标识，如 pet-v1" />
        </el-form-item>
        <el-form-item label="产品名称" required>
          <el-input v-model="form.name" placeholder="产品名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
        <el-form-item v-if="isEdit" label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="ACTIVE" value="ACTIVE" />
            <el-option label="INACTIVE" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProducts, createProduct, updateProduct, deleteProduct } from '@/api/product'
import type { ProductResponse } from '@/types/product'

const products = ref<ProductResponse[]>([])
const loading = ref(false)
const showDialog = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const editingKey = ref('')

const form = reactive({
  productKey: '',
  name: '',
  description: '',
  status: 'ACTIVE'
})

function formatTime(ts: string) {
  return new Date(ts).toLocaleString()
}

function resetForm() {
  form.productKey = ''
  form.name = ''
  form.description = ''
  form.status = 'ACTIVE'
}

function openCreateDialog() {
  isEdit.value = false
  resetForm()
  showDialog.value = true
}

function openEditDialog(row: ProductResponse) {
  isEdit.value = true
  editingKey.value = row.productKey
  form.productKey = row.productKey
  form.name = row.name
  form.description = row.description || ''
  form.status = row.status
  showDialog.value = true
}

async function loadData() {
  loading.value = true
  try {
    products.value = await getProducts()
  } catch (e) {
    console.error('Failed to load products', e)
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入产品名称')
    return
  }
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateProduct(editingKey.value, {
        name: form.name,
        description: form.description || undefined,
        status: form.status
      })
      ElMessage.success('更新成功')
    } else {
      if (!form.productKey.trim()) {
        ElMessage.warning('请输入产品标识')
        submitting.value = false
        return
      }
      await createProduct({
        productKey: form.productKey,
        name: form.name,
        description: form.description || undefined
      })
      ElMessage.success('创建成功')
    }
    showDialog.value = false
    loadData()
  } catch (e) {
    console.error('Failed to submit product', e)
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: ProductResponse) {
  try {
    await ElMessageBox.confirm(
      `确定要删除产品「${row.name}」吗？该操作不可恢复。`,
      '确认删除',
      { type: 'warning' }
    )
    await deleteProduct(row.productKey)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') console.error('Failed to delete product', e)
  }
}

loadData()
</script>
