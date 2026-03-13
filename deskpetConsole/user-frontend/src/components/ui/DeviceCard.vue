<script setup lang="ts">
import { computed } from 'vue'
import type { DeviceResponse } from '@/types/device'
import { formatRelativeTime } from '@/utils/format'
import AppStatusBadge from './AppStatusBadge.vue'
import CachedImage from './CachedImage.vue'

const props = defineProps<{
  device: DeviceResponse
  clickable?: boolean
  desktop?: boolean
}>()

const statusLabel = computed(() => (props.device.online ? '在线' : '离线'))
const statusTone = computed(() => (props.device.online ? 'success' : 'danger'))
const deviceName = computed(() => props.device.remark || props.device.model || props.device.deviceId)
const metaLine = computed(() => props.device.model || props.device.productKey)
const lastSeenText = computed(() => (props.device.online ? '设备运行中' : `最后在线 ${formatRelativeTime(props.device.lastSeen)}`))
const productIconCacheKey = computed(() => `product-icon:${props.device.productKey}`)

const emit = defineEmits<{
  click: []
}>()
</script>

<template>
  <article class="ui-card ui-device-card" :class="clickable ? 'clickable' : ''" @click="emit('click')">
    <div class="ui-device-media">
      <CachedImage
        v-if="device.productIcon"
        :src="device.productIcon"
        :cache-key="productIconCacheKey"
        :alt="deviceName"
        class="ui-device-media-image"
      >
        <template #fallback>
          <span>🤖</span>
        </template>
      </CachedImage>
      <span v-else>🤖</span>
    </div>
    <div class="ui-device-body">
      <div class="ui-device-name-row">
        <div class="ui-device-name">{{ deviceName }}</div>
        <AppStatusBadge :label="statusLabel" :tone="statusTone" />
      </div>
      <div class="ui-device-subtitle">{{ metaLine }}</div>
      <div class="ui-meta small">设备 ID · {{ device.deviceId }}</div>
      <div class="ui-meta small" :style="desktop ? 'margin-top: 6px;' : 'margin-top: 4px;'">{{ lastSeenText }}</div>
    </div>
  </article>
</template>
