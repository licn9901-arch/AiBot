<script setup lang="ts">
defineProps<{
  title: string
  description: string
  bullets: string[]
}>()
</script>

<template>
  <section class="auth-hero">
    <div class="auth-hero-copy">
      <div class="auth-hero-brand">Cubee</div>
      <h1 class="auth-hero-title">{{ title }}</h1>
      <p class="auth-hero-description">{{ description }}</p>
    </div>

    <div class="auth-hero-visual">
      <div class="auth-hero-logo-card">
        <div class="auth-hero-cube auth-hero-cube-shadow" />
        <div class="auth-hero-cube auth-hero-cube-face">
          <span class="auth-hero-eye auth-hero-eye-left" />
          <span class="auth-hero-eye auth-hero-eye-right" />
          <span class="auth-hero-smile" />
        </div>
      </div>
      <div class="auth-hero-points">
        <strong class="auth-hero-points-title">通过 Cubee 你可以：</strong>
        <ul class="auth-hero-points-list">
          <li v-for="bullet in bullets" :key="bullet">{{ bullet }}</li>
        </ul>
      </div>
    </div>
  </section>
</template>
