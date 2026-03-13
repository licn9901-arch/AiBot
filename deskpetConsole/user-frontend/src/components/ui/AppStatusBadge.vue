<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  label: string
  tone?: 'success' | 'danger' | 'warning' | 'info'
}>()

const toneClass = computed(() => props.tone ?? 'info')
</script>

<template>
  <span class="ui-status" :class="toneClass">{{ label }}</span>
</template>
