<script setup lang="ts">
defineProps<{
  subtitle?: string
  compact?: boolean
}>()
</script>

<template>
  <div class="ui-brand" :style="compact ? 'gap: 10px;' : ''">
    <span class="ui-brand-mark">C</span>
    <span>
      <div class="ui-brand-title" :style="compact ? 'font-size: 16px;' : ''">Cubee Console</div>
      <div v-if="subtitle" class="ui-brand-subtitle">{{ subtitle }}</div>
    </span>
  </div>
</template>
