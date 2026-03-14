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
  <div v-if="isMobile" class="mobile-auth-page">
    <section class="mobile-auth-hero is-forgot">
      <div class="mobile-auth-kicker">忘记密码</div>
      <h1 class="mobile-auth-hero-title">通过邮箱直接收取重置链接</h1>
      <p class="mobile-auth-hero-description">输入注册邮箱后，我们将发送密码重置邮件。</p>
    </section>

    <section class="mobile-auth-card">
      <div class="mobile-auth-card-title">找回密码</div>
      <form class="mobile-form-stack" @submit.prevent="handleSubmit">
        <div class="ui-field">
          <label class="ui-field-label" for="forgot-email-mobile">邮箱</label>
          <input id="forgot-email-mobile" v-model="form.email" class="ui-input mobile-auth-input" type="email" placeholder="请输入注册邮箱">
        </div>
        <button type="submit" class="mobile-submit-button" :disabled="loading">
          {{ loading ? '提交中...' : '发送重置邮件' }}
        </button>
        <p class="mobile-auth-footnote">
          想起来密码了？
          <RouterLink to="/auth/login" class="mobile-inline-link">返回登录</RouterLink>
        </p>
      </form>
    </section>

    <section class="mobile-auth-note-card">
      <div class="mobile-note-title">找回说明</div>
      <p class="mobile-note-text">· 仅支持通过注册邮箱找回密码</p>
      <p class="mobile-note-text">· 若长时间未收到邮件，请检查垃圾邮箱</p>
      <p class="mobile-note-text">· 重置链接具备时效性与唯一性</p>
    </section>
  </div>

  <div v-else class="page-stack">
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
