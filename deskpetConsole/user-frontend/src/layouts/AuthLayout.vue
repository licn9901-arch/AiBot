<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AuthHeroPanel from '@/components/ui/AuthHeroPanel.vue'
import { useResponsive } from '@/composables/useResponsive'

const route = useRoute()
const { isMobile } = useResponsive()

const authHero = computed(() => {
  switch (route.name) {
    case 'register':
      return {
        title: '加入你的 Cubee 空间',
        description: '创建账号后，我们会向你的邮箱发送激活邮件。完成激活后即可开始管理设备与账号信息。',
        bullets: ['注册后查收激活邮件', '完成激活后再登录', '统一管理设备与授权'],
      }
    case 'account-activate':
      return {
        title: '正在激活你的 Cubee 账号',
        description: '系统会自动验证激活链接并完成账号激活，完成后你就可以返回登录页继续使用。',
        bullets: ['自动校验激活 token', '成功后可直接登录', '失效链接可重新注册'],
      }
    case 'forgot-password':
      return {
        title: '找回你的登录入口',
        description: '输入注册邮箱后，我们会发送一封重置密码邮件。你可以在邮件链接里设置新的登录密码。',
        bullets: ['邮箱存在时自动发送邮件', '邮件里包含安全重置链接', '链接有效期由服务端控制'],
      }
    case 'reset-password':
      return {
        title: '设置一个新的密码',
        description: '我们会先验证重置链接是否有效，再允许你提交新密码。修改成功后即可使用新密码登录。',
        bullets: ['进入页面先校验链接', '新密码即时生效', '旧密码自动失效'],
      }
    case 'check-email':
      return {
        title: '查收你的邮箱提醒',
        description: '无论是激活账号还是重置密码，下一步都需要前往邮箱查看 Cubee 发出的邮件。',
        bullets: ['支持注册激活提醒', '支持密码重置提醒', '可快速返回登录或重填邮箱'],
      }
    default:
      return {
        title: '连接你的 Cubee 世界',
        description: '统一管理设备、互动伙伴、授权与账号资料。',
        bullets: ['查看设备状态', '与伙伴互动', '管理授权与订阅', '编辑账号资料'],
      }
  }
})

const mobileHeaderSubtitle = computed(() => {
  switch (route.name) {
    case 'register':
      return '创建账号，开始你的 Cubee 体验'
    case 'forgot-password':
      return '重置密码，重新找回你的登录权限'
    case 'reset-password':
      return '设置新密码，继续返回你的 Cubee 空间'
    case 'check-email':
      return '账号与功能提醒，请查看邮箱'
    case 'account-activate':
      return '账号激活确认，请稍候完成验证'
    default:
      return '欢迎回来，继续管理你的桌宠设备'
  }
})
</script>

<template>
  <div v-if="isMobile" class="mobile-auth-shell">
    <header class="mobile-topbar mobile-auth-header">
      <div class="mobile-brand-row">
        <div class="mobile-brand-copy">
          <div class="mobile-brand-title">Cubee</div>
          <div class="mobile-brand-subtitle">{{ mobileHeaderSubtitle }}</div>
        </div>
        <div class="mobile-brand-action mobile-brand-action-static">✦</div>
      </div>
    </header>

    <main class="mobile-auth-main">
      <RouterView />
    </main>
  </div>

  <div v-else class="auth-shell">
    <div class="auth-card">
      <AuthHeroPanel
        :title="authHero.title"
        :description="authHero.description"
        :bullets="authHero.bullets"
      />
      <div class="auth-form-pane">
        <div class="auth-form-stage">
          <RouterView />
        </div>
      </div>
    </div>
  </div>
</template>
