<script setup lang="ts">
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { showToast } from 'vant'
import { register } from '@/api/auth'
import { useResponsive } from '@/composables/useResponsive'

const router = useRouter()
const { isMobile } = useResponsive()

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
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

async function handleRegister() {
  if (!form.username || !form.password || !form.email) {
    notify('请输入用户名、邮箱和密码', 'warning')
    return
  }

  if (form.password !== form.confirmPassword) {
    notify('两次输入的密码不一致', 'warning')
    return
  }

  loading.value = true
  try {
    await register({
      username: form.username,
      password: form.password,
      email: form.email,
      phone: form.phone || undefined,
    })
    notify('注册成功，请查收激活邮件')
    router.push({
      name: 'check-email',
      query: {
        email: form.email,
        purpose: 'activation',
      },
    })
  } catch {
    notify('注册失败，请稍后重试', 'error')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page-stack">
    <div>
      <div class="ui-kicker">注册</div>
      <h2 style="margin: 12px 0 0; font-size: 32px; line-height: 1.2; letter-spacing: -0.03em;">创建你的 Cubee 账号</h2>
      <p class="page-subtitle">提交后我们会向你的邮箱发送激活邮件，完成激活后即可登录。</p>
    </div>

    <form class="auth-form" @submit.prevent="handleRegister">
      <div class="ui-field">
        <label class="ui-field-label" for="register-username">用户名</label>
        <input id="register-username" v-model="form.username" class="ui-input" type="text" placeholder="请输入用户名">
      </div>
      <div class="ui-field">
        <label class="ui-field-label" for="register-email">邮箱</label>
        <input id="register-email" v-model="form.email" class="ui-input" type="email" placeholder="请输入邮箱地址">
      </div>
      <div class="ui-field">
        <label class="ui-field-label" for="register-phone">手机号</label>
        <input id="register-phone" v-model="form.phone" class="ui-input" type="tel" placeholder="请输入手机号（可选）">
      </div>
      <div class="ui-field">
        <label class="ui-field-label" for="register-password">密码</label>
        <input id="register-password" v-model="form.password" class="ui-input" type="password" placeholder="请输入密码">
      </div>
      <div class="ui-field">
        <label class="ui-field-label" for="register-confirm">确认密码</label>
        <input id="register-confirm" v-model="form.confirmPassword" class="ui-input" type="password" placeholder="请再次输入密码">
      </div>
      <div class="auth-links">
        <span>注册成功后将跳转到查收邮箱页面</span>
        <span>已有账号？<RouterLink to="/auth/login">去登录</RouterLink></span>
      </div>
      <div class="auth-form-actions">
        <button type="submit" class="ui-button primary" :disabled="loading">
          {{ loading ? '注册中...' : '创建账号' }}
        </button>
      </div>
    </form>
  </div>
</template>
