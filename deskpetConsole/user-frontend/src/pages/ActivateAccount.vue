<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { showToast } from 'vant'
import { activateAccount } from '@/api/auth'
import { useResponsive } from '@/composables/useResponsive'

const route = useRoute()
const { isMobile } = useResponsive()

const loading = ref(true)
const success = ref(false)
const errorMessage = ref('')

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

async function runActivation() {
  if (!token.value) {
    loading.value = false
    errorMessage.value = '激活链接无效，请重新注册后使用最新邮件中的链接。'
    return
  }

  loading.value = true
  success.value = false
  errorMessage.value = ''

  try {
    await activateAccount(token.value)
    success.value = true
    notify('账号激活成功')
  } catch {
    errorMessage.value = '激活失败，可能是链接已过期或已被使用，请重新注册后再试。'
  } finally {
    loading.value = false
  }
}

onMounted(runActivation)
</script>

<template>
  <div class="page-stack">
    <div>
      <div class="ui-kicker">账号激活</div>
      <h2 style="margin: 12px 0 0; font-size: 32px; line-height: 1.2; letter-spacing: -0.03em;">
        {{ loading ? '正在验证激活链接' : success ? '账号激活成功' : '激活未完成' }}
      </h2>
      <p class="page-subtitle">
        <template v-if="loading">请稍候，我们正在为你完成账号激活。</template>
        <template v-else-if="success">你的 Cubee 账号已经激活完成，现在可以返回登录页开始使用。</template>
        <template v-else>{{ errorMessage }}</template>
      </p>
    </div>

    <section class="ui-card ui-section-card">
      <div class="ui-empty" style="padding: 16px 0 0; justify-items: start; text-align: left;">
        <div class="ui-empty-emoji">{{ loading ? '⏳' : success ? '✅' : '⚠️' }}</div>
        <div>
          <strong>{{ loading ? '处理中' : success ? '接下来可以这样做' : '你可以这样处理' }}</strong>
          <div class="ui-meta" style="margin-top: 8px;">
            <template v-if="loading">系统正在向服务端确认激活 token，请勿重复刷新页面。</template>
            <template v-else-if="success">返回登录页并使用刚刚注册的账号密码登录。如果尚未记住密码，可直接走重置密码流程。</template>
            <template v-else>请检查是否点击了最新邮件中的链接；如果仍然失败，可以重新注册获取新的激活邮件。</template>
          </div>
        </div>
      </div>
      <div style="display: flex; gap: 12px; margin-top: 20px; flex-wrap: wrap;">
        <RouterLink v-if="success" to="/auth/login" class="ui-button primary">去登录</RouterLink>
        <button v-else-if="!loading" type="button" class="ui-button primary" @click="runActivation">重试激活</button>
        <RouterLink v-if="!success" to="/auth/register" class="ui-button ghost">返回注册</RouterLink>
      </div>
    </section>
  </div>
</template>
