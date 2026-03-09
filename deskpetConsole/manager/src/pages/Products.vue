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
        <el-table-column label="图标" width="90">
          <template #default="{ row }">
            <img
              v-if="row.icon"
              :src="row.icon"
              alt="产品图标"
              style="width: 40px; height: 40px; border-radius: 12px; object-fit: cover; border: 1px solid rgba(148, 163, 184, 0.24);"
            />
            <div
              v-else
              style="width: 40px; height: 40px; border-radius: 12px; background: rgba(148, 163, 184, 0.12); display: flex; align-items: center; justify-content: center; color: #94a3b8; font-size: 12px;"
            >
              无
            </div>
          </template>
        </el-table-column>
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
    <el-dialog v-model="showDialog" :title="isEdit ? '编辑产品' : '创建产品'" width="560px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="产品标识" required>
          <el-input v-model="form.productKey" :disabled="isEdit" placeholder="唯一标识，如 pet-v1" />
        </el-form-item>
        <el-form-item label="产品名称" required>
          <el-input v-model="form.name" placeholder="产品名称" />
        </el-form-item>
        <el-form-item label="产品图标">
          <input
            ref="iconInput"
            type="file"
            accept="image/png,image/jpeg,image/webp,image/gif"
            style="display: none"
            @change="handleIconSelected"
          >
          <div style="display: flex; gap: 16px; align-items: center; flex-wrap: wrap;">
            <img
              v-if="form.icon"
              :src="form.icon"
              alt="产品图标预览"
              style="width: 72px; height: 72px; border-radius: 18px; object-fit: cover; border: 1px solid rgba(148, 163, 184, 0.24);"
            />
            <div
              v-else
              style="width: 72px; height: 72px; border-radius: 18px; display: flex; align-items: center; justify-content: center; background: rgba(148, 163, 184, 0.12); color: #94a3b8;"
            >
              无图标
            </div>
            <div style="display: flex; gap: 12px; flex-wrap: wrap;">
              <el-button :loading="iconUploading" @click="triggerIconUpload">
                {{ iconUploading ? '上传中...' : '上传图标' }}
              </el-button>
              <el-button v-if="form.icon" @click="clearIcon">清空</el-button>
            </div>
          </div>
          <div style="font-size: 12px; color: #64748b; margin-top: 8px;">支持 JPG、PNG、WEBP、GIF，大小不超过 5MB。</div>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
        <el-form-item v-if="isEdit" label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="ACTIVE" value="ACTIVE" />
            <el-option label="DEPRECATED" value="DEPRECATED" />
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
import { uploadImage } from '@/api/file'
import { getProducts, createProduct, updateProduct, deleteProduct } from '@/api/product'
import type { ProductResponse } from '@/types/product'

const products = ref<ProductResponse[]>([])
const loading = ref(false)
const showDialog = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const editingKey = ref('')
const iconUploading = ref(false)
const iconInput = ref<HTMLInputElement | null>(null)

const form = reactive({
  productKey: '',
  name: '',
  description: '',
  icon: '',
  iconKey: '',
  status: 'ACTIVE'
})

function formatTime(ts: string) {
  return new Date(ts).toLocaleString()
}

function resetForm() {
  form.productKey = ''
  form.name = ''
  form.description = ''
  form.icon = ''
  form.iconKey = ''
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
  form.icon = row.icon || ''
  form.iconKey = row.iconKey || ''
  form.status = row.status
  showDialog.value = true
}

function triggerIconUpload() {
  iconInput.value?.click()
}

async function handleIconSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  iconUploading.value = true
  try {
    const response = await uploadImage(file, 'product-icon')
    form.icon = response.url
    form.iconKey = response.objectKey
    if (isEdit.value) {
      await updateProduct(editingKey.value, {
        icon: response.objectKey
      })
      const index = products.value.findIndex(item => item.productKey === editingKey.value)
      if (index >= 0) {
        products.value[index] = {
          ...products.value[index],
          icon: response.url,
          iconKey: response.objectKey
        }
      }
    }
    ElMessage.success('图标上传成功')
  } catch (e) {
    console.error('Failed to upload icon', e)
  } finally {
    iconUploading.value = false
    input.value = ''
  }
}

function clearIcon() {
  form.icon = ''
  form.iconKey = ''
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
        icon: form.iconKey || undefined,
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
        description: form.description || undefined,
        icon: form.iconKey || undefined
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
