<script setup lang="ts">
defineProps<{
  title: string
  actionText?: string
}>()

const emit = defineEmits<{
  action: []
}>()
</script>

<template>
  <div class="ui-section-head">
    <h2 class="ui-section-title">{{ title }}</h2>
    <button v-if="actionText" type="button" class="ui-section-action" @click="emit('action')">{{ actionText }}</button>
  </div>
</template>
