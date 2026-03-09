<script setup lang="ts">
import { reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { showToast } from 'vant'
import { useResponsive } from '@/composables/useResponsive'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const { isMobile } = useResponsive()
const userStore = useUserStore()

const form = reactive({
  username: '',
  password: '',
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

async function handleLogin() {
  if (!form.username || !form.password) {
    notify('请输入用户名和密码', 'warning')
    return
  }

  loading.value = true
  try {
    await userStore.login({ username: form.username, password: form.password })
    notify('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/home'
    router.push(redirect)
  } catch {
    notify('登录失败，请检查账号信息', 'error')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page auth-page-login">
    <div class="auth-panel-header">
      <h2 class="auth-panel-title">登录</h2>
      <p class="auth-panel-description">欢迎回来，请登录你的 Cubee 账号继续访问设备、互动伙伴与授权管理能力。</p>
    </div>

    <form class="auth-form" @submit.prevent="handleLogin">
      <div class="ui-field">
        <label class="ui-field-label" for="login-username">账号</label>
        <input id="login-username" v-model="form.username" class="ui-input auth-input" type="text" placeholder="请输入用户名">
      </div>
      <div class="ui-field">
        <label class="ui-field-label" for="login-password">密码</label>
        <input id="login-password" v-model="form.password" class="ui-input auth-input" type="password" placeholder="请输入密码">
      </div>
      <div class="auth-inline-actions">
        <RouterLink to="/auth/forgot-password" class="auth-link-primary">忘记密码？</RouterLink>
      </div>
      <div class="auth-form-actions">
        <button type="submit" class="ui-button primary auth-submit-button" :disabled="loading">
          {{ loading ? '登录中...' : '立即登录' }}
        </button>
      </div>
      <p class="auth-footnote">
        还没有账号？
        <RouterLink to="/auth/register" class="auth-link-primary">去创建账号</RouterLink>
      </p>
    </form>
  </div>
</template>
