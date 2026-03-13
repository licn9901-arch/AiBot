<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { showToast } from 'vant'
import { resetPassword, validateResetPasswordToken } from '@/api/auth'
import { useResponsive } from '@/composables/useResponsive'

const route = useRoute()
const router = useRouter()
const { isMobile } = useResponsive()

const checking = ref(true)
const submitting = ref(false)
const tokenValid = ref(false)
const errorMessage = ref('')

const form = reactive({
  password: '',
  confirmPassword: '',
})

const token = computed(() => {
  const value = route.query.token
  return typeof value === 'string' ? value : ''
})

function notify(message: string, type: 'success' | 'warning' | 'error' = 'success') {
  if (isMobile.value) {
    showToast(message)
    return
  }

  if (type === 'success') ElMessage.success(message)
  if (type === 'warning') ElMessage.warning(message)
  if (type === 'error') ElMessage.error(message)
}

async function checkToken() {
  if (!token.value) {
    checking.value = false
    tokenValid.value = false
    errorMessage.value = '重置链接缺少必要参数，请重新发起找回密码流程。'
    return
  }

  checking.value = true
  errorMessage.value = ''

  try {
    const result = await validateResetPasswordToken(token.value)
    tokenValid.value = result.valid
  } catch {
    tokenValid.value = false
    errorMessage.value = '重置链接已失效或已被使用，请重新申请密码重置。'
  } finally {
    checking.value = false
  }
}

async function handleSubmit() {
  if (!tokenValid.value) {
    notify('当前重置链接不可用', 'warning')
    return
  }

  if (!form.password) {
    notify('请输入新密码', 'warning')
    return
  }

  if (form.password !== form.confirmPassword) {
    notify('两次输入的密码不一致', 'warning')
    return
  }

  submitting.value = true
  try {
    await resetPassword({ token: token.value, newPassword: form.password })
    notify('密码重置成功，请使用新密码登录')
    router.push('/auth/login')
  } catch {
    notify('密码重置失败，请重新申请重置链接', 'error')
  } finally {
    submitting.value = false
  }
}

onMounted(checkToken)
</script>

<template>
  <div class="page-stack">
    <div>
      <div class="ui-kicker">重置密码</div>
      <h2 style="margin: 12px 0 0; font-size: 32px; line-height: 1.2; letter-spacing: -0.03em;">
        {{ checking ? '正在验证链接' : tokenValid ? '设置新的登录密码' : '重置链接不可用' }}
      </h2>
      <p class="page-subtitle">
        <template v-if="checking">请稍候，我们正在确认该重置链接是否仍然有效。</template>
        <template v-else-if="tokenValid">请输入新的登录密码，提交后将立即生效。</template>
        <template v-else>{{ errorMessage }}</template>
      </p>
    </div>

    <form v-if="tokenValid" class="auth-form" @submit.prevent="handleSubmit">
      <div class="ui-field">
        <label class="ui-field-label" for="reset-password">新密码</label>
        <input id="reset-password" v-model="form.password" class="ui-input" type="password" placeholder="请输入新密码">
      </div>
      <div class="ui-field">
        <label class="ui-field-label" for="reset-confirm-password">确认新密码</label>
        <input id="reset-confirm-password" v-model="form.confirmPassword" class="ui-input" type="password" placeholder="请再次输入新密码">
      </div>
      <div class="auth-links">
        <RouterLink to="/auth/login">返回登录</RouterLink>
        <RouterLink to="/auth/forgot-password">重新获取重置邮件</RouterLink>
      </div>
      <div class="auth-form-actions">
        <button type="submit" class="ui-button primary" :disabled="submitting">
          {{ submitting ? '提交中...' : '确认重置密码' }}
        </button>
      </div>
    </form>

    <section v-else-if="!checking" class="ui-card ui-section-card">
      <div class="ui-empty" style="padding: 16px 0 0; justify-items: start; text-align: left;">
        <div class="ui-empty-emoji">⚠️</div>
        <div>
          <strong>建议操作</strong>
          <div class="ui-meta" style="margin-top: 8px;">
            你可以重新填写邮箱申请新的重置邮件，或者返回登录页尝试使用已有密码登录。
          </div>
        </div>
      </div>
      <div style="display: flex; gap: 12px; margin-top: 20px; flex-wrap: wrap;">
        <RouterLink to="/auth/forgot-password" class="ui-button primary">重新申请重置</RouterLink>
        <RouterLink to="/auth/login" class="ui-button ghost">返回登录</RouterLink>
      </div>
    </section>
  </div>
</template>
