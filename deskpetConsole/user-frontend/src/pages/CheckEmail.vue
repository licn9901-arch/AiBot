<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useResponsive } from '@/composables/useResponsive'

const route = useRoute()
const { isMobile } = useResponsive()

const email = computed(() => {
  const value = route.query.email
  return typeof value === 'string' && value ? value : 'your-email@example.com'
})

const purpose = computed(() => {
  const value = route.query.purpose
  return value === 'activation' ? 'activation' : 'reset-password'
})

const content = computed(() => {
  if (purpose.value === 'activation') {
    return {
      kicker: '查收激活邮件',
      title: '激活邮件已发送',
      description: `请前往 ${email.value} 查收账号激活邮件，并点击邮件中的激活链接完成注册。`,
      steps: [
        '打开邮箱收件箱并查找 Cubee 发出的激活邮件',
        '点击邮件中的激活链接完成账号激活',
        '激活完成后返回登录页继续使用',
      ],
      backTo: '/auth/login',
      backLabel: '回到登录',
      retryRoute: { name: 'register' },
      retryLabel: '重新填写注册信息',
    }
  }

  return {
    kicker: '查收重置邮件',
    title: '重置邮件已发送',
    description: `如果 ${email.value} 对应账号存在且已激活，请前往邮箱查看密码重置邮件。`,
    steps: [
      '打开邮箱收件箱并查找 Cubee 发出的重置邮件',
      '点击邮件中的重置链接进入新密码设置页',
      '重置完成后返回登录页使用新密码登录',
    ],
    backTo: '/auth/login',
    backLabel: '回到登录',
    retryRoute: { name: 'forgot-password', query: { email: email.value } },
    retryLabel: '重新填写邮箱',
  }
})
</script>

<template>
  <div v-if="isMobile" class="mobile-auth-page">
    <section class="mobile-status-card">
      <div class="mobile-status-icon">✉</div>
      <h1 class="mobile-status-title">{{ purpose === 'activation' ? '注册成功' : '邮件已发送' }}</h1>
      <p class="mobile-status-description">{{ content.description }}</p>

      <section class="mobile-status-note">
        <div class="mobile-note-title">建议操作</div>
        <p class="mobile-note-text">1. {{ content.steps[0] }}</p>
        <p class="mobile-note-text">2. {{ content.steps[1] }}</p>
        <p class="mobile-note-text">3. {{ content.steps[2] }}</p>
      </section>

      <RouterLink :to="content.backTo" class="mobile-submit-button mobile-submit-link">{{ content.backLabel }}</RouterLink>
      <p class="mobile-auth-footnote">
        已完成验证？
        <RouterLink :to="content.backTo" class="mobile-inline-link">返回登录</RouterLink>
      </p>
    </section>
  </div>

  <div v-else class="page-stack">
    <div>
      <div class="ui-kicker">{{ content.kicker }}</div>
      <h2 style="margin: 12px 0 0; font-size: 32px; line-height: 1.2; letter-spacing: -0.03em;">{{ content.title }}</h2>
      <p class="page-subtitle">{{ content.description }}</p>
    </div>

    <section class="ui-card ui-section-card">
      <div class="ui-empty" style="padding: 16px 0 0; justify-items: start; text-align: left;">
        <div class="ui-empty-emoji">✉️</div>
        <div>
          <strong>下一步建议</strong>
          <div class="ui-meta" style="margin-top: 8px; white-space: pre-line;">
            1. {{ content.steps[0] }}
            <br>
            2. {{ content.steps[1] }}
            <br>
            3. {{ content.steps[2] }}
          </div>
        </div>
      </div>
      <div style="display: flex; gap: 12px; margin-top: 20px; flex-wrap: wrap;">
        <RouterLink :to="content.backTo" class="ui-button primary">{{ content.backLabel }}</RouterLink>
        <RouterLink :to="content.retryRoute" class="ui-button ghost">{{ content.retryLabel }}</RouterLink>
      </div>
    </section>
  </div>
</template>
