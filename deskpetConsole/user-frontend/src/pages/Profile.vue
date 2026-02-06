<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Cell as VanCell, CellGroup as VanCellGroup, Button as VanButton,
  Field as VanField, Form as VanForm, showToast, Tag as VanTag
} from 'vant'
import { useResponsive } from '@/composables/useResponsive'
import { useUserStore } from '@/stores/user'
import { updateCurrentUser } from '@/api/user'
import { getMyLicenses } from '@/api/license'
import type { LicenseCodeResponse } from '@/types/license'
import { formatTime } from '@/utils/format'

const { isMobile } = useResponsive()
const userStore = useUserStore()

const editMode = ref(false)
const editForm = ref({ email: '', phone: '' })
const saving = ref(false)
const licenses = ref<LicenseCodeResponse[]>([])

function startEdit() {
  editForm.value.email = userStore.userInfo?.email || ''
  editForm.value.phone = userStore.userInfo?.phone || ''
  editMode.value = true
}

async function saveProfile() {
  saving.value = true
  try {
    await updateCurrentUser({
      email: editForm.value.email || undefined,
      phone: editForm.value.phone || undefined
    })
    await userStore.fetchUser()
    editMode.value = false
    const msg = '保存成功'
    isMobile.value ? showToast(msg) : ElMessage.success(msg)
  } catch {
    // 错误已处理
  } finally {
    saving.value = false
  }
}

async function fetchLicenses() {
  try {
    licenses.value = await getMyLicenses()
  } catch {
    // 错误已处理
  }
}

function statusLabel(status: string) {
  switch (status) {
    case 'ACTIVATED': return '已激活'
    case 'UNUSED': return '未使用'
    case 'REVOKED': return '已撤销'
    default: return status
  }
}

function statusClass(status: string) {
  switch (status) {
    case 'ACTIVATED': return 'success'
    case 'UNUSED': return 'info'
    case 'REVOKED': return 'danger'
    default: return ''
  }
}

onMounted(async () => {
  await userStore.fetchUser()
  await fetchLicenses()
})
</script>

<template>
  <div>
    <h1 class="page-title">个人中心</h1>

    <!-- 移动端 -->
    <template v-if="isMobile">
      <!-- 用户信息 -->
      <VanCellGroup inset title="个人信息" style="margin-bottom: 16px;">
        <VanCell title="用户名" :value="userStore.userInfo?.username || '-'" />
        <template v-if="!editMode">
          <VanCell title="邮箱" :value="userStore.userInfo?.email || '-'" />
          <VanCell title="手机" :value="userStore.userInfo?.phone || '-'" />
          <VanCell title="注册时间" :value="formatTime(userStore.userInfo?.createdAt)" />
        </template>
      </VanCellGroup>

      <template v-if="editMode">
        <VanForm @submit="saveProfile">
          <VanCellGroup inset>
            <VanField v-model="editForm.email" label="邮箱" placeholder="请输入邮箱" />
            <VanField v-model="editForm.phone" label="手机" placeholder="请输入手机号" />
          </VanCellGroup>
          <div style="margin: 16px; display: flex; gap: 12px;">
            <VanButton round block type="primary" native-type="submit" :loading="saving" color="var(--primary)">保存</VanButton>
            <VanButton round block @click="editMode = false">取消</VanButton>
          </div>
        </VanForm>
      </template>
      <div v-else style="margin: 16px;">
        <VanButton round block plain type="primary" @click="startEdit">编辑信息</VanButton>
      </div>

      <!-- 授权码列表 -->
      <VanCellGroup inset title="我的授权码" style="margin-top: 24px;">
        <VanCell v-if="licenses.length === 0" title="暂无授权码" />
        <VanCell
          v-for="lic in licenses"
          :key="lic.id"
          :title="lic.code"
          :label="`${lic.deviceId || '未绑定'} · ${formatTime(lic.activatedAt)}`"
        >
          <template #right-icon>
            <VanTag :type="lic.status === 'ACTIVATED' ? 'success' : lic.status === 'REVOKED' ? 'danger' : 'primary'">
              {{ statusLabel(lic.status) }}
            </VanTag>
          </template>
        </VanCell>
      </VanCellGroup>

      <!-- 退出登录 -->
      <div style="margin: 24px 16px;">
        <VanButton round block type="danger" plain @click="userStore.logout()">退出登录</VanButton>
      </div>
    </template>

    <!-- PC 端 -->
    <template v-else>
      <div class="grid-2">
        <!-- 用户信息卡片 -->
        <div class="card">
          <h3 style="font-size: 15px; font-weight: 600; margin-bottom: 16px;">个人信息</h3>

          <template v-if="!editMode">
            <div style="display: grid; gap: 12px;">
              <div>
                <span style="color: var(--muted); font-size: 13px;">用户名</span>
                <div style="font-weight: 500;">{{ userStore.userInfo?.username || '-' }}</div>
              </div>
              <div>
                <span style="color: var(--muted); font-size: 13px;">邮箱</span>
                <div style="font-weight: 500;">{{ userStore.userInfo?.email || '-' }}</div>
              </div>
              <div>
                <span style="color: var(--muted); font-size: 13px;">手机</span>
                <div style="font-weight: 500;">{{ userStore.userInfo?.phone || '-' }}</div>
              </div>
              <div>
                <span style="color: var(--muted); font-size: 13px;">注册时间</span>
                <div style="font-weight: 500;">{{ formatTime(userStore.userInfo?.createdAt) }}</div>
              </div>
            </div>
            <div style="margin-top: 16px; display: flex; gap: 12px;">
              <el-button round @click="startEdit">编辑信息</el-button>
              <el-button type="danger" round plain @click="userStore.logout()">退出登录</el-button>
            </div>
          </template>

          <template v-else>
            <el-form label-position="top" @submit.prevent="saveProfile">
              <el-form-item label="用户名">
                <el-input :model-value="userStore.userInfo?.username" disabled />
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input v-model="editForm.email" placeholder="请输入邮箱" />
              </el-form-item>
              <el-form-item label="手机">
                <el-input v-model="editForm.phone" placeholder="请输入手机号" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" round native-type="submit" :loading="saving" style="background: var(--primary); border-color: var(--primary);">保存</el-button>
                <el-button round @click="editMode = false">取消</el-button>
              </el-form-item>
            </el-form>
          </template>
        </div>

        <!-- 授权码列表卡片 -->
        <div class="card">
          <h3 style="font-size: 15px; font-weight: 600; margin-bottom: 16px;">我的授权码</h3>
          <div v-if="licenses.length === 0" class="empty-state" style="padding: 30px 0;">
            <div class="empty-text">暂无授权码</div>
          </div>
          <el-table v-else :data="licenses" stripe size="small">
            <el-table-column prop="code" label="授权码" min-width="160" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <span class="tag" :class="statusClass(row.status)">{{ statusLabel(row.status) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="deviceId" label="绑定设备" min-width="120">
              <template #default="{ row }">{{ row.deviceId || '-' }}</template>
            </el-table-column>
            <el-table-column label="激活时间" width="170">
              <template #default="{ row }">{{ formatTime(row.activatedAt) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </template>
  </div>
</template>
