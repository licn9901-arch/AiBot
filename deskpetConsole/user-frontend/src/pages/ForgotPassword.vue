<script setup lang="ts">
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { showToast } from 'vant'
import { forgotPassword } from '@/api/auth'
import { useResponsive } from '@/composables/useResponsive'

const router = useRouter()
const { isMobile } = useResponsive()

const form = reactive({
  email: '',
})
const loading = ref(false)

function notify(message: string, type: 'success' | 'warning' | 'error' = 'success') {
  if (isMobile.value) {
    showToast(message)
    return
  }

  if (type === 'success') ElMessage.success(message)
  if (type === 'warning') ElMessage.warning(message)
  if (type === 'error') ElMessage.error(message)
}

async function handleSubmit() {
  if (!form.email) {
    notify('请输入注册邮箱', 'warning')
    return
  }

  loading.value = true
  try {
    await forgotPassword({ email: form.email })
    notify('如果邮箱存在，我们已发送重置邮件')
    router.push({
      name: 'check-email',
      query: {
        email: form.email,
        purpose: 'reset-password',
      },
    })
  } catch {
    notify('发送失败，请稍后重试', 'error')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page-stack">
    <div>
      <div class="ui-kicker">找回密码</div>
      <h2 style="margin: 12px 0 0; font-size: 32px; line-height: 1.2; letter-spacing: -0.03em;">通过邮箱重置密码</h2>
      <p class="page-subtitle">输入注册邮箱后，我们会发送一封包含重置链接的邮件。</p>
    </div>

    <form class="auth-form" @submit.prevent="handleSubmit">
      <div class="ui-field">
        <label class="ui-field-label" for="forgot-email">邮箱</label>
        <input id="forgot-email" v-model="form.email" class="ui-input" type="email" placeholder="请输入注册邮箱">
      </div>
      <div class="auth-links">
        <RouterLink to="/auth/login">返回登录</RouterLink>
        <RouterLink to="/auth/register">没有账号？去注册</RouterLink>
      </div>
      <div class="auth-form-actions">
        <button type="submit" class="ui-button primary" :disabled="loading">
          {{ loading ? '提交中...' : '发送重置邮件' }}
        </button>
      </div>
    </form>
  </div>
</template>
