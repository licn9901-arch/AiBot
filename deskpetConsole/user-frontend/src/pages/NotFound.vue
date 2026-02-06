<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="empty-state" style="min-height: 80vh; display: flex; flex-direction: column; align-items: center; justify-content: center;">
    <div style="font-size: 72px; font-weight: 700; color: var(--border); font-family: var(--font-display);">404</div>
    <div style="font-size: 16px; color: var(--muted); margin: 12px 0 24px;">页面不存在</div>
    <button class="btn primary" @click="router.push('/home')">返回首页</button>
  </div>
</template>
