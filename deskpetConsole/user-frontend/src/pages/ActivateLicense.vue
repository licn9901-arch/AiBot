<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { showToast } from 'vant'
import { activateLicense } from '@/api/license'
import { useResponsive } from '@/composables/useResponsive'
import type { LicenseCodeResponse } from '@/types/license'

const router = useRouter()
const { isMobile } = useResponsive()

const code = ref('')
const loading = ref(false)
const result = ref<LicenseCodeResponse | null>(null)

function notify(message: string, type: 'success' | 'warning' | 'error' = 'success') {
  if (isMobile.value) {
    showToast(message)
    return
  }

  if (type === 'success') ElMessage.success(message)
  if (type === 'warning') ElMessage.warning(message)
  if (type === 'error') ElMessage.error(message)
}

async function handleActivate() {
  if (!code.value.trim()) {
    notify('请输入授权码', 'warning')
    return
  }

  loading.value = true
  result.value = null
  try {
    result.value = await activateLicense({ code: code.value.trim() })
    notify('授权激活成功')
    code.value = ''
  } catch {
    notify('激活失败，请稍后重试', 'error')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page-stack">
    <section class="page-title-block">
      <div>
        <h1 class="page-title">激活授权码</h1>
        <p class="page-subtitle">输入你的 Cubee 授权码，完成设备绑定与权限激活。</p>
      </div>
    </section>

    <section class="ui-card ui-section-card" style="max-width: 760px;">
      <form class="page-stack" @submit.prevent="handleActivate">
        <div class="ui-field">
          <label class="ui-field-label" for="license-code">授权码</label>
          <input
            id="license-code"
            v-model="code"
            class="ui-input"
            type="text"
            placeholder="DKPT-XXXX-XXXX-XXXX"
            autocomplete="off"
          >
          <div class="ui-field-help">支持粘贴标准授权码格式，提交后将自动校验并显示结果。</div>
        </div>
        <div style="display: flex; gap: 12px; flex-wrap: wrap;">
          <button type="submit" class="ui-button primary" :disabled="loading">
            {{ loading ? '激活中...' : '立即激活' }}
          </button>
          <button type="button" class="ui-button ghost" @click="code = ''">清空</button>
        </div>
      </form>
    </section>

    <section v-if="result" class="ui-card ui-section-card" style="max-width: 760px;">
      <div class="status-panel">
        <strong>激活成功</strong>
        <div class="ui-meta">授权码：{{ result.code }}</div>
        <div class="ui-meta">状态：{{ result.status }}</div>
        <div class="ui-meta">绑定设备：{{ result.deviceId || '暂未绑定' }}</div>
      </div>
      <div style="display: flex; gap: 12px; margin-top: 18px; flex-wrap: wrap;">
        <button
          v-if="result.deviceId"
          type="button"
          class="ui-button primary"
          @click="router.push(`/devices/${result.deviceId}`)"
        >
          前往设备控制
        </button>
        <button type="button" class="ui-button secondary" @click="router.push('/devices')">查看我的设备</button>
      </div>
    </section>
  </div>
</template>
