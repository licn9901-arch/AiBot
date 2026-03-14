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
  <div v-if="isMobile" class="mobile-auth-page">
    <section class="mobile-auth-hero is-login">
      <div class="mobile-auth-kicker">账号登录</div>
      <h1 class="mobile-auth-hero-title">连接你的 Cubee 世界</h1>
      <p class="mobile-auth-hero-description">查看设备与账号历史，继续你的设备管理旅程。</p>
    </section>

    <section class="mobile-auth-card">
      <div class="mobile-auth-card-title">登录</div>
      <form class="mobile-form-stack" @submit.prevent="handleLogin">
        <div class="ui-field">
          <label class="ui-field-label" for="login-username-mobile">账号</label>
          <input id="login-username-mobile" v-model="form.username" class="ui-input mobile-auth-input" type="text" placeholder="请输入用户名">
        </div>
        <div class="ui-field">
          <div class="mobile-label-row">
            <label class="ui-field-label" for="login-password-mobile">密码</label>
            <RouterLink to="/auth/forgot-password" class="mobile-inline-link">忘记密码？</RouterLink>
          </div>
          <input id="login-password-mobile" v-model="form.password" class="ui-input mobile-auth-input" type="password" placeholder="请输入密码">
        </div>
        <button type="submit" class="mobile-submit-button" :disabled="loading">
          {{ loading ? '登录中...' : '立即登录' }}
        </button>
        <p class="mobile-auth-footnote">
          还没有账号？
          <RouterLink to="/auth/register" class="mobile-inline-link">去创建账号</RouterLink>
        </p>
      </form>
    </section>

    <section class="mobile-auth-note-card">
      <div class="mobile-note-title">登录说明</div>
      <p class="mobile-note-text">· 支持使用注册时的账号登录</p>
      <p class="mobile-note-text">· 账号可查看设备与授权状态</p>
      <p class="mobile-note-text">· 如长时间未登录，可前往找回页面新建密码</p>
    </section>
  </div>

  <div v-else class="auth-page auth-page-login">
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
