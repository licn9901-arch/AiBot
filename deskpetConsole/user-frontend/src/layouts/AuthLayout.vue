<script setup lang="ts">
</script>

<template>
  <div class="auth-layout">
    <div class="auth-card card">
      <div class="brand" style="justify-content: center; margin-bottom: 24px;">
        <div class="brand-mark"></div>
        <div>
          <div class="brand-title">DeskPet</div>
          <div class="brand-subtitle">桌宠控制中心</div>
        </div>
      </div>
      <RouterView />
    </div>
  </div>
</template>

<style scoped>
.auth-layout {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: var(--bg);
}

.auth-card {
  width: 100%;
  max-width: 420px;
  padding: 32px;
}

@media (max-width: 767px) {
  .auth-card {
    max-width: 100%;
    border-radius: 12px;
    padding: 24px 20px;
  }
}
</style>
