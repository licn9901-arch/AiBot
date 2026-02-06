<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { showToast, Form as VanForm, Field as VanField, Button as VanButton, CellGroup as VanCellGroup } from 'vant'
import { useResponsive } from '@/composables/useResponsive'
import { activateLicense } from '@/api/license'
import type { LicenseCodeResponse } from '@/types/license'

const { isMobile } = useResponsive()
const router = useRouter()

const code = ref('')
const loading = ref(false)
const result = ref<LicenseCodeResponse | null>(null)

async function handleActivate() {
  if (!code.value.trim()) {
    const msg = '请输入授权码'
    isMobile.value ? showToast(msg) : ElMessage.warning(msg)
    return
  }
  loading.value = true
  try {
    result.value = await activateLicense({ code: code.value.trim() })
    const msg = '激活成功'
    isMobile.value ? showToast(msg) : ElMessage.success(msg)
  } catch {
    // 错误已在 request.ts 中处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div>
    <h1 class="page-title">激活授权码</h1>

    <!-- 激活成功结果 -->
    <div v-if="result" class="card" style="text-align: center; margin-bottom: 24px;">
      <div style="font-size: 40px; margin-bottom: 12px;">&#10003;</div>
      <div style="font-weight: 600; font-size: 16px; margin-bottom: 8px;">激活成功</div>
      <div style="color: var(--muted); font-size: 14px; margin-bottom: 4px;">
        授权码：{{ result.code }}
      </div>
      <div v-if="result.deviceId" style="color: var(--muted); font-size: 14px; margin-bottom: 16px;">
        绑定设备：{{ result.deviceId }}
      </div>
      <template v-if="isMobile">
        <VanButton
          v-if="result.deviceId"
          type="primary"
          round
          size="small"
          color="var(--primary)"
          @click="router.push(`/devices/${result.deviceId}`)"
        >
          去查看设备
        </VanButton>
        <VanButton round size="small" style="margin-left: 8px;" @click="result = null; code = ''">
          继续激活
        </VanButton>
      </template>
      <template v-else>
        <el-button
          v-if="result.deviceId"
          type="primary"
          round
          @click="router.push(`/devices/${result.deviceId}`)"
          style="background: var(--primary); border-color: var(--primary);"
        >
          去查看设备
        </el-button>
        <el-button round @click="result = null; code = ''">继续激活</el-button>
      </template>
    </div>

    <!-- 激活表单 -->
    <div v-else>
      <!-- 移动端 -->
      <template v-if="isMobile">
        <VanForm @submit="handleActivate">
          <VanCellGroup inset>
            <VanField
              v-model="code"
              label="授权码"
              placeholder="请输入授权码"
              :rules="[{ required: true, message: '请输入授权码' }]"
            />
          </VanCellGroup>
          <div style="margin: 24px 16px;">
            <VanButton round block type="primary" native-type="submit" :loading="loading" color="var(--primary)">
              激活
            </VanButton>
          </div>
        </VanForm>
      </template>

      <!-- PC 端 -->
      <template v-else>
        <div class="card" style="max-width: 500px;">
          <el-form @submit.prevent="handleActivate" label-position="top">
            <el-form-item label="授权码">
              <el-input v-model="code" placeholder="请输入授权码" size="large" clearable />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                native-type="submit"
                :loading="loading"
                size="large"
                round
                style="background: var(--primary); border-color: var(--primary);"
              >
                激活
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </template>
    </div>
  </div>
</template>
